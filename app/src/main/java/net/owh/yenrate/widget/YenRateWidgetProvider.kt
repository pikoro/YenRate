package net.owh.yenrate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import net.owh.yenrate.MainActivity
import net.owh.yenrate.R
import net.owh.yenrate.data.YenRatePreferences
import net.owh.yenrate.worker.YenRateUpdateWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class YenRateWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        YenRateUpdateWorker.schedulePeriodicWork(context)
        YenRateUpdateWorker.enqueueImmediateWork(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MANUAL_REFRESH) {
            YenRateUpdateWorker.enqueueImmediateWork(context)
        }
    }

    companion object {
        const val ACTION_MANUAL_REFRESH = "net.owh.yenrate.ACTION_MANUAL_REFRESH"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_yen_rate)
            val data = YenRatePreferences.getRateData(context)

            if (data.currentRate <= 0.0) {
                views.setTextViewText(R.id.text_exchange_rate, "¥ ---.--")
                views.setTextViewText(R.id.text_rate_change, context.getString(R.string.updating))
                views.setImageViewResource(R.id.image_arrow, R.drawable.ic_arrow_neutral)
                views.setTextColor(R.id.text_rate_change, Color.parseColor("#94A3B8"))
                views.setTextViewText(R.id.text_last_updated, context.getString(R.string.updating_rate))
            } else {
                views.setTextViewText(
                    R.id.text_exchange_rate,
                    String.format(Locale.US, "¥ %.2f", data.currentRate)
                )

                val changeStr = String.format(
                    Locale.US,
                    "%+.2f (%+.2f%%)",
                    data.change,
                    data.changePercentage
                )
                views.setTextViewText(R.id.text_rate_change, changeStr)

                when {
                    data.isPositive -> {
                        views.setImageViewResource(R.id.image_arrow, R.drawable.ic_arrow_up)
                        views.setTextColor(R.id.text_rate_change, Color.parseColor("#4CAF50"))
                    }
                    data.isNegative -> {
                        views.setImageViewResource(R.id.image_arrow, R.drawable.ic_arrow_down)
                        views.setTextColor(R.id.text_rate_change, Color.parseColor("#EF5350"))
                    }
                    else -> {
                        views.setImageViewResource(R.id.image_arrow, R.drawable.ic_arrow_neutral)
                        views.setTextColor(R.id.text_rate_change, Color.parseColor("#94A3B8"))
                    }
                }

                if (data.lastUpdatedTime > 0) {
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val formattedTime = timeFormat.format(Date(data.lastUpdatedTime))
                    views.setTextViewText(R.id.text_last_updated, context.getString(R.string.updated_at, formattedTime))
                } else {
                    views.setTextViewText(R.id.text_last_updated, context.getString(R.string.updated_recently))
                }
            }

            // PendingIntent to launch MainActivity when clicking the widget body
            val mainIntent = Intent(context, MainActivity::class.java)
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent)

            // PendingIntent for refresh button
            val refreshIntent = Intent(context, YenRateWidgetProvider::class.java).apply {
                action = ACTION_MANUAL_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, YenRateWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
