package com.cre.lashcam.video.videoUtils2.egl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class EncoderRenderer {

    private val vertexShaderCode =
            "precision mediump float;\n" +
                    "attribute vec4 a_position;\n" +
                    "attribute vec2 a_textureCoordinate;\n" +
                    "varying vec2 v_textureCoordinate;\n" +
                    "void main() {\n" +
                    "    v_textureCoordinate = a_textureCoordinate;\n" +
                    "    gl_Position = a_position;\n" +
                    "}"
    private val fragmentShaderCode =
            "precision mediump float;\n" +
                    "varying vec2 v_textureCoordinate;\n" +
                    "uniform sampler2D s_texture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(s_texture, v_textureCoordinate);" +
                    "}"

    private val vertexData = floatArrayOf(-1f, -1f, -1f, 1f, 1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f)
    private lateinit var vertexDataBuffer: FloatBuffer
    private val textureCoordinateData = floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 1f, 1f, 1f, 0f)
    private lateinit var textureCoordinateBuffer: FloatBuffer
    private val VERTEX_COMPONENT_COUNT = 2
    private val TEXTURE_COORDINATE_COMPONENT_COUNT = 2

    private var positionLocation = 0
    private var textureCoordinateLocation = 0
    private var textureLocation = 0

    var width = 0
    var height = 0

    private var programId = 0

    /**
     * 绘制纹理
     */
    fun drawFrame (texture: Int) {
        GLES30.glUseProgram(programId)

        GLES30.glEnableVertexAttribArray(positionLocation)
        GLES30.glVertexAttribPointer(
                positionLocation,
                VERTEX_COMPONENT_COUNT,
                GLES30.GL_FLOAT,
                false,
                0,
                vertexDataBuffer
        )

        GLES30.glEnableVertexAttribArray(textureCoordinateLocation)
        GLES30.glVertexAttribPointer(
                textureCoordinateLocation,
                TEXTURE_COORDINATE_COMPONENT_COUNT,
                GLES30.GL_FLOAT,
                false,
                0,
                textureCoordinateBuffer
        )

        GLES30.glUniform1i(textureLocation, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glClearColor(0.0f, 0.9f, 0.0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexData.size / VERTEX_COMPONENT_COUNT)
    }
    fun init() {
        programId = GLES30.glCreateProgram()

        val vertexShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER)
        val fragmentShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER)
        GLES30.glShaderSource(vertexShader, vertexShaderCode)
        GLES30.glShaderSource(fragmentShader, fragmentShaderCode)
        GLES30.glCompileShader(vertexShader)
        GLES30.glCompileShader(fragmentShader)

        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)

        GLES30.glLinkProgram(programId)

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)

        GLES30.glUseProgram(programId)

        vertexDataBuffer = ByteBuffer.allocateDirect(vertexData.size * java.lang.Float.SIZE / 8)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        vertexDataBuffer.put(vertexData)
        vertexDataBuffer.position(0)
        positionLocation = GLES30.glGetAttribLocation(programId, "a_position")

        textureCoordinateBuffer =
                ByteBuffer.allocateDirect(textureCoordinateData.size * java.lang.Float.SIZE / 8)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer()
        textureCoordinateBuffer.put(textureCoordinateData)
        textureCoordinateBuffer.position(0)
        textureCoordinateLocation = GLES30.glGetAttribLocation(programId, "a_textureCoordinate")

        textureLocation = GLES30.glGetUniformLocation(programId, "s_texture")
        GLES30.glUniform1i(textureLocation, 0)
    }

    fun release() {
        GLES30.glDeleteProgram(programId)
    }
}