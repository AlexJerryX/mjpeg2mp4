package com.cre.lashcam.video.videoUtils2.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import com.cre.LashCamera.R
import java.io.*
import java.text.DecimalFormat


class Utils {
    companion object {
        private var decimalFormat:DecimalFormat ? = null;

        /**
         * 拷贝私有路径下的文件到公共目录下
         * orgFilePath是要复制的文件私有目录路径
         * displayName复制后文件要显示的文件名称带后缀（如xx.txt）
         */
        public fun copyFile2PublicPath(context: Context, originPath: String, displayName: String) {
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            values.put(MediaStore.Video.Media.DESCRIPTION, "This is an video")
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            values.put(MediaStore.Video.Media.TITLE, displayName)
            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/lashcam")

            val external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val insertUri = context.contentResolver.insert(external, values)


            // 拷贝
            var ist: InputStream? = null
            var ost: OutputStream? = null
            try {
                ist = FileInputStream(File(originPath))
                if (insertUri != null) {
                    ost = context.contentResolver.openOutputStream(insertUri)
                }
                if (ost != null) {
                    val buffer = ByteArray(4096)
                    var byteCount = 0
                    while (ist.read(buffer).also { byteCount = it } != -1) {  // 循环从输入流读取 buffer字节
                        ost.write(buffer, 0, byteCount) // 将读取的输入流写入到输出流
                    }
                    // write what you want
                }
            } catch (e: IOException) {
                LogUtil.logd("copyPrivateToDownload--", "fail: " + e.printStackTrace());
            } finally {
                try {
                    if (ist != null) {
                        ist.close()
                    }
                    if (ost != null) {
                        ost.close()
                    }
                } catch (e: IOException) {
                    //Log.i("copyPrivateToDownload--","fail in close: " + e.getCause());
                }
            }

        }

        /**
         * 拷贝私有路径下的文件到公共目录下
         * orgFilePath是要复制的文件私有目录路径
         * displayName复制后文件要显示的文件名称带后缀（如xx.txt）
         */
        public fun copyPic2PublicPath(context: Context, originPath: String, displayName: String) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            values.put(MediaStore.Images.Media.DESCRIPTION, "This is an picture")
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.TITLE, displayName)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LashCamPicture")

            val external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val insertUri = context.contentResolver.insert(external, values)

            val path = getFilePath_below19(context, insertUri)
            // 拷贝
            var ist: InputStream? = null
            var ost: OutputStream? = null
            try {
                ist = FileInputStream(File(originPath))
                if (insertUri != null) {
                    ost = context.contentResolver.openOutputStream(insertUri)
                }
                if (ost != null) {
                    val buffer = ByteArray(4096)
                    var byteCount = 0
                    while (ist.read(buffer).also { byteCount = it } != -1) {  // 循环从输入流读取 buffer字节
                        ost.write(buffer, 0, byteCount) // 将读取的输入流写入到输出流
                    }
                    // write what you want

                }
            } catch (e: IOException) {
                LogUtil.logd("copyPrivateToDownload--", "fail: " + e.printStackTrace());
            } finally {
                try {
                    if (ist != null) {
                        ist.close()
                    }
                    if (ost != null) {
                        ost.close()
                    }
                } catch (e: IOException) {
                    //Log.i("copyPrivateToDownload--","fail in close: " + e.getCause());
                }
            }
            Toast.makeText(context, R.string.tip_save_success_en, Toast.LENGTH_SHORT).show()
        }

        public fun formatTime(time: Long): String {
            if (this.decimalFormat == null) {
                decimalFormat = DecimalFormat("00")
            }
            val hh: String = decimalFormat!!.format(time / 3600)
            val mm: String = decimalFormat!!.format(time % 3600 / 60)
            val ss: String = decimalFormat!!.format(time % 60)

            return hh + ":" + mm + ":" + ss
        }

        /**
         * 获取小于api19时获取相册中图片真正的uri
         * 对于路径是：content://media/external/images/media/33517这种的，需要转成/storage/emulated/0/DCIM/Camera/IMG_20160807_133403.jpg路径，也是使用这种方法
         * @param context
         * @param uri
         * @return
         */
        fun getFilePath_below19(context: Context, uri: Uri?): String? {
            //这里开始的第二部分，获取图片的路径：低版本的是没问题的，但是sdk>19会获取不到
            var cursor: Cursor? = null
            var path: String? = ""
            try {
                val proj = arrayOf(MediaStore.Images.Media.DATA)
                //好像是android多媒体数据库的封装接口，具体的看Android文档
                cursor = context.contentResolver.query(uri!!, proj, null, null, null)
                //获得用户选择的图片的索引值
                val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                //将光标移至开头 ，这个很重要，不小心很容易引起越界
                cursor.moveToFirst()
                //最后根据索引值获取图片路径   结果类似：/mnt/sdcard/DCIM/Camera/IMG_20151124_013332.jpg
                path = cursor.getString(column_index)
            } finally {
                cursor?.close()
                return path
            }
        }

    }
}