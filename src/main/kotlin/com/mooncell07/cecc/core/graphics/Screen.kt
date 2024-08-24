package com.mooncell07.cecc.core.graphics

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color

class Screen(
    scale: Double = 2.0,
) {
    private val WIDTH = 256
    private val HEIGHT = 240
    val canvas: Canvas = Canvas(WIDTH * scale, HEIGHT * scale)
    private val graphicsContext: GraphicsContext = canvas.graphicsContext2D
    private val buffer: WritableImage = WritableImage(WIDTH, HEIGHT)
    private val pixelWriter: PixelWriter = buffer.pixelWriter
    private var bufferIndex = 0

    init {
        graphicsContext.scale(scale, scale)
    }

    fun drawPixel(color: Color) {
        pixelWriter.setColor(bufferIndex % WIDTH, bufferIndex / WIDTH, color)
        bufferIndex += 1
    }

    fun render() {
        graphicsContext.drawImage(buffer, 0.0, 0.0)
        bufferIndex = 0
    }
}
