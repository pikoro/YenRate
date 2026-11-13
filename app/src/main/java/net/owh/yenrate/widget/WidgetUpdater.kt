package net.owh.yenrate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import net.owh.yenrate.MainActivity
import net.owh.yenrate.R
import net.owh.yenrate.data.RateData
import net.owh.yenrate.data.YenRatePreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetUpdater {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Update the main widget
        val mainProvider = YenRateWidgetProvider::class.java
        val mainIds = appWidgetManager.getAppWidgetIds(ComponentName(context, mainProvider))
        for (id in mainIds) {
            updateWidget(context, appWidgetManager, id, R.layout.widget_yen_rate)
        }

        // Update the compact widget
        val compactProvider = YenRateCompactWidgetProvider::class.java
        val compactIds = appWidgetManager.getAppWidgetIds(ComponentName(context, compactProvider))
        for (id in compactIds) {
            updateWidget(context, appWidgetManager, id, R.layout.widget_yen_rate_compact)
        }
    }

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, layoutResId: Int) {
        val views = RemoteViews(context.packageName, layoutResId)
        val data = YenRatePreferences.getRateData(context)

        if (data.currentRate <= 0.0) {
            updateEmptyState(views, context, layoutResId)
        } else {
            updateDataState(views, context, layoutResId, data)
        }

        // PendingIntent to launch MainActivity
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine container ID based on layout
        val containerId = if (layoutResId == R.layout.widget_yen_rate) {
            R.id.widget_container
        } else {
            R.id.widget_compact_container
        }
        views.setOnClickPendingIntent(containerId, mainPendingIntent)

        // PendingIntent for refresh button on main widget
        if (layoutResId == R.layout.widget_yen_rate) {
            val refreshIntent = Intent(context, YenRateWidgetProvider::class.java).apply {
                action = YenRateWidgetProvider.ACTION_MANUAL_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateEmptyState(views: RemoteViews, context: Context, layoutResId: Int) {
        val rateId = if (layoutResId == R.layout.widget_yen_rate) {
            R.id.text_exchange_rate
        } else {
            R.id.text_compact_exchange_rate
        }

        val realRateId = if (layoutResId == R.layout.widget_yen_rate) {
            R.id.text_real_exchange_rate
        } else {
            R.id.text_compact_real_exchange_rate
        }

        val arrowId = if (layoutResId == R.layout.widget_yen_rate) {
            R.id.image_arrow
        } else {
            R.id.image_compact_arrow
        }

        views.setTextViewText(rateId, "¥ ---.--")
        views.setTextViewText(realRateId, "")
        views.setImageViewResource(arrowId, R.drawable.ic_arrow_neutral)

        if (layoutResId == R.layout.widget_yen_rate) {
            views.setTextViewText(R.id.text_rate_change, context.getString(R.string.updating))
            views.setTextColor(R.id.text_rate_change, "#94A3B8".toColorInt())
            views.setTextViewText(R.id.text_last_updated, context.getString(R.string.updating_rate))
        }
    }

    private fun updateDataState(views: RemoteViews, context: Context, layoutResId: Int, data: RateData) {
        val rateId = if (layoutResId == R.layout.widget_yen_rate) {
            R.id.text_exchange_rate
        } else {
            R.id.text_compact_exchange_rate
        }

        val realRateId = if (layoutResId == R.layout.widget_yen_rate) {
            R.id.text_real_exchange_rate
        } else {
            R.id.text_compact_real_exchange_rate
        }

        val arrowId = if (layoutResId == R.layout.widget_yen_rate) {
            R.id.image_arrow
        } else {
            R.id.image_compact_arrow
        }

        views.setTextViewText(
            rateId,
            String.format(Locale.US, "¥ %.2f", data.currentRate)
        )

        val formattedRealRate = String.format(Locale.US, "¥ %.2f", data.realRate)
        views.setTextViewText(realRateId, formattedRealRate)
        views.setTextColor(realRateId, "#FFD600".toColorInt())

        val arrowRes = when {
            data.isPositive -> R.drawable.ic_arrow_up
            data.isNegative -> R.drawable.ic_arrow_down
            else -> R.drawable.ic_arrow_neutral
        }
        views.setImageViewResource(arrowId, arrowRes)

        if (layoutResId == R.layout.widget_yen_rate) {
            val changeStr = String.format(
                Locale.US,
                "%+.2f (%+.2f%%)",
                data.change,
                data.changePercentage
            )
            views.setTextViewText(R.id.text_rate_change, changeStr)

            val colorHex = when {
                data.isPositive -> "#4CAF50"
                data.isNegative -> "#EF5350"
                else -> "#94A3B8"
            }
            views.setTextColor(R.id.text_rate_change, colorHex.toColorInt())

            if (data.lastUpdatedTime > 0) {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val formattedTime = timeFormat.format(Date(data.lastUpdatedTime))
                views.setTextViewText(R.id.text_last_updated, context.getString(R.string.updated_at, formattedTime))
            } else {
                views.setTextViewText(R.id.text_last_updated, context.getString(R.string.updated_recently))
            }

            // Render 24h Graph Bitmap with X/Y Scales
            val graphBitmap = createGraphBitmap(context, data)
            views.setImageViewBitmap(R.id.image_graph, graphBitmap)
        }
    }

    private fun createGraphBitmap(
        context: Context,
        data: RateData,
        widthPx: Int = 400,
        heightPx: Int = 140
    ): Bitmap {
        val bitmap = createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val history = data.history

        if (history.size < 2) return bitmap

        val lineColor = "#3B82F6".toColorInt() // Always blue line
        val fillColorTop = "#333B82F6".toColorInt() // Always blue gradient fill

        val minRate = data.minRate
        val maxRate = data.maxRate
        val rateRange = (maxRate - minRate).let { if (it <= 0.001) 1.0 else it }

        val leftPadding = 85f
        val rightPadding = 12f
        val topPadding = 14f
        val bottomPadding = 28f

        val chartWidth = widthPx - leftPadding - rightPadding
        val chartHeight = heightPx - topPadding - bottomPadding

        // Draw horizontal gridlines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#334155".toColorInt()
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val topY = topPadding
        val midY = topPadding + chartHeight / 2f
        val bottomY = topPadding + chartHeight

        canvas.drawLine(leftPadding, topY, widthPx - rightPadding, topY, gridPaint)
        canvas.drawLine(leftPadding, midY, widthPx - rightPadding, midY, gridPaint)
        canvas.drawLine(leftPadding, bottomY, widthPx - rightPadding, bottomY, gridPaint)

        // Draw Light Dotted Lines for High and Low Rates
        val historyMax = history.maxOf { it.rate }
        val historyMin = history.minOf { it.rate }

        val highY = topPadding + chartHeight - (((historyMax - minRate) / rateRange).toFloat() * chartHeight)
        val lowY = topPadding + chartHeight - (((historyMin - minRate) / rateRange).toFloat() * chartHeight)

        val dottedPaintHigh = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#AA4CAF50".toColorInt()
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }
        val dottedPaintLow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#AAEF5350".toColorInt()
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }

        canvas.drawLine(leftPadding, highY, widthPx - rightPadding, highY, dottedPaintHigh)
        canvas.drawLine(leftPadding, lowY, widthPx - rightPadding, lowY, dottedPaintLow)

        // Draw Y-Axis Rate Scale Labels (High & Low)
        val yTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 19f
            isFakeBoldText = true
        }

        yTextPaint.color = "#4CAF50".toColorInt()
        canvas.drawText(String.format(Locale.US, "¥%.2f", maxRate), 6f, topY + 14f, yTextPaint)

        yTextPaint.color = "#EF5350".toColorInt()
        canvas.drawText(String.format(Locale.US, "¥%.2f", minRate), 6f, bottomY, yTextPaint)

        // Main Market Rate Points (Straight Line Segments)
        val mainPoints = history.mapIndexed { index, point ->
            val x = leftPadding + (index.toFloat() / (history.size - 1)) * chartWidth
            val normalizedRate = (point.rate - minRate) / rateRange
            val y = topPadding + chartHeight - (normalizedRate.toFloat() * chartHeight)
            Pair(x, y)
        }

        val strokePath = Path()
        val fillPath = Path()

        strokePath.moveTo(mainPoints[0].first, mainPoints[0].second)
        fillPath.moveTo(mainPoints[0].first, bottomY)
        fillPath.lineTo(mainPoints[0].first, mainPoints[0].second)

        for (i in 1 until mainPoints.size) {
            strokePath.lineTo(mainPoints[i].first, mainPoints[i].second)
            fillPath.lineTo(mainPoints[i].first, mainPoints[i].second)
        }

        fillPath.lineTo(mainPoints.last().first, bottomY)
        fillPath.close()

        // Fill Paint
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, topPadding, 0f, bottomY,
                fillColorTop, "#00000000".toColorInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(fillPath, fillPaint)

        // Line Paint (Main Market Rate - Blue Line)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            color = lineColor
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(strokePath, linePaint)

        // Draw X-Axis Time Scale Labels
        val xTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#94A3B8".toColorInt()
            textSize = 18f
        }

        val leftTimeStr = context.getString(R.string.time_24h_ago)
        val rightTimeStr = context.getString(R.string.time_now)

        canvas.drawText(leftTimeStr, leftPadding, heightPx - 4f, xTextPaint)

        val rightTextWidth = xTextPaint.measureText(rightTimeStr)
        canvas.drawText(rightTimeStr, widthPx - rightPadding - rightTextWidth, heightPx - 4f, xTextPaint)

        return bitmap
    }
}
