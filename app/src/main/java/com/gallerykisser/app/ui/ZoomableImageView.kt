package com.gallerykisser.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * ImageView com zoom por pinça (pinch-to-zoom) de 1x até [MAX_SCALE], e arraste (pan)
 * quando a imagem está ampliada. Duplo toque alterna entre 1x e um zoom médio.
 *
 * IMPORTANTE: sempre trabalhamos numa CÓPIA nova da matriz (Matrix(imageMatrix)) antes de
 * mudar e reatribuir. Se mutarmos o objeto que imageMatrix devolve e reatribuirmos ele
 * mesmo, o ImageView entende que "nada mudou" (é o mesmo objeto) e nunca redesenha a tela.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    companion object {
        private const val MIN_SCALE = 1f
        private const val MAX_SCALE = 32f
        private const val DOUBLE_TAP_SCALE = 4f
    }

    private var currentScale = 1f
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var activePointers = 0

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = (currentScale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
            val factor = newScale / currentScale
            currentScale = newScale

            val newMatrix = Matrix(imageMatrix)
            newMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
            imageMatrix = newMatrix

            clampTranslation()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val target = if (currentScale > MIN_SCALE + 0.01f) MIN_SCALE else DOUBLE_TAP_SCALE
            val factor = target / currentScale
            currentScale = target

            val newMatrix = Matrix(imageMatrix)
            newMatrix.postScale(factor, factor, e.x, e.y)
            imageMatrix = newMatrix

            clampTranslation()
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    /** Chame ao trocar de imagem para começar de novo em 1x, centralizada. */
    fun resetZoom() {
        currentScale = 1f
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        fitToCenter()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        fitToCenter()
    }

    /** Reproduz manualmente o comportamento do scaleType "fitCenter", já que aqui usamos MATRIX. */
    private fun fitToCenter() {
        currentScale = 1f
        val d = drawable ?: return
        if (width == 0 || height == 0) {
            post { fitToCenter() }
            return
        }
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (dw <= 0 || dh <= 0) return
        val scale = minOf(width / dw, height / dh)
        val dx = (width - dw * scale) / 2f
        val dy = (height - dh * scale) / 2f
        val newMatrix = Matrix()
        newMatrix.postScale(scale, scale)
        newMatrix.postTranslate(dx, dy)
        imageMatrix = newMatrix
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointers = 1
                lastFocusX = event.x
                lastFocusY = event.y
                parent?.requestDisallowInterceptTouchEvent(currentScale > MIN_SCALE + 0.01f)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                activePointers++
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && activePointers == 1 && currentScale > MIN_SCALE + 0.01f) {
                    val dx = event.x - lastFocusX
                    val dy = event.y - lastFocusY

                    val newMatrix = Matrix(imageMatrix)
                    newMatrix.postTranslate(dx, dy)
                    imageMatrix = newMatrix

                    clampTranslation()
                    lastFocusX = event.x
                    lastFocusY = event.y
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                activePointers--
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointers = 0
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    /** Evita que a imagem seja arrastada para fora da tela quando ampliada. */
    private fun clampTranslation() {
        val drawableRef = drawable ?: return
        val values = FloatArray(9)
        imageMatrix.getValues(values)

        val drawableWidth = drawableRef.intrinsicWidth * values[Matrix.MSCALE_X]
        val drawableHeight = drawableRef.intrinsicHeight * values[Matrix.MSCALE_Y]

        var dx = 0f
        var dy = 0f

        if (drawableWidth <= width) {
            dx = (width - drawableWidth) / 2f - values[Matrix.MTRANS_X]
        } else {
            val minX = width - drawableWidth
            val maxX = 0f
            val currentX = values[Matrix.MTRANS_X]
            dx = when {
                currentX > maxX -> maxX - currentX
                currentX < minX -> minX - currentX
                else -> 0f
            }
        }

        if (drawableHeight <= height) {
            dy = (height - drawableHeight) / 2f - values[Matrix.MTRANS_Y]
        } else {
            val minY = height - drawableHeight
            val maxY = 0f
            val currentY = values[Matrix.MTRANS_Y]
            dy = when {
                currentY > maxY -> maxY - currentY
                currentY < minY -> minY - currentY
                else -> 0f
            }
        }

        if (dx != 0f || dy != 0f) {
            val newMatrix = Matrix(imageMatrix)
            newMatrix.postTranslate(dx, dy)
            imageMatrix = newMatrix
        }
    }
}
