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
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.platform.time.getToday
import org.isoron.uhabits.R
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardState
import org.isoron.uhabits.core.ui.views.Theme
import org.isoron.uhabits.utils.StyledResources
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WeeklyHabitsRow(val bitmap: Bitmap, val today: Rect)

/**
 * The day header, as text for the widget's own text views. [labels] run left to right.
 * The padding puts the seven columns exactly over the rows' day buttons.
 */
class WeeklyHabitsHeader(
    val labels: List<String>,
    val paddingLeft: Int,
    val paddingRight: Int,
    val height: Int,
    val textSize: Float,
    val textColor: Int,
    val backgroundColor: Int
)

/**
 * Draws the main screen's habit rows into bitmaps for the weekly habits widget, and lays out
 * the day header to match them.
 *
 * Views are laid out at the width the main screen would use. When the widget is narrower than
 * one full row, the finished bitmap is scaled down, so the widget keeps the main screen's look.
 */
class WeeklyHabitsRenderer(
    appContext: Context,
    private val preferences: Preferences
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

    /**
     * Same labels, text style and column positions as the main screen's HeaderView, but as
     * text: a bitmap in the widget's frame shifts the list rows' bitmaps by one on some
     * Android builds, so every row would show the image of the row above it.
     *
     * [widthPx] is the rows' width. When the widget is wider ([widgetWidthPx], e.g. in
     * landscape) the rows sit at its start edge, so the extra space goes on the end side.
     */
    fun header(widthPx: Int, widgetWidthPx: Int = widthPx): WeeklyHabitsHeader {
        val scale = scaleFor(widthPx)
        val buttonWidth = context.resources.getDimension(R.dimen.checkmarkWidth)
        val columns = (BUTTON_COUNT * buttonWidth * scale).roundToInt()
        val edge = (cardMargin * scale).roundToInt()
        val start = max(0, widthPx - columns - edge)
        val end = edge + max(0, widgetWidthPx - widthPx)
        val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        val today = getToday()
        val formatter = JavaLocalDateFormatter(Locale.getDefault())
        val days = (0 until BUTTON_COUNT).map { index ->
            val date = today.minus(index)
            formatter.shortWeekdayName(date).uppercase() + "\n" + date.day
        }
        // Today is on the left unless the order is reversed, and everything mirrors in RTL.
        val todayOnLeft = preferences.isCheckmarkSequenceReversed == rtl
        return WeeklyHabitsHeader(
            labels = if (todayOnLeft) days else days.reversed(),
            paddingLeft = if (rtl) end else start,
            paddingRight = if (rtl) start else end,
            height = headerHeight(widthPx),
            textSize = context.resources.getDimension(R.dimen.tinyTextSize) * scale,
            textColor = styled.getColor(R.attr.contrast60),
            backgroundColor = styled.getColor(R.attr.headerBackgroundColor)
        )
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
