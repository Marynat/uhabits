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
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import org.isoron.uhabits.R
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.ThemeSwitcher
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.isoron.uhabits.core.ui.views.Theme

/**
 * Picks the same theme as the main screen (light, dark or pure black, following the
 * user's preference) without touching an Activity, which a widget does not have.
 * AndroidThemeSwitcher casts its context to Activity, so it cannot be used here.
 */
class WeeklyHabitsThemeSwitcher(
    private val context: Context,
    preferences: Preferences
) : ThemeSwitcher(preferences) {

    override var currentTheme: Theme = LightTheme()
        private set

    var styleRes: Int = R.style.AppBaseTheme
        private set

    override fun getSystemTheme(): Int {
        val uiMode = context.resources.configuration.uiMode
        return if ((uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES) THEME_DARK else THEME_LIGHT
    }

    override fun applyDarkTheme() {
        currentTheme = DarkTheme()
        styleRes = R.style.AppBaseThemeDark
    }

    override fun applyLightTheme() {
        currentTheme = LightTheme()
        styleRes = R.style.AppBaseTheme
    }

    override fun applyPureBlackTheme() {
        currentTheme = PureBlackTheme()
        styleRes = R.style.AppBaseThemeDark_PureBlack
    }
}
