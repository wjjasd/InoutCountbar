package com.utarex.inoutbar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.max
import kotlin.math.roundToInt

class InOutCountBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var inValue = 0; set(v) { field = v; updateRatios() }
    var outValue = 0; set(v) { field = v; updateRatios() }
    var countValue = 0; set(v) { field = v; updateRatios() }

    var lastEntry: String = ""; set(v) { field = v; invalidate() }
    var lastExit: String = ""; set(v) { field = v; invalidate() }

    // dp 값을 픽셀로 변환
    var cornerRadius = 12f * resources.displayMetrics.density

    // 텍스트 패딩 추가 (dp)
    private val textPaddingDp = 8f
    private val textPaddingPx = textPaddingDp * resources.displayMetrics.density

    private val paintIn = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintOut = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintCount = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    // animation ratios
    private var animIn = 0f
    private var animOut = 0f
    private var animCount = 0f

    // targets
    private var targetIn = 0f
    private var targetOut = 0f
    private var targetCount = 0f

    // start values for lerp
    private var startIn = 0f
    private var startOut = 0f
    private var startCount = 0f

    private var animator: ValueAnimator? = null

    init {
        minimumHeight = 100

        attrs?.let {
            // R.styleable.InOutCountBar는 프로젝트의 attrs.xml에 정의되어 있다고 가정합니다.
            val a = context.obtainStyledAttributes(it, R.styleable.InOutCountBar)

            paintIn.color = a.getColor(R.styleable.InOutCountBar_barInColor, 0xFF4CAF50.toInt())
            paintOut.color = a.getColor(R.styleable.InOutCountBar_barOutColor, 0xFFF44336.toInt())
            paintCount.color = a.getColor(R.styleable.InOutCountBar_barCountColor, 0xFF2196F3.toInt())
            barOutlinePaint.color = a.getColor(R.styleable.InOutCountBar_barOutlineColor, Color.TRANSPARENT)

            textPaint.color = a.getColor(R.styleable.InOutCountBar_entryTextColor, Color.DKGRAY)
            textPaint.textSize = a.getDimension(R.styleable.InOutCountBar_entryTextSize, 36f)

            val fontId = a.getResourceId(R.styleable.InOutCountBar_entryTextFont, 0)
            if (fontId != 0) textPaint.typeface = ResourcesCompat.getFont(context, fontId)

            a.recycle()
        }

        // initialize ratios so first draw is consistent
        animIn = 0f
        animOut = 0f
        animCount = 0f
    }

    private fun updateRatios() {
        val total = max(1, inValue + outValue + countValue)
        val nextIn = inValue.toFloat() / total
        val nextOut = outValue.toFloat() / total
        val nextCount = countValue.toFloat() / total

        // save start values for smooth lerp (prevents cumulative error)
        startIn = animIn
        startOut = animOut
        startCount = animCount

        targetIn = nextIn
        targetOut = nextOut
        targetCount = nextCount

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                animIn = lerp(startIn, targetIn, t)
                animOut = lerp(startOut, targetOut, t)
                animCount = lerp(startCount, targetCount, t)

                // normalize to ensure sum == 1 (protect against tiny FP drift)
                val sum = animIn + animOut + animCount
                if (sum > 0f) {
                    animIn /= sum
                    animOut /= sum
                    animCount /= sum
                }

                invalidate()
            }
            start()
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    override fun onDraw(canvas: Canvas) {
        // 막대 높이를 뷰 높이의 45%로 설정
        val barHeight = height * 0.45f
        // 텍스트가 시작될 위치 (막대 아래 + 약간의 간격)
        val textTop = barHeight + 10f

        drawBar(canvas, barHeight)
        drawText(canvas, textTop)
    }

    /**
     * 막대 세그먼트를 그리는 함수.
     * 그려지는 세그먼트 중 가장 왼쪽과 오른쪽에만 둥근 모서리를 적용하도록 수정되었습니다.
     */
    private fun drawBar(canvas: Canvas, barHeight: Float) {
        val w = width.toFloat()
        val r = cornerRadius

        // 0이 아닌 세그먼트 목록을 비율 순서대로 준비합니다.
        // Triple: (Paint, 비율, 이름)
        val segmentsRaw = mutableListOf<Triple<Paint, Float, String>>()
        if (animIn > 0f) segmentsRaw.add(Triple(paintIn, animIn, "IN"))
        if (animOut > 0f) segmentsRaw.add(Triple(paintOut, animOut, "OUT"))
        if (animCount > 0f) segmentsRaw.add(Triple(paintCount, animCount, "COUNT"))

        if (segmentsRaw.isEmpty()) return

        var startX = 0f
        val totalWidth = w.toInt() // 전체 너비 (정수 픽셀)

        // 실제로 그려질 세그먼트 목록을 저장합니다. (Paint, Width)
        val segmentsToDraw = mutableListOf<Pair<Paint, Float>>()

        // 1단계: 첫 번째 (N-1)개의 세그먼트 너비를 계산하고 정수로 반올림
        for (i in 0 until segmentsRaw.size - 1) {
            val segment = segmentsRaw[i]
            // 비율 * 전체 너비를 정수 픽셀로 반올림하여 현재 세그먼트의 너비를 결정합니다.
            val currentWidth = (segment.second * w).roundToInt().toFloat()

            // 누적 시작점(startX)에서 현재 세그먼트의 끝점(startX + currentWidth)이
            // totalWidth를 넘어서지 않도록 제한합니다. (안전 장치)
            val limitedWidth = if (startX + currentWidth > totalWidth) {
                totalWidth - startX
            } else {
                currentWidth
            }

            segmentsToDraw.add(Pair(segment.first, limitedWidth))
            startX += limitedWidth // 다음 세그먼트의 시작점으로 업데이트
        }

        // 2단계: 마지막 세그먼트는 남은 모든 너비를 차지합니다.
        val lastSegment = segmentsRaw.last()
        val lastWidth = totalWidth - startX // 전체 너비에서 이전 세그먼트들의 누적 너비를 뺌

        // 마지막 세그먼트의 너비가 0보다 클 경우에만 추가 (안전 장치)
        if (lastWidth > 0) {
            segmentsToDraw.add(Pair(lastSegment.first, lastWidth.toFloat()))
        }

        // 3단계: Path를 이용해 세그먼트 그리기 (이전 로직 유지 + 수정된 둥근 모서리)
        startX = 0f
        segmentsToDraw.forEachIndexed { index, segment ->
            val paint = segment.first
            val segW = segment.second

            val isFirst = index == 0
            val isLast = index == segmentsToDraw.size - 1

            // 둥근 모서리 배열: [TL_X, TL_Y, TR_X, TR_Y, BR_X, BR_Y, BL_X, BL_Y]
            val radii = FloatArray(8) { 0f }
            if (isFirst) {
                radii[0] = r; radii[1] = r // Top Left
                radii[6] = r; radii[7] = r // Bottom Left
            }
            if (isLast) {
                radii[2] = r; radii[3] = r // Top Right
                radii[4] = r; radii[5] = r // Bottom Right
            }

            // Path로 그리기
            val rectF = RectF(startX, 0f, startX + segW, barHeight)
            val path = Path().apply {
                addRoundRect(rectF, radii, Path.Direction.CW)
            }
            canvas.drawPath(path, paint)
            startX += segW
        }
    }

    /**
     * 텍스트를 그리는 함수.
     * Entry(왼쪽), Exit(오른쪽)에 각각 패딩을 적용하고 정렬하도록 수정되었습니다.
     */
    private fun drawText(canvas: Canvas, top: Float) {
        val w = width.toFloat()
        val halfWidth = width / 2
        val pad = textPaddingPx

        // 왼쪽 영역의 최대 너비 (중앙선까지 - 패딩)
        val leftMaxWidth = halfWidth - pad.roundToInt()
        // 오른쪽 영역의 최대 너비 (중앙선부터 - 패딩)
        val rightMaxWidth = halfWidth - pad.roundToInt()

        // Entry (왼쪽, start 정렬)
        val entryLayout = buildLayout(lastEntry, leftMaxWidth, Layout.Alignment.ALIGN_NORMAL)
        canvas.save()
        // X = 패딩, Y = top
        canvas.translate(pad, top)
        entryLayout.draw(canvas)
        canvas.restore()

        // Exit (오른쪽, end 정렬)
        val exitLayout = buildLayout(lastExit, rightMaxWidth, Layout.Alignment.ALIGN_OPPOSITE)
        canvas.save()
        // X 좌표: 전체 너비 - 레이아웃 너비 - 패딩 (오른쪽 정렬)
        val exitX = w - exitLayout.width - pad
        canvas.translate(exitX, top)
        exitLayout.draw(canvas)
        canvas.restore()
    }

    /**
     * StaticLayout을 생성하는 함수.
     * API 23 미만에서 텍스트 줄임 처리를 간소화했습니다.
     */
    private fun buildLayout(text: String, width: Int, alignment: Layout.Alignment): StaticLayout {
        // width는 Int여야 합니다.
        if (width <= 0) return StaticLayout("", textPaint, 1, alignment, 1f, 0f, false)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1f)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        } else {
            // API 23 미만은 StaticLayout 생성자로 처리.
            // 2줄 제한 및 Ellipsize 처리는 구형 API에서 수동으로 해야 함. 여기서는 간소화된 버전 사용.
            @Suppress("DEPRECATION")
            StaticLayout(text, textPaint, width, alignment, 1f, 0f, false)
        }
    }
}