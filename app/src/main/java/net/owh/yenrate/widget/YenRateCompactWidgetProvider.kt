package net.owh.yenrate.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import net.owh.yenrate.R
import net.owh.yenrate.worker.YenRateUpdateWorker

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
            WidgetUpdater.updateWidget(context, appWidgetManager, appWidgetId, R.layout.widget_yen_rate_compact)
        }
    }
}
