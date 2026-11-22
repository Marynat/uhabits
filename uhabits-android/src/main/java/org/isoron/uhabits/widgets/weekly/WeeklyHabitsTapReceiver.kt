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
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardState
import org.isoron.uhabits.receivers.WidgetReceiver

/**
 * Receives taps on today's box in the weekly habits widget.
 *
 * A widget list allows only one PendingIntent template for all rows, but yes/no habits
 * need a broadcast (the existing toggle in WidgetReceiver) and numerical habits need an
 * activity (the number dialog the checkmark widget opens). This receiver is that single
 * template and hands each tap to the right place.
 */
class WeeklyHabitsTapReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HabitsApplication
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1)
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, -1)
        val habit = app.component.habitList.getById(habitId)
        if (habit == null || timestamp < 0) {
            Log.w(TAG, "Ignoring tap: habit=$habitId timestamp=$timestamp")
            return
        }
        when (intent.action) {
            ACTION_TOGGLE -> context.sendBroadcast(
                Intent(context, WidgetReceiver::class.java).apply {
                    action = WidgetReceiver.ACTION_TOGGLE_REPETITION
                    data = Uri.parse(habit.uriString)
                    putExtra(EXTRA_TIMESTAMP, timestamp)
                }
            )
            ACTION_EDIT_VALUE -> context.startActivity(
                Intent(context, ListHabitsActivity::class.java).apply {
                    action = ListHabitsActivity.ACTION_EDIT
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("habit", habitId)
                    putExtra("timestamp", timestamp)
                }
            )
        }
    }

    companion object {
        private const val TAG = "WeeklyHabitsTap"
        const val ACTION_TOGGLE = "org.isoron.uhabits.weekly.ACTION_TOGGLE"
        const val ACTION_EDIT_VALUE = "org.isoron.uhabits.weekly.ACTION_EDIT_VALUE"
        private const val EXTRA_HABIT_ID = "habit"
        private const val EXTRA_TIMESTAMP = "timestamp"

        fun template(context: Context): PendingIntent {
            var flags = FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags = flags or FLAG_MUTABLE
            return PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, WeeklyHabitsTapReceiver::class.java),
                flags
            )
        }

        fun fillIn(card: HabitCardState, today: LocalDate) = Intent().apply {
            action = if (card.isNumerical) ACTION_EDIT_VALUE else ACTION_TOGGLE
            putExtra(EXTRA_HABIT_ID, card.id)
            putExtra(EXTRA_TIMESTAMP, today.unixTime)
        }
    }
}
