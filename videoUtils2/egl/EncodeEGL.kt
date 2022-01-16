package com.cre.lashcam.video.videoUtils2.egl

import android.opengl.*
import android.util.Log
import android.view.Surface
import com.cre.lashcam.video.videoutil.opengl.checkEglError

class EncodeEGL (private var shareContext:EGLContext, private var surface: Surface) {
    companion object {
        private val TAG = EncodeEGL::class.java.simpleName
    }

    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface = EGL14.EGL_NO_SURFACE

    /**
     * 初始化相关数据
     */
    fun init () {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("eglInitialize failed")
        }
        val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,  0x3142, 1, EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
        )

        val eglConfig = arrayOfNulls<android.opengl.EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, eglConfig, 0, eglConfig.size, numConfigs, 0)) {
            throw RuntimeException("eglChooseConfig failed")
        }

        eglContext = EGL14.eglCreateContext(
                eglDisplay, eglConfig[0], shareContext,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
        )
        val values = IntArray(1)
        EGL14.eglQueryContext(
                eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0
        )
        Log.d("debug", "EGLContext created, client version " + values[0])
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay, eglConfig[0], surface,
                surfaceAttribs, 0
        )
    }

    /**
     * 释放资源
     */
    fun release () {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            // 让当前线程操作的 EGLContext 指向默认的 EGL_NO_CONTEXT 对象。解绑
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            // 释放egl环境绑定的线程
            EGL14.eglReleaseThread()
            // 终止 display对象
            EGL14.eglTerminate(eglDisplay)
        }

        surface.release()
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }

    /**
     * 将上下文绑定到当前线程
     */
    fun makeCurrent () {
//        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            checkEglError("EGL make current failed")
        }
    }
    fun swapBuffers():Boolean {
        val result = EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        checkEglError("eglSwapBuffers")
        return result
    }
    fun setTimestamp(timestamp:Long) {
        Log.d(TAG, "setTimestamp = $timestamp")
        Log.d(TAG, "eglPresentationTimeANDROID start")
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, timestamp)
        Log.d(TAG, "eglPresentationTimeANDROID end")
    }
}