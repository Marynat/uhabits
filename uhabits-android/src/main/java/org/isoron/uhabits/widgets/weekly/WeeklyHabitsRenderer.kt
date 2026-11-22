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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.ContextThemeWrapper
import android.view.View
import android.view.View.MeasureSpec
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.views.HeaderState
import org.isoron.uhabits.activities.habits.list.views.HeaderView
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardState
import org.isoron.uhabits.core.ui.views.Theme
import org.isoron.uhabits.core.utils.MidnightTimer
import org.isoron.uhabits.utils.StyledResources
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WeeklyHabitsRow(val bitmap: Bitmap, val today: Rect)

/**
 * Draws the main screen's header and habit rows into bitmaps for the weekly habits widget.
 *
 * Views are laid out at the width the main screen would use. When the widget is narrower than
 * one full row, the finished bitmap is scaled down, so the widget keeps the main screen's look.
 */
class WeeklyHabitsRenderer(
    appContext: Context,
    private val preferences: Preferences,
    private val midnightTimer: MidnightTimer
) {
    private val themeSwitcher = WeeklyHabitsThemeSwitcher(appContext, preferences).apply { apply() }

    val context: Context = ContextThemeWrapper(appContext, themeSwitcher.styleRes)

    val theme: Theme get() = themeSwitcher.currentTheme

    private val styled = StyledResources(context)

    val backgroundColor: Int get() = styled.getColor(R.attr.windowBackgroundColor)

    val textColor: Int get() = styled.getColor(R.attr.contrast60)

    private val minimumRowWidth = WeeklyHabitRowView.minimumWidth(context, BUTTON_COUNT)

    private val buttonHeight = context.resources.getDimension(R.dimen.checkmarkHeight)

    private val cardMargin = context.resources.displayMetrics.density *
        WeeklyHabitRowView.CARD_MARGIN_DP

    /** Factor applied to everything drawn for a widget [widthPx] pixels wide. */
    fun scaleFor(widthPx: Int): Float = min(1f, widthPx / minimumRowWidth)

    fun headerHeight(widthPx: Int): Int = (buttonHeight * scaleFor(widthPx)).roundToInt()

    fun rowHeight(widthPx: Int): Int = ((buttonHeight + cardMargin) * scaleFor(widthPx)).roundToInt()

    fun renderHeader(widthPx: Int): Bitmap {
        val header = HeaderView(context, preferences, midnightTimer)
        header.setState(HeaderState(BUTTON_COUNT, 0))
        return render(header, widthPx)
    }

    fun renderRow(card: HabitCardState, widthPx: Int): WeeklyHabitsRow {
        val row = WeeklyHabitRowView(context, preferences)
        val color = when (card.archived) {
            true -> styled.getColor(R.attr.contrast60)
            false -> theme.color(card.color).toInt()
        }
        row.setState(card, weeklyShortName(card.name), color, BUTTON_COUNT)
        val bitmap = render(row, widthPx)
        val today = row.todayButtonBounds()
        val s = bitmap.width.toFloat() / max(1, row.width)
        val scaled = Rect(
            (today.left * s).roundToInt(),
            (today.top * s).roundToInt(),
            (today.right * s).roundToInt(),
            (today.bottom * s).roundToInt()
        )
        return WeeklyHabitsRow(bitmap, scaled)
    }

    private fun render(view: View, widthPx: Int): Bitmap {
        val layoutWidth = max(1, (widthPx / scaleFor(widthPx)).roundToInt())
        view.measure(
            MeasureSpec.makeMeasureSpec(layoutWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val full = Bitmap.createBitmap(
            max(1, view.width),
            max(1, view.height),
            Bitmap.Config.ARGB_8888
        )
        view.draw(Canvas(full))
        if (full.width == widthPx) return full
        val height = max(1, (full.height * widthPx.toFloat() / full.width).roundToInt())
        val scaled = Bitmap.createScaledBitmap(full, max(1, widthPx), height, true)
        full.recycle()
        return scaled
    }

    companion object {
        /** Today plus the six days before it. */
        const val BUTTON_COUNT = 7
    }
}
