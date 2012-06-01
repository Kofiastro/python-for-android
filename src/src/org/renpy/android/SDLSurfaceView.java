/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This string is autogenerated by ChangeAppSettings.sh, do not change spaces amount
package org.renpy.android;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES20;
import android.opengl.Matrix;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.net.Uri;
import android.os.PowerManager;

import java.io.IOException;
import java.io.InputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.graphics.Color;
import android.content.res.Resources;


public class SDLSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
	private static String TAG = "SDLSurface";
    private final String mVertexShader =
        "uniform mat4 uMVPMatrix;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aTextureCoord;\n" +
        "varying vec2 vTextureCoord;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "  vTextureCoord = aTextureCoord;\n" +
        "}\n";

    private final String mFragmentShader =
        "precision mediump float;\n" +
        "varying vec2 vTextureCoord;\n" +
        "uniform sampler2D sTexture;\n" +
        "void main() {\n" +
        "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +

        "}\n";
	private static class ConfigChooser implements GLSurfaceView.EGLConfigChooser {

		public ConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
			mRedSize = r;
			mGreenSize = g;
			mBlueSize = b;
			mAlphaSize = a;
			mDepthSize = depth;
			mStencilSize = stencil;
		}

		/* This EGL config specification is used to specify 2.0 rendering.
		 * We use a minimum size of 4 bits for red/green/blue, but will
		 * perform actual matching in chooseConfig() below.
		 */
		private static int EGL_OPENGL_ES2_BIT = 4;
		private static int[] s_configAttribs2 =
		{
			EGL10.EGL_RED_SIZE, 4,
			EGL10.EGL_GREEN_SIZE, 4,
			EGL10.EGL_BLUE_SIZE, 4,
			EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
			EGL10.EGL_NONE
		};

		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

			/* Get the number of minimally matching EGL configurations
			*/
			int[] num_config = new int[1];
			egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config);

			int numConfigs = num_config[0];

			if (numConfigs <= 0) {
				throw new IllegalArgumentException("No configs match configSpec");
			}

			/* Allocate then read the array of minimally matching EGL configs
			*/
			EGLConfig[] configs = new EGLConfig[numConfigs];
			egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs, num_config);

			/* Now return the "best" one
			*/
			//printConfigs(egl, display, configs);
			return chooseConfig(egl, display, configs);
		}

		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
				EGLConfig[] configs) {
			for(EGLConfig config : configs) {
				int d = findConfigAttrib(egl, display, config,
						EGL10.EGL_DEPTH_SIZE, 0);
				int s = findConfigAttrib(egl, display, config,
						EGL10.EGL_STENCIL_SIZE, 0);

				// We need at least mDepthSize and mStencilSize bits
				if (d < mDepthSize || s < mStencilSize)
					continue;

				// We want an *exact* match for red/green/blue/alpha
				int r = findConfigAttrib(egl, display, config,
						EGL10.EGL_RED_SIZE, 0);
				int g = findConfigAttrib(egl, display, config,
						EGL10.EGL_GREEN_SIZE, 0);
				int b = findConfigAttrib(egl, display, config,
						EGL10.EGL_BLUE_SIZE, 0);
				int a = findConfigAttrib(egl, display, config,
						EGL10.EGL_ALPHA_SIZE, 0);

				if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize)
					return config;
			}
			return null;
		}

		private int findConfigAttrib(EGL10 egl, EGLDisplay display,
				EGLConfig config, int attribute, int defaultValue) {

			if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
				return mValue[0];
			}
			return defaultValue;
		}

		private void printConfigs(EGL10 egl, EGLDisplay display,
				EGLConfig[] configs) {
			int numConfigs = configs.length;
			Log.w(TAG, String.format("%d configurations", numConfigs));
			for (int i = 0; i < numConfigs; i++) {
				Log.w(TAG, String.format("Configuration %d:\n", i));
				printConfig(egl, display, configs[i]);
			}
		}

		private void printConfig(EGL10 egl, EGLDisplay display,
				EGLConfig config) {
			int[] attributes = {
				EGL10.EGL_BUFFER_SIZE,
				EGL10.EGL_ALPHA_SIZE,
				EGL10.EGL_BLUE_SIZE,
				EGL10.EGL_GREEN_SIZE,
				EGL10.EGL_RED_SIZE,
				EGL10.EGL_DEPTH_SIZE,
				EGL10.EGL_STENCIL_SIZE,
				EGL10.EGL_CONFIG_CAVEAT,
				EGL10.EGL_CONFIG_ID,
				EGL10.EGL_LEVEL,
				EGL10.EGL_MAX_PBUFFER_HEIGHT,
				EGL10.EGL_MAX_PBUFFER_PIXELS,
				EGL10.EGL_MAX_PBUFFER_WIDTH,
				EGL10.EGL_NATIVE_RENDERABLE,
				EGL10.EGL_NATIVE_VISUAL_ID,
				EGL10.EGL_NATIVE_VISUAL_TYPE,
				0x3030, // EGL10.EGL_PRESERVED_RESOURCES,
				EGL10.EGL_SAMPLES,
				EGL10.EGL_SAMPLE_BUFFERS,
				EGL10.EGL_SURFACE_TYPE,
				EGL10.EGL_TRANSPARENT_TYPE,
				EGL10.EGL_TRANSPARENT_RED_VALUE,
				EGL10.EGL_TRANSPARENT_GREEN_VALUE,
				EGL10.EGL_TRANSPARENT_BLUE_VALUE,
				0x3039, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
				0x303A, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
				0x303B, // EGL10.EGL_MIN_SWAP_INTERVAL,
				0x303C, // EGL10.EGL_MAX_SWAP_INTERVAL,
				EGL10.EGL_LUMINANCE_SIZE,
				EGL10.EGL_ALPHA_MASK_SIZE,
				EGL10.EGL_COLOR_BUFFER_TYPE,
				EGL10.EGL_RENDERABLE_TYPE,
				0x3042 // EGL10.EGL_CONFORMANT
			};
			String[] names = {
				"EGL_BUFFER_SIZE",
				"EGL_ALPHA_SIZE",
				"EGL_BLUE_SIZE",
				"EGL_GREEN_SIZE",
				"EGL_RED_SIZE",
				"EGL_DEPTH_SIZE",
				"EGL_STENCIL_SIZE",
				"EGL_CONFIG_CAVEAT",
				"EGL_CONFIG_ID",
				"EGL_LEVEL",
				"EGL_MAX_PBUFFER_HEIGHT",
				"EGL_MAX_PBUFFER_PIXELS",
				"EGL_MAX_PBUFFER_WIDTH",
				"EGL_NATIVE_RENDERABLE",
				"EGL_NATIVE_VISUAL_ID",
				"EGL_NATIVE_VISUAL_TYPE",
				"EGL_PRESERVED_RESOURCES",
				"EGL_SAMPLES",
				"EGL_SAMPLE_BUFFERS",
				"EGL_SURFACE_TYPE",
				"EGL_TRANSPARENT_TYPE",
				"EGL_TRANSPARENT_RED_VALUE",
				"EGL_TRANSPARENT_GREEN_VALUE",
				"EGL_TRANSPARENT_BLUE_VALUE",
				"EGL_BIND_TO_TEXTURE_RGB",
				"EGL_BIND_TO_TEXTURE_RGBA",
				"EGL_MIN_SWAP_INTERVAL",
				"EGL_MAX_SWAP_INTERVAL",
				"EGL_LUMINANCE_SIZE",
				"EGL_ALPHA_MASK_SIZE",
				"EGL_COLOR_BUFFER_TYPE",
				"EGL_RENDERABLE_TYPE",
				"EGL_CONFORMANT"
			};
			int[] value = new int[1];
			for (int i = 0; i < attributes.length; i++) {
				int attribute = attributes[i];
				String name = names[i];
				if ( egl.eglGetConfigAttrib(display, config, attribute, value)) {
					Log.w(TAG, String.format("  %s: %d\n", name, value[0]));
				} else {
					// Log.w(TAG, String.format("  %s: failed\n", name));
					while (egl.eglGetError() != EGL10.EGL_SUCCESS);
				}
			}
		}

		// Subclasses can adjust these values:
		protected int mRedSize;
		protected int mGreenSize;
		protected int mBlueSize;
		protected int mAlphaSize;
		protected int mDepthSize;
		protected int mStencilSize;
		private int[] mValue = new int[1];
	}

    // The activity we're a part of.
    private static Activity mActivity;

    // Have we started yet?
    public boolean mStarted = false;

    // Is Python ready to receive input events?
    static boolean mInputActivated = false;

    // The number of times we should clear the screen after swap.
    private int mClears = 2;

    // Has the display been changed?
    private boolean mChanged = false;

    // Are we running yet?
    private boolean mRunning = false;

    // The EGL used by our thread.
    private EGL10 mEgl = null;

    // The EGL Display used.
    private EGLDisplay mEglDisplay = null;

    // The EGL Context used.
    private EGLContext mEglContext = null;

    // The EGL Surface used.
    private EGLSurface mEglSurface = null;

    // The EGL Config used.
    private EGLConfig mEglConfig = null;

    // The user program is not participating in the pause protocol.
    static int PAUSE_NOT_PARTICIPATING = 0;

    // A pause has not been requested by the OS.
    static int PAUSE_NONE = 1;

    // A pause has been requested by Android, but the user program has
    // not bothered responding yet.
    static int PAUSE_REQUEST = 2;

    // The user program is waiting in waitForResume.
    static int PAUSE_WAIT_FOR_RESUME = 3;

	static int PAUSE_STOP_REQUEST = 4;
	static int PAUSE_STOP_ACK = 5;

    // This stores the state of the pause system.
    static int mPause = PAUSE_NOT_PARTICIPATING;

    private PowerManager.WakeLock wakeLock;

    // The width and height. (This should be set at startup time -
    // these values just prevent segfaults and divide by zero, etc.)
    int mWidth = 100;
    int mHeight = 100;

    // The name of the directory where the context stores its files.
    String mFilesDirectory = null;

    // The value of the argument passed in.
    String mArgument = null;

    // The resource manager we use.
    ResourceManager mResourceManager;

	// Our own view
	static SDLSurfaceView instance = null;

    public SDLSurfaceView(Activity act, String argument) {
        super(act);

		SDLSurfaceView.instance = this;

        mActivity = act;
        mResourceManager = new ResourceManager(act);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

        mFilesDirectory = mActivity.getFilesDir().getAbsolutePath();
        mArgument = argument;

        PowerManager pm = (PowerManager) act.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Screen On");
    }


    /**
     * The user program should call this frequently to check if a
     * pause has been requested by android. If this ever returns
     * true, the user program should clean up and call waitForResume.
     */
    public int checkPause() {
        if (mPause == PAUSE_NOT_PARTICIPATING) {
            mPause = PAUSE_NONE;
        }

        if (mPause == PAUSE_REQUEST) {
            return 1;
        } else {
            return 0;
        }
    }


    /**
     * The user program should call this quickly after checkPause
     * returns true. This causes the android application to sleep,
     * waiting for resume. While sleeping, it should not have any
     * activity. (Notably, it should stop all timers.)
     *
     * While we're waiting in this method, android is allowed to
     * kill us to reclaim memory, without any further warning.
     */
    public void waitForResume() {
        synchronized (this) {
            mPause = PAUSE_WAIT_FOR_RESUME;

            // Notify any threads waiting in onPause.
            this.notifyAll();

            while (mPause == PAUSE_WAIT_FOR_RESUME) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        setOpenFile();
    }

    /**
     * if the activity was called with a file parameter, put it in the
     * 'PYTHON_OPENFILE' env var
     */
    public static void setOpenFile(){
        final android.content.Intent intent = mActivity.getIntent();
        if (intent != null) {
            final android.net.Uri data = intent.getData ();
            if (data != null){
                nativeSetEnv("PYTHON_OPENFILE", data.getEncodedPath());
            }
        }
    }

    /**
     * Inform the view that the activity is paused. The owner of this view must
     * call this method when the activity is paused. Calling this method will
     * pause the rendering thread.
     * Must not be called before a renderer has been set.
     */
    public void onPause() {

        synchronized (this) {
            if (mPause == PAUSE_NONE) {
                mPause = PAUSE_REQUEST;

                while (mPause == PAUSE_REQUEST) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        // pass
                    }
                }
            }
        }

        wakeLock.release();

    }

    /**
     * Inform the view that the activity is resumed. The owner of this view must
     * call this method when the activity is resumed. Calling this method will
     * recreate the OpenGL display and resume the rendering
     * thread.
     * Must not be called before a renderer has been set.
     */
    public void onResume() {
        synchronized (this) {
            if (mPause == PAUSE_WAIT_FOR_RESUME) {
                mPause = PAUSE_NONE;
                this.notifyAll();
            }
        }
        wakeLock.acquire();
    }

	public void onDestroy() {
		Log.w(TAG, "onDestroy() called");
		synchronized (this) {
			this.notifyAll();

			if ( mPause == PAUSE_STOP_ACK ) {
				Log.d(TAG, "onDestroy() app already leaved.");
				return;
			}

			// application didn't leave, give 10s before closing.
			// hopefully, this could be enough for launching the on_stop() trigger within the app.
			mPause = PAUSE_STOP_REQUEST;
			int i = 50;

			Log.d(TAG, "onDestroy() stop requested, wait for an event from the app");
			for (; i >= 0 && mPause == PAUSE_STOP_REQUEST; i--) {
				try {
					this.wait(200);
				} catch (InterruptedException e) {
					break;
				}
			}
			Log.d(TAG, "onDestroy() stop finished waiting.");
		}
	}

	static int checkStop() {
        if (mPause == PAUSE_STOP_REQUEST)
			return 1;
		return 0;
	}

	static void ackStop() {
		Log.d(TAG, "ackStop() notify");
		synchronized (instance) {
			mPause = PAUSE_STOP_ACK;
			instance.notifyAll();
		}
	}


    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated() is not handled :|");
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyed() is not handled :|");
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mWidth = w;
        mHeight = h;

        if (mActivity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE &&
            mWidth < mHeight) {
            return;
        }

        if (mActivity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT &&
            mWidth > mHeight) {
            return;
        }

        if (!mRunning) {
            mRunning = true;
            new Thread(this).start();
        } else {
            mChanged = true;
        }
    }


    public void run() {
        mEgl = (EGL10) EGLContext.getEGL();

        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        int[] version = new int[2];
        mEgl.eglInitialize(mEglDisplay, version);

        // Pick an appropriate config. We just take the first config
        // the system offers to us, because anything more complicated
        // than that stands a really good chance of not working.
        int[] configSpec = {
            // RENDERABLE_TYPE = OpenGL ES is the default.
            EGL10.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
		int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        int[] num_config = new int[1];
		int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };

		// Create an opengl es 2.0 surface
		Log.w(TAG, "Choose egl configuration");
		int configToTest = 0;
		boolean configFound = false;

		while (true) {
			try {
				if (configToTest == 0) {
					Log.i(TAG, "Try to use graphics config R8G8B8A8S8");
					ConfigChooser chooser = new ConfigChooser(8, 8, 8, 8, 0, 8);
					mEglConfig = chooser.chooseConfig(mEgl, mEglDisplay);
				} else if (configToTest == 1) {
					Log.i(TAG, "Try to use graphics config R5G6B5S8");
					ConfigChooser chooser = new ConfigChooser(5, 6, 5, 0, 0, 8);
					mEglConfig = chooser.chooseConfig(mEgl, mEglDisplay);
				} else {
					Log.e(TAG, "Unable to found a correct surface for this device !");
					break;
				}

			} catch (IllegalArgumentException e) {
				configToTest++;
				continue;
			}

			Log.w(TAG, "Create egl context");
			mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
			if (mEglContext == null) {
				Log.w(TAG, "Unable to create egl context with this configuration, try the next one.");
				configToTest++;
				continue;
			}

			Log.w(TAG, "Create egl surface");
			if (!createSurface()) {
				Log.w(TAG, "Unable to create egl surface with this configuration, try the next one.");
				configToTest++;
				continue;
			}

			configFound = true;
			break;
		}

		if (!configFound) {
			System.exit(0);
			return;
		}

                Log.w(TAG, "Done");
                waitForStart();
                setOpenFile();

        nativeResize(mWidth, mHeight);
        nativeInitJavaCallbacks();
        nativeSetEnv("ANDROID_PRIVATE", mFilesDirectory);
        nativeSetEnv("ANDROID_ARGUMENT", mArgument);
        nativeSetEnv("PYTHONOPTIMIZE", "2");
        nativeSetEnv("PYTHONHOME", mFilesDirectory);
        nativeSetEnv("PYTHONPATH", mArgument + ":" + mFilesDirectory + "/lib");
		nativeSetMultitouchUsed();
        nativeInit();

		mPause = PAUSE_STOP_ACK;

		//Log.i(TAG, "End of native init, stop everything (exit0)");
        System.exit(0);
    }

    private void glCheck(GL10 gl) {
        int gle = gl.glGetError();
        if (gle != gl.GL_NO_ERROR) {
            throw new RuntimeException("GL Error: " + gle);
        }
    }

    private void waitForStart() {

        int presplashId = mResourceManager.getIdentifier("presplash", "drawable");
        InputStream is = mActivity.getResources().openRawResource(presplashId);

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(is);
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        } finally {
            try {
                is.close();
            } catch (IOException e) { }
        }

        mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);

		mProgram = createProgram(mVertexShader, mFragmentShader);
		if (mProgram == 0) {
			synchronized (this) {
				while (!mStarted) {
					try {
						this.wait(250);
					} catch (InterruptedException e) {
						continue;
					}
				}
			}
			return;
		}

        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        // Create our texture. This has to be done each time the
        // surface is created.

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        Matrix.setLookAtM(mVMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        GLES20.glViewport(0, 0, mWidth, mHeight);

		if (bitmap != null) {
			float mx = ((float)mWidth / bitmap.getWidth()) / 2.0f;
			float my = ((float)mHeight / bitmap.getHeight()) / 2.0f;
			Matrix.orthoM(mProjMatrix, 0, -mx, mx, my, -my, 0, 10);
			int value = bitmap.getPixel(0, 0);
			Color color = new Color();
			GLES20.glClearColor(
					(float)color.red(value) / 255.0f,
					(float)color.green(value) / 255.0f,
					(float)color.blue(value) / 255.0f,
					0.0f);
		} else {
			Matrix.orthoM(mProjMatrix, 0, -1, 1, -1, 1, 0, 10);
			GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		}

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");

        Matrix.setRotateM(mMMatrix, 0, 0, 0, 0, 1.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        checkGlError("glDrawArrays");
		mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);

        // Wait to be notified it's okay to start Python.
        synchronized (this) {
            while (!mStarted) {
                // Draw & Flip.
				GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
				GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
                mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);

                try {
                    this.wait(250);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        // Delete texture.
        GLES20.glDeleteTextures(1, textures, 0);
		if (bitmap != null)
			bitmap.recycle();

		// Delete program
		GLES20.glDeleteProgram(mProgram);
    }


    public void start() {
        this.setFocusableInTouchMode(true);
        this.setFocusable(true);
        this.requestFocus();

        synchronized (this) {
            mStarted = true;
            this.notify();
        }

    }

    public boolean createSurface() {
        mChanged = false;

        // Destroy the old surface.
        if (mEglSurface != null) {

            /*
             * Unbind and destroy the old EGL surface, if
             * there is one.
             */
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
        }

        // Create a new surface.
        mEglSurface = mEgl.eglCreateWindowSurface(
            mEglDisplay, mEglConfig, getHolder(), null);

        // Make the new surface current.
        boolean rv = mEgl.eglMakeCurrent(
            mEglDisplay, mEglSurface, mEglSurface, mEglContext);
		if (!rv) {
			mEglSurface = null;
			return false;
		}

        if (mStarted) {
            nativeResize(mWidth, mHeight);
        }

		return true;

    }

    public int swapBuffers() {
        // If the display has been changed, then disregard all the
        // rendering we've done to it, and make a new surface.
        //
        // Otherwise, swap the buffers.
        if (mChanged) {
            createSurface();
            mClears = 2;
            return 0;

        } else {
            mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
            if (mClears-- > 0)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return 1;
        }

    }

	private static final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;

    @Override
    public boolean onTouchEvent(final MotionEvent event) {

		if (mInputActivated == false)
			return true;

		int action = event.getAction() & MotionEvent.ACTION_MASK;
		int sdlAction = -1;
		int pointerId = -1;
		int pointerIndex = -1;

		switch ( action ) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				sdlAction = 0;
				break;
			case MotionEvent.ACTION_MOVE:
				sdlAction = 2;
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				sdlAction = 1;
				break;
		}

		// http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
		switch ( action  & MotionEvent.ACTION_MASK ) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_UP:
				pointerIndex = event.findPointerIndex(mActivePointerId);
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_UP:
				pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
					>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				if ( action == MotionEvent.ACTION_POINTER_UP ) {
					pointerId = event.getPointerId(pointerIndex);
					if ( pointerId == mActivePointerId )
						mActivePointerId = event.getPointerId(pointerIndex == 0 ? 1 : 0);
				}
				break;
		}

		if ( sdlAction >= 0 ) {

			for ( int i = 0; i < event.getPointerCount(); i++ ) {

				if ( pointerIndex == -1 || pointerIndex == i ) {

					/**
        			Log.i("python", String.format("mouse id=%d action=%d x=%f y=%f",
							event.getPointerId(i),
							sdlAction,
							event.getX(i),
							event.getY(i)
					));
					**/
					SDLSurfaceView.nativeMouse(
							(int)event.getX(i),
							(int)event.getY(i),
							sdlAction,
							event.getPointerId(i),
							(int)(event.getPressure(i) * 1000.0),
							(int)(event.getSize(i) * 1000.0));
				}

			}

		}

        return true;
    };

    @Override
    public boolean onKeyDown(int keyCode, final KeyEvent event) {
        //Log.i("python", String.format("key down %d", keyCode));
        if (mInputActivated && nativeKey(keyCode, 1, event.getUnicodeChar())) {
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, final KeyEvent event) {
        //Log.i("python", String.format("key up %d", keyCode));
        if (mInputActivated && nativeKey(keyCode, 0, event.getUnicodeChar())) {
            return true;
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    static void activateInput() {
        mInputActivated = true;
    }

	static void openUrl(String url) {
		Log.i("python", "Opening URL: " + url);

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		mActivity.startActivity(i);
	}

	// Taken from the "GLES20TriangleRenderer" in Android SDK
	private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private final float[] mTriangleVerticesData = {
		// X, Y, Z, U, V
		-0.5f, -0.5f, 0, 1.0f, 0.0f,
		0.5f, -0.5f, 0, 0.0f, 0.0f,
		0.5f, 0.5f, 0, 0.0f, 1.0f,
		-0.5f, -0.5f, 0, 1.0f, 0.0f,
		0.5f, 0.5f, 0, 0.0f, 1.0f,
		-0.5f, 0.5f, 0, 1.0f, 1.0f,
	};

    private FloatBuffer mTriangleVertices;

    private float[] mMVPMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private int mProgram;
    private int mTextureID;
    private int muMVPMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;

	// Native part

    public static native void nativeSetEnv(String name, String value);
    public static native void nativeInit();

    public static native void nativeMouse( int x, int y, int action, int pointerId, int pressure, int radius );
    public static native boolean nativeKey(int keyCode, int down, int unicode);
    public static native void nativeSetMouseUsed();
    public static native void nativeSetMultitouchUsed();

    public native void nativeResize(int width, int height);
    public native void nativeInitJavaCallbacks();

}
