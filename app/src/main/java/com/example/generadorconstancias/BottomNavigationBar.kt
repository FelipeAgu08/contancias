package com.example.generadorconstancias

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat


class BottomNavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    // Colores
    private val backgroundColor = Color.parseColor("#FFFFFF")
    private val selectedColor = Color.parseColor("#7B61FF")
    private val unselectedColor = Color.parseColor("#9E9E9E")
    private val indicatorColor = Color.parseColor("#EDE7FF")

    // Propiedades de animación
    private var selectedIndex = 0
    private var indicatorX = 0f
    private var indicatorRadius = 0f
    private val maxIndicatorRadius = 75f

    // Definir los ítems de navegación
    data class NavItem(
        val icon: (Canvas, Float, Float, Float, Paint) -> Unit,
        val label: String
    )

    private val navItems = listOf(
        NavItem(::drawHomeIcon, "Home"),
        NavItem(::drawHistoryIcon, "Historial"),
        NavItem(::drawChartIcon, "Gráficas")
    )

    private var itemWidth = 0f
    private var onItemSelectedListener: ((Int) -> Unit)? = null

    init {
        paint.style = Paint.Style.FILL
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = 5f
        iconPaint.strokeCap = Paint.Cap.ROUND
        iconPaint.strokeJoin = Paint.Join.ROUND

        textPaint.style = Paint.Style.FILL
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                handleTouch(event.x)
                performClick()
                true
            } else {
                false
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun setOnNavigationItemSelectedListener(listener: (Int) -> Unit) {
        onItemSelectedListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        itemWidth = w.toFloat() / navItems.size
        indicatorX = itemWidth * selectedIndex + itemWidth / 2
        indicatorRadius = maxIndicatorRadius
    }

    private fun handleTouch(x: Float) {
        val newIndex = (x / itemWidth).toInt().coerceIn(0, navItems.size - 1)
        if (newIndex != selectedIndex) {
            animateToIndex(newIndex)
            onItemSelectedListener?.invoke(newIndex)
        }
    }

    private fun animateToIndex(newIndex: Int) {
        val startX = indicatorX
        val endX = itemWidth * newIndex + itemWidth / 2

        // Animación de posición del indicador
        val positionAnimator = ValueAnimator.ofFloat(startX, endX)
        positionAnimator.duration = 400
        positionAnimator.interpolator = OvershootInterpolator(2f)
        positionAnimator.addUpdateListener { animation ->
            indicatorX = animation.animatedValue as Float
            invalidate()
        }

        // Animación del radio (efecto rebote)
        val radiusAnimator = ValueAnimator.ofFloat(
            maxIndicatorRadius,
            maxIndicatorRadius * 1.3f,
            maxIndicatorRadius * 0.9f,
            maxIndicatorRadius
        )
        radiusAnimator.duration = 400
        radiusAnimator.addUpdateListener { animation ->
            indicatorRadius = animation.animatedValue as Float
        }

        selectedIndex = newIndex
        positionAnimator.start()
        radiusAnimator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Dibujar fondo con bordes redondeados superiores
        path.reset()
        val cornerRadius = 35f
        path.moveTo(0f, cornerRadius)
        path.quadTo(0f, 0f, cornerRadius, 0f)
        path.lineTo(width - cornerRadius, 0f)
        path.quadTo(width.toFloat(), 0f, width.toFloat(), cornerRadius)
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()

        paint.color = backgroundColor
        paint.setShadowLayer(20f, 0f, -5f, Color.parseColor("#30000000"))
        canvas.drawPath(path, paint)
        paint.clearShadowLayer()

        // Dibujar indicador circular
        if (indicatorRadius > 0) {
            paint.color = indicatorColor
            paint.setShadowLayer(
                15f,
                0f,
                0f,
                Color.parseColor("#40" + Integer.toHexString(selectedColor).substring(2))
            )
            canvas.drawCircle(indicatorX, height / 2.3f, indicatorRadius, paint)
            paint.clearShadowLayer()
        }

        // Dibujar iconos y labels
        for (i in navItems.indices) {
            val x = itemWidth * i + itemWidth / 2
            val isSelected = i == selectedIndex

            // Configurar color
            iconPaint.color = if (isSelected) selectedColor else unselectedColor

            // Efecto de elevación y escala
            val scale = if (isSelected) 1.15f else 1f
            val yOffset = if (isSelected) -10f else 0f
            val iconY = height / 2.3f + yOffset

            // Dibujar icono
            canvas.save()
            canvas.translate(x, iconY)
            canvas.scale(scale, scale)
            navItems[i].icon(canvas, 0f, 0f, 24f, iconPaint)
            canvas.restore()

            // Dibujar label solo para el seleccionado
            if (isSelected) {
                textPaint.color = selectedColor
                textPaint.textSize = 28f
                canvas.drawText(navItems[i].label, x, height - 22f, textPaint)
            }
        }
    }

    // ICONOS DIBUJADOS CON PATHS

    private fun drawHomeIcon(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        val path = Path()
        // Casa
        path.moveTo(x, y - size / 3)
        path.lineTo(x - size / 2, y + size / 6)
        path.lineTo(x - size / 2, y + size / 2)
        path.lineTo(x + size / 2, y + size / 2)
        path.lineTo(x + size / 2, y + size / 6)
        path.lineTo(x, y - size / 3)

        // Puerta
        path.moveTo(x - size / 6, y + size / 2)
        path.lineTo(x - size / 6, y + size / 6)
        path.lineTo(x + size / 6, y + size / 6)
        path.lineTo(x + size / 6, y + size / 2)

        canvas.drawPath(path, paint)
    }

    private fun drawHistoryIcon(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        // Círculo del reloj
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(x, y, size / 2, paint)

        paint.style = Paint.Style.FILL
        // Manecilla de hora
        canvas.drawLine(x, y, x, y - size / 3, paint)
        // Manecilla de minuto
        canvas.drawLine(x, y, x + size / 3, y, paint)

        // Flecha circular (indicando rotación/historial)
        val arrowPath = Path()
        arrowPath.moveTo(x - size / 2 - 5, y - size / 3)
        arrowPath.lineTo(x - size / 2 + 5, y - size / 3)
        arrowPath.lineTo(x - size / 2, y - size / 3 - 10)
        arrowPath.close()
        paint.style = Paint.Style.FILL
        canvas.drawPath(arrowPath, paint)

        paint.style = Paint.Style.STROKE
    }

    private fun drawChartIcon(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        val barWidth = size / 6
        val spacing = size / 8

        // Barra 1 (pequeña)
        val bar1Height = size / 3
        canvas.drawRoundRect(
            x - size / 2 + spacing,
            y + size / 2 - bar1Height,
            x - size / 2 + spacing + barWidth,
            y + size / 2,
            8f, 8f, paint
        )

        // Barra 2 (mediana)
        val bar2Height = size / 1.5f
        canvas.drawRoundRect(
            x - barWidth / 2,
            y + size / 2 - bar2Height,
            x + barWidth / 2,
            y + size / 2,
            8f, 8f, paint
        )

        // Barra 3 (grande)
        val bar3Height = size / 1.2f
        canvas.drawRoundRect(
            x + size / 2 - spacing - barWidth,
            y + size / 2 - bar3Height,
            x + size / 2 - spacing,
            y + size / 2,
            8f, 8f, paint
        )

        // Línea base
        paint.strokeWidth = 3f
        canvas.drawLine(
            x - size / 2 - 5,
            y + size / 2,
            x + size / 2 + 5,
            y + size / 2,
            paint
        )
        paint.strokeWidth = 5f
    }
}