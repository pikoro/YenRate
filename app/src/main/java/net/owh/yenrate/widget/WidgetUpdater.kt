package net.owh.yenrate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
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

        val arrowId = if (layoutResId == R.layout.widget_yen_rate) {
            R.id.image_arrow
        } else {
            R.id.image_compact_arrow
        }

        views.setTextViewText(rateId, "¥ ---.--")
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

        val arrowId = if (layoutResId == R.layout.widget_yen_rate) {
            R.id.image_arrow
        } else {
            R.id.image_compact_arrow
        }

        views.setTextViewText(
            rateId,
            String.format(Locale.US, "¥ %.2f", data.currentRate)
        )

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
        }
    }
}
