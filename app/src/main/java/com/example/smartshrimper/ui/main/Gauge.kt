package com.example.smartshrimper.ui.main

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Parcelable
import android.os.Bundle
import android.text.Spanned
import android.text.Html
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.smartshrimper.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class Gauge : View {
    private var needlePaint: Paint? = null
    private var needlePath: Path? = null
    private var needleScrewPaint: Paint? = null
    private var canvasCenterX = 0f
    private var canvasCenterY = 0f
    private var canvasWidth = 0f
    private var canvasHeight = 0f
    private var needleTailLength = 0f
    private var needleWidth = 0f
    private var needleLength = 0f
    private var rimRect: RectF? = null
    private var rimPaint: Paint? = null
    private var rimCirclePaint: Paint? = null
    private var faceRect: RectF? = null
    private var facePaint: Paint? = null
    private var rimShadowPaint: Paint? = null
    private var scalePaint: Paint? = null
    private var scaleRect: RectF? = null
    private var totalNicks = 120 // on a full circle
    private var degreesPerNick = (totalNicks / 360).toFloat()
    private var valuePerNick = 10f
    private var minValue = 0f
    private var maxValue = 1000f
    private var intScale = true
    private var requestedLabelTextSize = 0f
    private var initialValue = 0f
    private var value = 0f
    private var needleValue = 0f
    private var needleStep = 0f
    private var centerValue = 0f
    private var labelRadius = 0f
    private var majorNickInterval = 10
    private var deltaTimeInterval = 5
    private var needleStepFactor = 3f
    private var labelPaint: Paint? = null
    private var lastMoveTime: Long = 0
    private var needleShadow = true
    private var faceColor = 0
    private var scaleColor = 0
    private var needleColor = 0
    private var upperTextPaint: Paint? = null
    private var lowerTextPaint: Paint? = null
    private var requestedTextSize = 0f
    private var requestedUpperTextSize = 0f
    private var requestedLowerTextSize = 0f
    private var upperText = ""
    private var lowerText = ""
    private var textScaleFactor = 0f

    constructor(context: Context?) : super(context) {
        initValues()
        initPaint()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        applyAttrs(context, attrs)
        initValues()
        initPaint()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        applyAttrs(context, attrs)
        initValues()
        initPaint()
    }

    private fun applyAttrs(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Gauge, 0, 0)
        totalNicks = a.getInt(R.styleable.Gauge_totalNicks, totalNicks)
        degreesPerNick = 360.0f / totalNicks
        valuePerNick = a.getFloat(R.styleable.Gauge_valuePerNick, valuePerNick)
        majorNickInterval = a.getInt(R.styleable.Gauge_majorNickInterval, 10)
        minValue = a.getFloat(R.styleable.Gauge_minValue, minValue)
        maxValue = a.getFloat(R.styleable.Gauge_maxValue, maxValue)
        intScale = a.getBoolean(R.styleable.Gauge_intScale, intScale)
        initialValue = a.getFloat(R.styleable.Gauge_initialValue, initialValue)
        requestedLabelTextSize = a.getFloat(R.styleable.Gauge_labelTextSize, requestedLabelTextSize)
        faceColor = a.getColor(R.styleable.Gauge_faceColor, Color.argb(0xff, 0xff, 0xff, 0xff))
        scaleColor = a.getColor(R.styleable.Gauge_scaleColor, -0x60ffb2f1)
        needleColor = a.getColor(R.styleable.Gauge_needleColor, Color.RED)
        needleShadow = a.getBoolean(R.styleable.Gauge_needleShadow, needleShadow)
        requestedTextSize = a.getFloat(R.styleable.Gauge_textSize, requestedTextSize)
        upperText = if (a.getString(R.styleable.Gauge_upperText) == null) upperText else fromHtml(
            a.getString(R.styleable.Gauge_upperText)
        ).toString()
        lowerText = if (a.getString(R.styleable.Gauge_lowerText) == null) lowerText else fromHtml(
            a.getString(R.styleable.Gauge_lowerText)
        ).toString()
        requestedUpperTextSize = a.getFloat(R.styleable.Gauge_upperTextSize, 0f)
        requestedLowerTextSize = a.getFloat(R.styleable.Gauge_lowerTextSize, 0f)
        a.recycle()
        validate()
    }

    private fun initValues() {
        needleStep = needleStepFactor * valuePerDegree()
        centerValue = (minValue + maxValue) / 2
        value = initialValue
        needleValue = value
        val widthPixels = resources.displayMetrics.widthPixels
        textScaleFactor = widthPixels.toFloat() / REF_MAX_PORTRAIT_CANVAS_SIZE.toFloat()
        if (resources.getBoolean(R.bool.landscape)) {
            val heightPixels = resources.displayMetrics.heightPixels
            val portraitAspectRatio = heightPixels.toFloat() / widthPixels.toFloat()
            textScaleFactor *= portraitAspectRatio
        }
    }

    private fun initPaint() {
        isSaveEnabled = true

        // Rim and shadow are based on the Vintage Thermometer:
        // http://mindtherobot.com/blog/272/android-custom-ui-making-a-vintage-thermometer/
        rimPaint = Paint()
        rimPaint!!.flags = Paint.ANTI_ALIAS_FLAG
        rimCirclePaint = Paint()
        rimCirclePaint!!.isAntiAlias = true
        rimCirclePaint!!.style = Paint.Style.STROKE
        rimCirclePaint!!.color = Color.argb(0x4f, 0x33, 0x36, 0x33)
        rimCirclePaint!!.strokeWidth = 0.005f
        facePaint = Paint()
        facePaint!!.isAntiAlias = true
        facePaint!!.style = Paint.Style.FILL
        facePaint!!.color = faceColor
        rimShadowPaint = Paint()
        rimShadowPaint!!.style = Paint.Style.FILL
        scalePaint = Paint()
        scalePaint!!.style = Paint.Style.STROKE
        scalePaint!!.isAntiAlias = true
        scalePaint!!.color = scaleColor
        labelPaint = Paint()
        labelPaint!!.color = scaleColor
        labelPaint!!.typeface = Typeface.SANS_SERIF
        labelPaint!!.textAlign = Paint.Align.CENTER
        upperTextPaint = Paint()
        upperTextPaint!!.color = scaleColor
        upperTextPaint!!.typeface = Typeface.SANS_SERIF
        upperTextPaint!!.textAlign = Paint.Align.CENTER
        lowerTextPaint = Paint()
        lowerTextPaint!!.color = scaleColor
        lowerTextPaint!!.typeface = Typeface.SANS_SERIF
        lowerTextPaint!!.textAlign = Paint.Align.CENTER
        needlePaint = Paint()
        needlePaint!!.color = needleColor
        needlePaint!!.style = Paint.Style.FILL_AND_STROKE
        needlePaint!!.isAntiAlias = true
        needlePath = Path()
        needleScrewPaint = Paint()
        needleScrewPaint!!.color = Color.BLACK
        needleScrewPaint!!.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawRim(canvas)
        drawFace(canvas)
        drawScale(canvas)
        drawLabels(canvas)
        drawTexts(canvas)
        canvas.rotate(
            scaleToCanvasDegrees(valueToDegrees(needleValue)),
            canvasCenterX,
            canvasCenterY
        )
        canvas.drawPath(needlePath!!, needlePaint!!)
        canvas.drawCircle(canvasCenterX, canvasCenterY, canvasWidth / 61f, needleScrewPaint!!)
        if (needsToMove()) {
            moveNeedle()
        }
    }

    private fun moveNeedle() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastMoveTime
        if (deltaTime >= deltaTimeInterval) {
            if (abs(value - needleValue) <= needleStep) {
                needleValue = value
            } else {
                if (value > needleValue) {
                    needleValue += 2 * valuePerDegree()
                } else {
                    needleValue -= 2 * valuePerDegree()
                }
            }
            lastMoveTime = System.currentTimeMillis()
            postInvalidateDelayed(deltaTimeInterval.toLong())
        }
    }

    private fun drawRim(canvas: Canvas) {
        canvas.drawOval(rimRect!!, rimPaint!!)
        canvas.drawOval(rimRect!!, rimCirclePaint!!)
    }

    private fun drawFace(canvas: Canvas) {
        canvas.drawOval(faceRect!!, facePaint!!)
        canvas.drawOval(faceRect!!, rimCirclePaint!!)
        canvas.drawOval(faceRect!!, rimShadowPaint!!)
    }

    private fun drawScale(canvas: Canvas) {
        canvas.save()
        for (i in 0 until totalNicks) {
            val y1 = scaleRect!!.top
            val y2 = y1 + 0.020f * canvasHeight
            val y3 = y1 + 0.060f * canvasHeight
            val y4 = y1 + 0.030f * canvasHeight
            val value = nickToValue(i)
            if (value in minValue..maxValue) {
                canvas.drawLine(0.5f * canvasWidth, y1, 0.5f * canvasWidth, y2, scalePaint!!)
                if (i % majorNickInterval == 0) {
                    canvas.drawLine(0.5f * canvasWidth, y1, 0.5f * canvasWidth, y3, scalePaint!!)
                }
                if (i % (majorNickInterval / 2) == 0) {
                    canvas.drawLine(0.5f * canvasWidth, y1, 0.5f * canvasWidth, y4, scalePaint!!)
                }
            }
            canvas.rotate(degreesPerNick, 0.5f * canvasWidth, 0.5f * canvasHeight)
        }
        canvas.restore()
    }

    private fun drawLabels(canvas: Canvas) {
        var i = 0
        while (i < totalNicks) {
            val value = nickToValue(i)
            if (value in minValue..maxValue) {
                val scaleAngle = i * degreesPerNick
                val scaleAngleRads = Math.toRadians(scaleAngle.toDouble())
                    .toFloat()
                //Log.d(TAG, "i = " + i + ", angle = " + scaleAngle + ", value = " + value);
                val deltaX = labelRadius * sin(scaleAngleRads.toDouble())
                    .toFloat()
                val deltaY = labelRadius * cos(scaleAngleRads.toDouble())
                    .toFloat()
                var valueLabel: String = if (intScale) {
                    value.toInt().toString()
                } else {
                    value.toString()
                }
                drawTextCentered(
                    valueLabel,
                    canvasCenterX + deltaX,
                    canvasCenterY - deltaY,
                    labelPaint,
                    canvas
                )
            }
            i += majorNickInterval
        }
    }

    private fun drawTexts(canvas: Canvas) {
        drawTextCentered(
            upperText,
            canvasCenterX,
            canvasCenterY - canvasHeight / 6.5f,
            upperTextPaint,
            canvas
        )
        drawTextCentered(
            lowerText,
            canvasCenterX,
            canvasCenterY + canvasHeight / 6.5f,
            lowerTextPaint,
            canvas
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        canvasWidth = w.toFloat()
        canvasHeight = h.toFloat()
        canvasCenterX = w / 2f
        canvasCenterY = h / 2f
        needleTailLength = canvasWidth / 12f
        needleWidth = canvasWidth / 98f
        needleLength = canvasWidth / 2f * 0.8f
        needlePaint!!.strokeWidth = canvasWidth / 197f
        if (needleShadow) needlePaint!!.setShadowLayer(
            canvasWidth / 123f,
            canvasWidth / 10000f,
            canvasWidth / 10000f,
            Color.GRAY
        )
        setNeedle()
        rimRect = RectF(
            canvasWidth * .05f,
            canvasHeight * .05f,
            canvasWidth * 0.95f,
            canvasHeight * 0.95f
        )
        rimPaint!!.shader = LinearGradient(
            canvasWidth * 0.40f, canvasHeight * 0.0f, canvasWidth * 0.60f, canvasHeight * 1.0f,
            Color.rgb(0xf0, 0xf5, 0xf0),
            Color.rgb(0x30, 0x31, 0x30),
            Shader.TileMode.CLAMP
        )
        val rimSize = 0.02f * canvasWidth
        faceRect = RectF()
        faceRect!![rimRect!!.left + rimSize, rimRect!!.top + rimSize, rimRect!!.right - rimSize] =
            rimRect!!.bottom - rimSize
        rimShadowPaint!!.shader = RadialGradient(
            0.5f * canvasWidth,
            0.5f * canvasHeight,
            faceRect!!.width() / 2.0f,
            intArrayOf(0x00000000, 0x00000500, 0x50000500),
            floatArrayOf(0.96f, 0.96f, 0.99f),
            Shader.TileMode.MIRROR
        )
        scalePaint!!.strokeWidth = 0.005f * canvasWidth
        scalePaint!!.textSize = 0.045f * canvasWidth
        scalePaint!!.textScaleX = 0.8f * canvasWidth
        val scalePosition = 0.015f * canvasWidth
        scaleRect = RectF()
        scaleRect!![faceRect!!.left + scalePosition, faceRect!!.top + scalePosition, faceRect!!.right - scalePosition] =
            faceRect!!.bottom - scalePosition
        labelRadius = (canvasCenterX - scaleRect!!.left) * 0.70f

        /*
        Log.d(TAG, "width = " + w);
        Log.d(TAG, "height = " + h);
        Log.d(TAG, "width pixels = " + getResources().getDisplayMetrics().widthPixels);
        Log.d(TAG, "height pixels = " + getResources().getDisplayMetrics().heightPixels);
        Log.d(TAG, "density = " + getResources().getDisplayMetrics().density);
        Log.d(TAG, "density dpi = " + getResources().getDisplayMetrics().densityDpi);
        Log.d(TAG, "scaled density = " + getResources().getDisplayMetrics().scaledDensity);
        */
        var textSize: Float = if (requestedLabelTextSize > 0) {
            requestedLabelTextSize * textScaleFactor
        } else {
            canvasWidth / 16f
        }
        Log.d(TAG, "Label text size = $textSize")
        labelPaint!!.textSize = textSize
        textSize = if (requestedTextSize > 0) {
            requestedTextSize * textScaleFactor
        } else {
            canvasWidth / 14f
        }
        Log.d(TAG, "Default upper/lower text size = $textSize")
        upperTextPaint!!.textSize =
            if (requestedUpperTextSize > 0) requestedUpperTextSize * textScaleFactor else textSize
        lowerTextPaint!!.textSize =
            if (requestedLowerTextSize > 0) requestedLowerTextSize * textScaleFactor else textSize
        super.onSizeChanged(w, h, oldw, oldh)
    }

    private fun setNeedle() {
        needlePath!!.reset()
        needlePath!!.moveTo(canvasCenterX - needleTailLength, canvasCenterY)
        needlePath!!.lineTo(canvasCenterX, canvasCenterY - needleWidth / 2)
        needlePath!!.lineTo(canvasCenterX + needleLength, canvasCenterY)
        needlePath!!.lineTo(canvasCenterX, canvasCenterY + needleWidth / 2)
        needlePath!!.lineTo(canvasCenterX - needleTailLength, canvasCenterY)
        needlePath!!.addCircle(canvasCenterX, canvasCenterY, canvasWidth / 49f, Path.Direction.CW)
        needlePath!!.close()
        needleScrewPaint!!.shader = RadialGradient(
            canvasCenterX, canvasCenterY, needleWidth / 2,
            Color.DKGRAY, Color.BLACK, Shader.TileMode.CLAMP
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size: Int
        val width = measuredWidth
        val height = measuredHeight
        val widthWithoutPadding = width - paddingLeft - paddingRight
        val heightWithoutPadding = height - paddingTop - paddingBottom
        size = if (widthWithoutPadding > heightWithoutPadding) {
            heightWithoutPadding
        } else {
            widthWithoutPadding
        }
        setMeasuredDimension(size + paddingLeft + paddingRight, size + paddingTop + paddingBottom)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        bundle.putFloat("value", value)
        bundle.putFloat("needleValue", needleValue)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            val bundle = state
            value = bundle.getFloat("value")
            needleValue = bundle.getFloat("needleValue")
            super.onRestoreInstanceState(bundle.getParcelable("superState"))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private fun nickToValue(nick: Int): Float {
        val rawValue = (if (nick < totalNicks / 2) nick else nick - totalNicks) * valuePerNick
        return rawValue + centerValue
    }

    private fun valueToDegrees(value: Float): Float {
        // these are scale degrees, 0 is on top
        return (value - centerValue) / valuePerNick * degreesPerNick
    }

    private fun valuePerDegree(): Float {
        return valuePerNick / degreesPerNick
    }

    private fun scaleToCanvasDegrees(degrees: Float): Float {
        return degrees - 90
    }

    private fun needsToMove(): Boolean {
        return abs(needleValue - value) > 0
    }

    private fun drawTextCentered(text: String, x: Float, y: Float, paint: Paint?, canvas: Canvas) {
        //float xPos = x - (paint.measureText(text)/2f);
        val yPos = y - (paint!!.descent() + paint.ascent()) / 2f
        canvas.drawText(text, x, yPos, paint)
    }

    /**
     * Set gauge to value.
     *
     * @param value Value
     */
    fun setValue(value: Float) {
        if (value < minValue) this.value = minValue
        else if (value > maxValue) this.value = maxValue
        else  this.value = value
        needleValue = this.value
        postInvalidate()
    }

    /**
     * Animate gauge to value.
     *
     * @param value Value
     */
    fun moveToValue(value: Float) {
        this.value = value
        postInvalidate()
    }

    /**
     * Set string to display on upper gauge face.
     *
     * @param text Text
     */
    fun setUpperText(text: String) {
        upperText = text
        invalidate()
    }

    /**
     * Set string to display on lower gauge face.
     *
     * @param text Text
     */
    fun setLowerText(text: String) {
        lowerText = text
        invalidate()
    }

    /**
     * Request a text size.
     *
     * @param size Size (pixels)
     * @see Paint.setTextSize
     */
    @Deprecated("")
    fun setRequestedTextSize(size: Float) {
        setTextSize(size)
    }

    /**
     * Set a text size for the upper and lower text.
     *
     * Size is in pixels at a screen width (max. canvas width/height) of 1080 and is scaled
     * accordingly at different resolutions. E.g. a value of 48 is unchanged at 1080 x 1920
     * and scaled down to 27 at 600 x 1024.
     *
     * @param size Size (relative pixels)
     * @see Paint.setTextSize
     */
    fun setTextSize(size: Float) {
        requestedTextSize = size
    }

    /**
     * Set or override the text size for the upper text.
     *
     * Size is in pixels at a screen width (max. canvas width/height) of 1080 and is scaled
     * accordingly at different resolutions. E.g. a value of 48 is unchanged at 1080 x 1920
     * and scaled down to 27 at 600 x 1024.
     *
     * @param size (relative pixels)
     * @see Paint.setTextSize
     */
    fun setUpperTextSize(size: Float) {
        requestedUpperTextSize = size
    }

    /**
     * Set or override the text size for the lower text
     *
     * Size is in pixels at a screen width (max. canvas width/height) of 1080 and is scaled
     * accordingly at different resolutions. E.g. a value of 48 is unchanged at 1080 x 1920
     * and scaled down to 27 at 600 x 1024.
     *
     * @param size (relative pixels)
     * @see Paint.setTextSize
     */
    fun setLowerTextSize(size: Float) {
        requestedLowerTextSize = size
    }

    /**
     * Set the delta time between movement steps during needle animation (default: 5 ms).
     *
     * @param interval Time (ms)
     */
    fun setDeltaTimeInterval(interval: Int) {
        deltaTimeInterval = interval
    }

    /**
     * Set the factor that determines the step size during needle animation (default: 3f).
     * The actual step size is calculated as follows: step_size = step_factor * scale_value_per_degree.
     *
     * @param factor Step factor
     */
    fun setNeedleStepFactor(factor: Float) {
        needleStepFactor = factor
    }

    /**
     * Set the minimum scale value.
     *
     * @param value minimum value
     */
    fun setMinValue(value: Float) {
        minValue = value
        initValues()
        validate()
        invalidate()
    }

    /**
     * Set the maximum scale value.
     *
     * @param value maximum value
     */
    fun setMaxValue(value: Float) {
        maxValue = value
        initValues()
        validate()
        invalidate()
    }

    /**
     * Set the total amount of nicks on a full 360 degree scale. Should be a multiple of majorNickInterval.
     *
     * @param nicks number of nicks
     */
    fun setTotalNicks(nicks: Int) {
        totalNicks = nicks
        degreesPerNick = 360.0f / totalNicks
        initValues()
        validate()
        invalidate()
    }

    /**
     * Set the value (interval) per nick.
     *
     * @param value value per nick
     */
    fun setValuePerNick(value: Float) {
        valuePerNick = value
        initValues()
        validate()
        invalidate()
    }

    /**
     * Set the interval (number of nicks) between enlarged nicks.
     *
     * @param interval major nick interval
     */
    fun setMajorNickInterval(interval: Int) {
        majorNickInterval = interval
        validate()
        invalidate()
    }

    private fun validate() {
        var valid = true
        if (totalNicks % majorNickInterval != 0) {
            valid = false
            Log.w(
                TAG,
                resources.getString(R.string.invalid_number_of_nicks, totalNicks, majorNickInterval)
            )
        }
        val sum = minValue + maxValue
        val intSum = sum.roundToInt()
        if (maxValue >= 1 && (sum != intSum.toFloat() || intSum and 1 != 0) || minValue >= maxValue) {
            valid = false
            Log.w(TAG, resources.getString(R.string.invalid_min_max_ratio, minValue, maxValue))
        }
        if ((sum % valuePerNick).roundToInt() != 0) {
            valid = false
            Log.w(
                TAG,
                resources.getString(R.string.invalid_min_max, minValue, maxValue, valuePerNick)
            )
        }
        if (valid) Log.i(TAG, resources.getString(R.string.scale_ok))
    }

    companion object {
        private val TAG = Gauge::class.java.simpleName
        private const val REF_MAX_PORTRAIT_CANVAS_SIZE =
            1080 // reference size, scale text accordingly

        private fun fromHtml(html: String?): Spanned {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(html)
            }
        }
    }
}