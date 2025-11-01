package com.banktracker.vn.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.banktracker.vn.R

class TodayBalanceWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_today_balance)
            views.setTextViewText(R.id.tvBalance, "0 VND")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
