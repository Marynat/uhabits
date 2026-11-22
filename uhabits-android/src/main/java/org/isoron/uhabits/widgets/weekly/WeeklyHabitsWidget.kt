/*
 * Copyright (C) 2026 Marynat
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.widgets.weekly

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.widgets.BaseWidget
import org.isoron.uhabits.widgets.BaseWidgetProvider

class WeeklyHabitsWidgetProvider : BaseWidgetProvider() {
    override fun getWidgetFromId(context: Context, id: Int): BaseWidget = WeeklyHabitsWidget(context, id)
}

/**
 * A compact copy of the main screen: every active habit with its last seven days, in a
 * scrollable list. Rows come from [WeeklyHabitsWidgetService]; this class builds the frame
 * around them (background, day header, empty text, tap template).
 */
class WeeklyHabitsWidget(context: Context, widgetId: Int) : BaseWidget(context, widgetId, false) {
    override val defaultHeight: Int = 0
    override val defaultWidth: Int = 0

    override fun getOnClickPendingIntent(context: Context): PendingIntent? = null

    override fun refreshData(widgetView: View) {
        // unused, see getRemoteViews
    }

    override fun buildView(): View? = null

    override fun getRemoteViews(width: Int, height: Int): RemoteViews {
        val app = context.applicationContext as HabitsApplication
        val renderer = WeeklyHabitsRenderer(app, prefs, app.component.midnightTimer)
        val views = RemoteViews(context.packageName, R.layout.widget_weekly_habits)

        views.setInt(R.id.weeklyBackground, "setColorFilter", renderer.backgroundColor)
        views.setInt(R.id.weeklyBackground, "setImageAlpha", prefs.widgetOpacity)

        // The day header only fits when there is room for it plus a couple of rows.
        val showHeader = height >= renderer.headerHeight(width) + 2 * renderer.rowHeight(width)
        if (showHeader) {
            // An Icon, not setImageViewBitmap: on newer Android the list rows are merged into
            // this RemoteViews and a bitmap here would replace the first row's bitmap.
            views.setImageViewIcon(R.id.weeklyHeader, Icon.createWithBitmap(renderer.renderHeader(width)))
            views.setViewVisibility(R.id.weeklyHeader, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.weeklyHeader, View.GONE)
        }
        views.setOnClickPendingIntent(R.id.weeklyHeader, openAppIntent())

        views.setTextColor(R.id.weeklyEmpty, renderer.textColor)
        views.setOnClickPendingIntent(R.id.weeklyEmpty, openAppIntent())

        val serviceIntent = Intent(context, WeeklyHabitsWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.weeklyList, serviceIntent)
        views.setEmptyView(R.id.weeklyList, R.id.weeklyEmpty)
        views.setPendingIntentTemplate(R.id.weeklyList, WeeklyHabitsTapReceiver.template(context))
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(id, R.id.weeklyList)
        return views
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, ListHabitsActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
