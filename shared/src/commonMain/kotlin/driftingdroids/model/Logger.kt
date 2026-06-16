/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011-2025 Michael Henke

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package driftingdroids.model

import android.util.Log

/** a very simple logger class  */
object Logger {
    fun println(msg: String?) {
        kotlin.io.println(msg)
    }

    /**
     * Log a formatted message with variable arguments
     * @param level Log level (from android.util.Log)
     * @param tag Log tag
     * @param format Format string (like in String.format)
     * @param args Variable arguments to insert into the format string
     */
    fun println(level: Int, tag: String?, format: String, vararg args: Any?) {
        var message: String?
        try {
            message = String.format(format, *args)
        } catch (e: Exception) {
            message = format + " [Error formatting log message: " + e.message + "]"
        }


        // First log to Android system log
        Log.println(level, tag, message)


        // Also output to standard output for debugging purposes
        kotlin.io.println(tag + ": " + message)
    }
}
