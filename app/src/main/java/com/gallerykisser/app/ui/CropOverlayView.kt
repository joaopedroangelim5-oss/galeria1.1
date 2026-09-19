package com.gallerykisser.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import kotlin.math.hypot
import kotlin.math.min

/**
 * Overlay desenhado por cima da imagem em edição, com um retângulo de corte que o
 * usuário pode mover (arrastando o meio) ou redimensionar (arrastando os cantos).
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val rect = RectF()
    private val handleRadius = 28f

    private val paintBorder = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val paintHandle = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintDim = Paint().apply {
        color = Color.parseColor("#AA000000")
    }

    private enum class DragMode { NONE, MOVE, TL, TR, BL, BR }
    private var dragMode = DragMode.NONE
    private var lastX = 0f
    private var lastY = 0f

    /** Prepara um retângulo inicial (10% de margem) baseado no tamanho atual da view de referência. */
    fun resetRect(reference: View) {
        post {
            val marginX = reference.width * 0.1f
            val marginY = reference.height * 0.1f
            rect.set(marginX, marginY, reference.width - marginX, reference.height - marginY)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rect.isEmpty) return

        canvas.drawRect(0f, 0f, width.toFloat(), rect.top, paintDim)
        canvas.drawRect(0f, rect.bottom, width.toFloat(), height.toFloat(), paintDim)
        canvas.drawRect(0f, rect.top, rect.left, rect.bottom, paintDim)
        canvas.drawRect(rect.right, rect.top, width.toFloat(), rect.bottom, paintDim)

        canvas.drawRect(rect, paintBorder)
        canvas.drawCircle(rect.left, rect.top, handleRadius, paintHandle)
        canvas.drawCircle(rect.right, rect.top, handleRadius, paintHandle)
        canvas.drawCircle(rect.left, rect.bottom, handleRadius, paintHandle)
        canvas.drawCircle(rect.right, rect.bottom, handleRadius, paintHandle)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragMode = when {
                    near(x, y, rect.left, rect.top) -> DragMode.TL
                    near(x, y, rect.right, rect.top) -> DragMode.TR
                    near(x, y, rect.left, rect.bottom) -> DragMode.BL
                    near(x, y, rect.right, rect.bottom) -> DragMode.BR
                    rect.contains(x, y) -> DragMode.MOVE
                    else -> DragMode.NONE
                }
                lastX = x
                lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastX
                val dy = y - lastY
                when (dragMode) {
                    DragMode.MOVE -> rect.offset(dx, dy)
                    DragMode.TL -> { rect.left += dx; rect.top += dy }
                    DragMode.TR -> { rect.right += dx; rect.top += dy }
                    DragMode.BL -> { rect.left += dx; rect.bottom += dy }
                    DragMode.BR -> { rect.right += dx; rect.bottom += dy }
                    DragMode.NONE -> {}
                }
                clampRect()
                lastX = x
                lastY = y
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragMode = DragMode.NONE
        }
        return true
    }

    private fun near(x: Float, y: Float, hx: Float, hy: Float): Boolean =
        hypot((x - hx).toDouble(), (y - hy).toDouble()) < handleRadius * 2.2

    private fun clampRect() {
        if (rect.left < 0) rect.left = 0f
        if (rect.top < 0) rect.top = 0f
        if (rect.right > width) rect.right = width.toFloat()
        if (rect.bottom > height) rect.bottom = height.toFloat()
        val minSize = handleRadius * 4
        if (rect.right - rect.left < minSize) {
            if (dragMode == DragMode.TL || dragMode == DragMode.BL) rect.left = rect.right - minSize
            else rect.right = rect.left + minSize
        }
        if (rect.bottom - rect.top < minSize) {
            if (dragMode == DragMode.TL || dragMode == DragMode.TR) rect.top = rect.bottom - minSize
            else rect.bottom = rect.top + minSize
        }
    }

    /**
     * Converte o retângulo desenhado na tela para coordenadas do bitmap original,
     * levando em conta que a ImageView usa fitCenter (a imagem some centralizada,
     * com "faixas vazias" nas laterais ou em cima/embaixo).
     */
    fun getBitmapRect(imageView: ImageView, bitmapWidth: Int, bitmapHeight: Int): Rect? {
        if (rect.isEmpty || bitmapWidth <= 0 || bitmapHeight <= 0) return null

        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        val scale = min(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
        val displayedWidth = bitmapWidth * scale
        val displayedHeight = bitmapHeight * scale
        val offsetX = (viewWidth - displayedWidth) / 2f
        val offsetY = (viewHeight - displayedHeight) / 2f

        val left = ((rect.left - offsetX) / scale).toInt().coerceIn(0, bitmapWidth)
        val top = ((rect.top - offsetY) / scale).toInt().coerceIn(0, bitmapHeight)
        val right = ((rect.right - offsetX) / scale).toInt().coerceIn(0, bitmapWidth)
        val bottom = ((rect.bottom - offsetY) / scale).toInt().coerceIn(0, bitmapHeight)

        if (right <= left || bottom <= top) return null
        return Rect(left, top, right, bottom)
    }
}
