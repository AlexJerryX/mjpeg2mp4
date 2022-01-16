package com.cre.lashcam.video.videoUtils2.encode

import android.media.*
import android.opengl.EGLContext
import android.opengl.GLUtils
import com.cre.lashcam.video.videoUtils2.egl.EncodeEGL
import com.cre.lashcam.video.videoUtils2.egl.EncoderRenderer
import com.cre.lashcam.video.videoUtils2.utils.GLUtil
import com.cre.lashcam.video.videoUtils2.utils.LogUtil

import java.io.File

class VideoEncoder {
    companion object {
        private val TAG = VideoEncoder::class.java.simpleName
    }

    private val bufferInfo = MediaCodec.BufferInfo()
    private lateinit var mediaCodec: MediaCodec
    private lateinit var egl: EncodeEGL
    private lateinit var mediaMuxer: MediaMuxer
    private var encodeWidth = 0
    private var encodeHeight = 0
    private var trackIndex = 0
    private var muxerStarted = false
    private lateinit var encodeRenderer: EncoderRenderer
    private lateinit var outputPath: String
    private lateinit var shareContext: EGLContext

    fun init () {
        // 配置.h264格式
        val format = MediaFormat.createVideoFormat(EncoderConfig.MIME_TYPE, encodeWidth, encodeHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, EncoderConfig.BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, EncoderConfig.FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, EncoderConfig.FRAME_INTERVAL)
        }
        // 构建对象
        mediaCodec = MediaCodec.createEncoderByType(EncoderConfig.MIME_TYPE)
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        // 初始化EGL环境
        egl = EncodeEGL(shareContext, mediaCodec.createInputSurface()).apply {
            init()
            makeCurrent()
        }
        // 构建render对象
        encodeRenderer = EncoderRenderer().apply {
            this.width = encodeWidth
            this.height = encodeHeight
            init()
        }

        // 开始接受输入队列进行编码处理
        mediaCodec.start()
        // 创建输出的视频文件路径
        var file = File(outputPath)
        if (!file.exists()) {
            file.createNewFile()
        }
        // 初始化媒体混合器对象 设置为mp4组装格式
        mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        trackIndex = -1
        muxerStarted = false
    }

    fun setOutputPath(outputPath: String) {
        this.outputPath = outputPath
    }

    fun setShareContext(shareContext: EGLContext) {
        this.shareContext = shareContext
    }

    fun setEncodeSize(encodeWidth: Int, encodeHeight: Int) {
        this.encodeWidth = encodeWidth
        this.encodeHeight = encodeHeight
    }

    /**
     * 编码帧数据
     */
    fun encodeFrame (texture: Int, timestamp: Long) {
        if (texture != 0) {
            // 绘制纹理
            encodeRenderer.drawFrame(texture)
            egl.setTimestamp(timestamp)
            egl.swapBuffers()
            GLUtil.checkGLError()
            drainEncoder(false)
        } else {
            drainEncoder(true)
        }
    }

    /**
     * 输出处理
     */
    fun drainEncoder (endOfStream: Boolean) {
        try {
            if (endOfStream) {
                mediaCodec.signalEndOfInputStream()
            }
            var encoderOutputBuffers = mediaCodec.outputBuffers
            while (true) {
                val bufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) {
                        break
                    }
                } else if (bufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = mediaCodec.outputBuffers
                } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        mediaCodec.releaseOutputBuffer(bufferIndex, false)
                        continue
                    }
                    trackIndex = mediaMuxer.addTrack(mediaCodec.outputFormat)
                    mediaMuxer.start()
                    muxerStarted = true
                } else {
                    val encodedData = encoderOutputBuffers[bufferIndex]
                    if (encodedData == null) {
                        mediaCodec.releaseOutputBuffer(bufferIndex, false)
                        continue
                    } else {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) {
                                mediaCodec.releaseOutputBuffer(bufferIndex, false)
                                continue
                            }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        mediaCodec.releaseOutputBuffer(bufferIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    }
                }
            }
        } catch (e: RuntimeException) {
            LogUtil.logd(TAG, e.toString())
        }
    }

    /**
     * release
     */
    fun release() {
        mediaCodec.stop()
        mediaCodec.release()
        egl.release()
        mediaMuxer.stop()
        mediaMuxer.release()
        encodeRenderer.release()
    }
}