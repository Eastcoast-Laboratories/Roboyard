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

package driftingdroids.model;

import android.util.Log;

/** a very simple logger class */
public class Logger {

    public static void println(String msg) {
        System.out.println(msg);
    }
    
    /**
     * Log a formatted message with variable arguments
     * @param level Log level (from android.util.Log)
     * @param tag Log tag
     * @param format Format string (like in String.format)
     * @param args Variable arguments to insert into the format string
     */
    public static void println(int level, String tag, String format, Object... args) {
        String message;
        try {
            message = String.format(format, args);
        } catch (Exception e) {
            message = format + " [Error formatting log message: " + e.getMessage() + "]";
        }
        
        // First log to Android system log
        Log.println(level, tag, message);
        
        // Also output to standard output for debugging purposes
        System.out.println(tag + ": " + message);
    }
}
