package net.owh.yenrate.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import net.owh.yenrate.R
import net.owh.yenrate.worker.YenRateUpdateWorker

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
            WidgetUpdater.updateWidget(context, appWidgetManager, appWidgetId, R.layout.widget_yen_rate)
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
    }
}
