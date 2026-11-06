package net.owh.yenrate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import net.owh.yenrate.MainActivity
import net.owh.yenrate.R
import net.owh.yenrate.data.YenRatePreferences
import net.owh.yenrate.worker.YenRateUpdateWorker
import java.util.Locale

class YenRateCompactWidgetProvider : AppWidgetProvider() {

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

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_yen_rate_compact)
            val data = YenRatePreferences.getRateData(context)

            if (data.currentRate <= 0.0) {
                views.setTextViewText(R.id.text_compact_exchange_rate, "¥ ---.--")
                views.setImageViewResource(R.id.image_compact_arrow, R.drawable.ic_arrow_neutral)
            } else {
                views.setTextViewText(
                    R.id.text_compact_exchange_rate,
                    String.format(Locale.US, "¥ %.2f", data.currentRate)
                )

                when {
                    data.isPositive -> {
                        views.setImageViewResource(R.id.image_compact_arrow, R.drawable.ic_arrow_up)
                    }
                    data.isNegative -> {
                        views.setImageViewResource(R.id.image_compact_arrow, R.drawable.ic_arrow_down)
                    }
                    else -> {
                        views.setImageViewResource(R.id.image_compact_arrow, R.drawable.ic_arrow_neutral)
                    }
                }
            }

            // PendingIntent to launch MainActivity when clicking anywhere on the compact widget
            val mainIntent = Intent(context, MainActivity::class.java)
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_compact_container, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, YenRateCompactWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
