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

import java.util.Locale

/**
 * Short label shown instead of the habit name in the weekly habits widget.
 *
 * Several words: the first letter of each word, uppercased ("Wake up early" -> "WUE").
 * One word: its first three letters ("Meditate" -> "Med"). Counts code points, so a
 * leading emoji stays whole.
 */
fun weeklyShortName(name: String): String {
    val words = name.trim().split(' ').filter { it.isNotBlank() }
    return when (words.size) {
        0 -> ""
        1 -> words[0].takeCodePoints(3)
        else -> words.joinToString("") { it.takeCodePoints(1).uppercase(Locale.getDefault()) }
    }
}

private fun String.takeCodePoints(n: Int): String {
    val count = codePointCount(0, length)
    if (count <= n) return this
    return substring(0, offsetByCodePoints(0, n))
}
