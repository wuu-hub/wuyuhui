package com.example.wordapp.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.wordapp.data.Stats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 用 Canvas 手绘学习报告，生成可分享的 Bitmap 图片。
 * 不依赖任何第三方图表库。
 */
object BitmapReport {

    private const val W = 1080
    private const val H = 1440

    fun generate(stats: Stats): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 背景
        canvas.drawColor(Color.parseColor("#F2F4FA"))

        // 顶部渐变头
        val header = RectF(0f, 0f, W.toFloat(), 520f)
        val shader = LinearGradient(
            0f, 0f, W.toFloat(), 520f,
            Color.parseColor("#3F6BE0"), Color.parseColor("#7A5CE0"),
            Shader.TileMode.CLAMP
        )
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.drawRect(header, headerPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }

        // 标题
        textPaint.textSize = 64f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("学习报告", 80f, 180f, textPaint)

        // 日期
        textPaint.textSize = 34f
        textPaint.typeface = Typeface.DEFAULT
        val date = SimpleDateFormat("yyyy年MM月dd日 EEE", Locale.CHINA).format(Date())
        canvas.drawText(date, 80f, 250f, textPaint)

        // 正确率大数字
        textPaint.textSize = 120f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("${stats.correctRate}%", 80f, 430f, textPaint)
        textPaint.textSize = 32f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("累计答题正确率", 80f, 480f, textPaint)

        // 白色统计卡片
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        val card = RectF(60f, 600f, W - 60f, 1200f)
        canvas.drawRoundRect(card, 36f, 36f, cardPaint)

        val items = listOf(
            "总词数" to stats.total.toString(),
            "已学习" to stats.learned.toString(),
            "已掌握" to stats.mastered.toString(),
            "待复习" to stats.due.toString()
        )

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7488")
            textSize = 36f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C2333")
            textSize = 84f
            typeface = Typeface.DEFAULT_BOLD
        }

        val colX = floatArrayOf(150f, 590f)
        val rowY = floatArrayOf(760f, 1030f)
        for ((index, pair) in items.withIndex()) {
            val x = colX[index % 2]
            val y = rowY[index / 2]
            canvas.drawText(pair.first, x, y, labelPaint)
            canvas.drawText(pair.second, x, y + 110f, valuePaint)
        }

        // 分割线
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E6E9F2")
            strokeWidth = 3f
        }
        canvas.drawLine(120f, 900f, W - 120f, 900f, linePaint)
        canvas.drawLine(W / 2f, 660f, W / 2f, 1140f, linePaint)

        // 底部鼓励语
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3F6BE0")
            textSize = 38f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("坚持间隔重复，记忆更牢固 —— 间隔背单词", W / 2f, 1320f, footerPaint)

        return bitmap
    }
}
