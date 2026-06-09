package com.example.qiblaapp2

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
/**
 * Neumorphic toggle: concave track, convex thumb, warm milk palette.
 */
class NeumorphicSwitchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val trackWidthPx = resources.getDimension(R.dimen.neo_switch_track_width)
    private val trackHeightPx = resources.getDimension(R.dimen.neo_switch_track_height)
    private val thumbMarginPx = resources.getDimension(R.dimen.neo_switch_thumb_margin)
    private val trackRadiusPx = trackHeightPx / 2f

    private val trackColor = ContextCompat.getColor(context, R.color.neo_switch_track)
    private val thumbTop = ContextCompat.getColor(context, R.color.neo_switch_thumb_top)
    private val thumbBottom = ContextCompat.getColor(context, R.color.neo_switch_thumb_bottom)
    private val shadowColor = ContextCompat.getColor(context, R.color.neo_icon_emboss_shadow)
    private val highlightColor = ContextCompat.getColor(context, R.color.neo_icon_emboss_highlight)
    private val ledOnColor = ContextCompat.getColor(context, R.color.neo_switch_led_on)
    private val ledOffColor = ContextCompat.getColor(context, R.color.neo_switch_led_off)

    private val trackRect = RectF()
    private val thumbRect = RectF()
    private val thumbShadowRect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val thumbShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ledGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var checked = false
    private var thumbPosition = 0f
    private var animator: ValueAnimator? = null
    private var checkedChangeListener: ((NeumorphicSwitchView, Boolean) -> Unit)? = null
    private var suppressListener = false

    var isChecked: Boolean
        get() = checked
        set(value) = setChecked(value, animate = isLaidOut)

    init {
        isClickable = true
        isFocusable = true
        minimumWidth = trackWidthPx.toInt()
        minimumHeight = trackHeightPx.toInt()
    }

    fun setChecked(value: Boolean, animate: Boolean = false) {
        if (checked == value) return
        checked = value
        val target = if (value) 1f else 0f
        if (animate) {
            animateThumbTo(target)
        } else {
            animator?.cancel()
            thumbPosition = target
            invalidate()
        }
        if (!suppressListener) {
            checkedChangeListener?.invoke(this, value)
        }
    }

    fun setOnCheckedChangeListener(listener: (NeumorphicSwitchView, Boolean) -> Unit) {
        checkedChangeListener = listener
    }

    fun setCheckedSilently(value: Boolean) {
        suppressListener = true
        setChecked(value, animate = false)
        suppressListener = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(trackWidthPx.toInt(), widthMeasureSpec)
        val h = resolveSize(trackHeightPx.toInt(), heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val left = (w - trackWidthPx) / 2f
        val top = (h - trackHeightPx) / 2f
        trackRect.set(left, top, left + trackWidthPx, top + trackHeightPx)
        updateThumbRect()
    }

    override fun performClick(): Boolean {
        super.performClick()
        setChecked(!checked, animate = true)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        drawConcaveTrack(canvas)
        drawRaisedThumb(canvas)
    }

    private fun drawConcaveTrack(canvas: Canvas) {
        trackPaint.shader = LinearGradient(
            trackRect.left,
            trackRect.top,
            trackRect.right,
            trackRect.bottom,
            intArrayOf(
                Color.argb(100, 255, 255, 255),
                trackColor,
                Color.argb(70, 110, 100, 90),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(trackRect, trackRadiusPx, trackRadiusPx, trackPaint)
        trackPaint.shader = null

        val inset = 1.25f * density
        val inner = RectF(trackRect).apply { inset(inset, inset) }
        val innerRadius = (trackRadiusPx - inset).coerceAtLeast(0f)
        rimPaint.strokeWidth = 1.25f * density
        rimPaint.color = Color.argb(120, 255, 255, 255)
        canvas.drawRoundRect(inner, innerRadius, innerRadius, rimPaint)
        rimPaint.color = Color.argb(55, 90, 80, 70)
        val innerDark = RectF(inner).apply { inset(0.5f * density, 0.5f * density) }
        canvas.drawRoundRect(
            innerDark,
            innerRadius - 0.5f * density,
            innerRadius - 0.5f * density,
            rimPaint,
        )
    }

    private fun drawRaisedThumb(canvas: Canvas) {
        updateThumbRect()
        val thumbRadius = thumbRect.height() / 2f
        val depth = 1.5f * density

        thumbShadowRect.set(
            thumbRect.left + depth * 0.55f,
            thumbRect.top + depth * 0.65f,
            thumbRect.right + depth * 0.55f,
            thumbRect.bottom + depth * 0.65f,
        )
        thumbShadowPaint.color = Color.argb(90, Color.red(shadowColor), Color.green(shadowColor), Color.blue(shadowColor))
        canvas.drawRoundRect(thumbShadowRect, thumbRadius, thumbRadius, thumbShadowPaint)

        val highlightRect = RectF(
            thumbRect.left - depth * 0.35f,
            thumbRect.top - depth * 0.35f,
            thumbRect.right - depth * 0.35f,
            thumbRect.bottom - depth * 0.35f,
        )
        thumbHighlightPaint.color = Color.argb(130, 255, 255, 255)
        canvas.drawRoundRect(highlightRect, thumbRadius, thumbRadius, thumbHighlightPaint)

        thumbPaint.shader = LinearGradient(
            thumbRect.left,
            thumbRect.top,
            thumbRect.right,
            thumbRect.bottom,
            intArrayOf(thumbTop, thumbBottom),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(thumbRect, thumbRadius, thumbRadius, thumbPaint)
        thumbPaint.shader = null

        val ledRadius = 2.75f * density
        val ledCx = thumbRect.left + 8f * density
        val ledCy = thumbRect.centerY()
        val ledColor = if (checked) ledOnColor else ledOffColor
        if (checked) {
            ledGlowPaint.color = Color.argb(90, Color.red(ledOnColor), Color.green(ledOnColor), Color.blue(ledOnColor))
            canvas.drawCircle(ledCx, ledCy, ledRadius * 2.2f, ledGlowPaint)
        }
        ledPaint.color = ledColor
        canvas.drawCircle(ledCx, ledCy, ledRadius, ledPaint)
    }

    private fun updateThumbRect() {
        val thumbHeight = trackRect.height() - thumbMarginPx * 2f
        val thumbWidth = thumbHeight * 1.55f
        val travel = trackRect.width() - thumbMarginPx * 2f - thumbWidth
        val left = trackRect.left + thumbMarginPx + travel * thumbPosition
        val top = trackRect.top + thumbMarginPx
        thumbRect.set(left, top, left + thumbWidth, top + thumbHeight)
    }

    private fun animateThumbTo(target: Float) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(thumbPosition, target).apply {
            duration = 280L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                thumbPosition = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
