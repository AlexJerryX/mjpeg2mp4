package com.cre.lashcam.video.videoUtils2.utils;

import android.util.Log

class LogUtil {

    companion object {

        private val enableLog = true

        fun logd(tag : String, msg : String) {
            if (enableLog) {
                Log.d(tag, msg)
            }
        }

        fun loge(tag : String, msg : String) {
            if (enableLog) {
                Log.e(tag, msg)
            }
        }

        fun loge(e: Exception) {
            if (enableLog) {
                e.printStackTrace()
            }
        }

        fun loge(e : Throwable) {
            if (enableLog) {
                e.printStackTrace()
            }
        }

    }

}