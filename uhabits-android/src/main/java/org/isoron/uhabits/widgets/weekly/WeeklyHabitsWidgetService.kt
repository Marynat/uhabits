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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Looper
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.isoron.platform.time.getToday
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardState
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsPresenter
import org.isoron.uhabits.utils.InterfaceUtils.dpToPixels

class WeeklyHabitsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return WeeklyHabitsViewsFactory(applicationContext, widgetId)
    }
}

/**
 * One row per active habit, in the main screen's order. The data comes from the same
 * presenter the main screen uses (ListHabitsPresenter), so values, notes and completion
 * match what the app shows.
 */
internal class WeeklyHabitsViewsFactory(
    private val context: Context,
    private val widgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private val component = (context.applicationContext as HabitsApplication).component
    private var items: List<HabitCardState> = emptyList()
    private var renderer: WeeklyHabitsRenderer? = null

    override fun onCreate() = Unit

    override fun onDestroy() = Unit

    override fun onDataSetChanged() {
        if (Looper.myLooper() == null) Looper.prepare()
        val prefs = component.preferences
        val r = WeeklyHabitsRenderer(context, prefs, component.midnightTimer)
        renderer = r
        items = ListHabitsPresenter.buildState(
            allHabits = component.habitList,
            filter = HabitMatcher(isArchivedAllowed = false),
            primaryOrder = prefs.defaultPrimaryOrder,
            secondaryOrder = prefs.defaultSecondaryOrder,
            checkmarkCount = WeeklyHabitsRenderer.BUTTON_COUNT,
            theme = r.theme
        ).items
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val card = items.getOrNull(position) ?: return null
        val r = renderer ?: return null
        if (Looper.myLooper() == null) Looper.prepare()
        return buildRow(r, card, currentWidthPx())
    }

    /**
     * Width of the widget in the current orientation (min width in portrait, max width in
     * landscape, like BaseWidget). One layout only: newer Android versions turn collection
     * rows into RemoteCollectionItems, which reject separate portrait/landscape layouts.
     */
    private fun currentWidthPx(): Int {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
        val landscape = context.resources.configuration.orientation == ORIENTATION_LANDSCAPE
        val key = when (landscape) {
            true -> AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            false -> AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
        }
        return dpToPixels(context, options.getInt(key).toFloat()).toInt()
    }

    private fun buildRow(r: WeeklyHabitsRenderer, card: HabitCardState, widthPx: Int): RemoteViews {
        val row = r.renderRow(card, widthPx)
        val today = row.today
        return RemoteViews(context.packageName, R.layout.widget_weekly_habits_row).apply {
            setImageViewBitmap(R.id.weeklyRowImage, row.bitmap)
            setContentDescription(R.id.weeklyRowImage, card.name)
            // Shrink the tap area's container so its child covers exactly today's box.
            setViewPadding(
                R.id.weeklyRowTapArea,
                today.left,
                today.top,
                row.bitmap.width - today.right,
                row.bitmap.height - today.bottom
            )
            setContentDescription(R.id.weeklyRowTap, card.name)
            setOnClickFillInIntent(R.id.weeklyRowTap, WeeklyHabitsTapReceiver.fillIn(card, getToday()))
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
