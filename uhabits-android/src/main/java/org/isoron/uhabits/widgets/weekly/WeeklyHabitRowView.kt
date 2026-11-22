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
import android.graphics.Rect
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.RingView
import org.isoron.uhabits.activities.habits.list.views.CheckmarkButtonViewFactory
import org.isoron.uhabits.activities.habits.list.views.CheckmarkPanelState
import org.isoron.uhabits.activities.habits.list.views.CheckmarkPanelView
import org.isoron.uhabits.activities.habits.list.views.CheckmarkPanelViewFactory
import org.isoron.uhabits.activities.habits.list.views.NumberButtonViewFactory
import org.isoron.uhabits.activities.habits.list.views.NumberPanelState
import org.isoron.uhabits.activities.habits.list.views.NumberPanelView
import org.isoron.uhabits.activities.habits.list.views.NumberPanelViewFactory
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardState
import org.isoron.uhabits.utils.InterfaceUtils.dpToPixels

/**
 * One habit row of the weekly habits widget, drawn into a bitmap.
 *
 * Mirrors the layout of the main screen's HabitCardView (score ring, label, checkmark or
 * number panel) and uses the main screen's own panels and buttons for the days. HabitCardView
 * itself is not reused because it needs an Activity (its theme lookup and touch controller).
 * Keep the sizes below in sync with HabitCardView when merging upstream.
 */
class WeeklyHabitRowView(
    context: Context,
    preferences: Preferences
) : FrameLayout(context) {

    private val checkmarkPanel: CheckmarkPanelView =
        CheckmarkPanelViewFactory(
            context,
            preferences,
            CheckmarkButtonViewFactory(context, preferences)
        ).create()

    private val numberPanel: NumberPanelView =
        NumberPanelViewFactory(
            context,
            preferences,
            NumberButtonViewFactory(context, preferences)
        ).create()

    private val scoreRing = RingView(context).apply {
        val margin = dp(RING_MARGIN_DP).toInt()
        val ringSize = dp(RING_SIZE_DP).toInt()
        layoutParams = LinearLayout.LayoutParams(ringSize, ringSize).apply {
            setMargins(margin, 0, margin, 0)
            gravity = Gravity.CENTER
        }
        setThickness(dp(RING_THICKNESS_DP))
    }

    private val label = TextView(context).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
    }

    private val innerFrame = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setBackgroundResource(R.drawable.ripple)
        addView(scoreRing)
        addView(label)
        addView(checkmarkPanel)
        addView(numberPanel)
    }

    private var isNumerical = false

    init {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val margin = dp(CARD_MARGIN_DP).toInt()
        setPadding(margin, 0, margin, margin)
        addView(innerFrame)
    }

    /** Same mapping from HabitCardState to panels as HabitCardView.setState. */
    fun setState(card: HabitCardState, label: String, color: Int, buttonCount: Int) {
        isNumerical = card.isNumerical
        this.label.text = label
        this.label.setTextColor(color)
        scoreRing.setPercentage(card.score.toFloat())
        scoreRing.setPrecision(1.0f / 16)
        scoreRing.setColor(color)
        checkmarkPanel.visibility = if (card.isNumerical) View.GONE else View.VISIBLE
        numberPanel.visibility = if (card.isNumerical) View.VISIBLE else View.GONE
        checkmarkPanel.setState(CheckmarkPanelState(card.checkmarks, color, card.notes, buttonCount))
        numberPanel.setState(
            NumberPanelState(
                card.checkmarks.map { it / 1000.0 },
                card.completed,
                color,
                card.unit,
                card.notes,
                buttonCount
            )
        )
    }

    /** Bounds of today's button, relative to this view. Call after layout. */
    fun todayButtonBounds(): Rect {
        val panel = if (isNumerical) numberPanel else checkmarkPanel
        val button: View = panel.buttons[0]
        var x = button.left
        var y = button.top
        var parent = button.parent as View
        while (parent !== this) {
            x += parent.left
            y += parent.top
            parent = parent.parent as View
        }
        return Rect(x, y, x + button.width, y + button.height)
    }

    private fun dp(value: Float) = dpToPixels(context, value)

    companion object {
        const val CARD_MARGIN_DP = 3f
        const val RING_MARGIN_DP = 8f
        const val RING_SIZE_DP = 15f
        const val RING_THICKNESS_DP = 3f

        /** Room kept for the short name before the row starts to shrink. */
        const val MIN_LABEL_WIDTH_DP = 40f

        /** Narrowest width at which a row with [buttonCount] days is drawn unscaled. */
        fun minimumWidth(context: Context, buttonCount: Int): Float {
            val fixedDp = 2 * CARD_MARGIN_DP + RING_SIZE_DP + 2 * RING_MARGIN_DP + MIN_LABEL_WIDTH_DP
            val buttonWidth = context.resources.getDimension(R.dimen.checkmarkWidth)
            return dpToPixels(context, fixedDp) + buttonCount * buttonWidth
        }
    }
}
