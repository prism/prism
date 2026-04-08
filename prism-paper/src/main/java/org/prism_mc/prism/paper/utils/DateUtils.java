/*
 * prism
 *
 * Copyright (c) 2022 M Botsko (viveleroi)
 *                    Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.prism_mc.prism.paper.utils;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateUtils {

    /**
     * The pattern for parsing time durations.
     */
    private static final Pattern TIME_PATTERN = Pattern.compile("([0-9]+)([shmdw])");

    /**
     * Parses a string duration (e.g. "3d", "12h", "1w") into a unix timestamp (seconds)
     * representing that duration in the past from the current time.
     *
     * @param value The duration string
     * @return The timestamp in seconds, or null if invalid
     */
    public static Long parseTimestamp(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        final Matcher matcher = TIME_PATTERN.matcher(value);
        final Calendar cal = Calendar.getInstance();

        int matchedChars = 0;
        boolean matched = false;

        while (matcher.find()) {
            // Ensure matches are contiguous from the start so we don't silently
            // accept inputs like "foo3d" or "3dfoo12h".
            if (matcher.start() != matchedChars) {
                return null;
            }
            matchedChars = matcher.end();
            matched = true;

            final int time = Integer.parseInt(matcher.group(1));
            final String duration = matcher.group(2);

            switch (duration) {
                case "w":
                    cal.add(Calendar.WEEK_OF_YEAR, -1 * time);
                    break;
                case "d":
                    cal.add(Calendar.DAY_OF_MONTH, -1 * time);
                    break;
                case "h":
                    cal.add(Calendar.HOUR, -1 * time);
                    break;
                case "m":
                    cal.add(Calendar.MINUTE, -1 * time);
                    break;
                case "s":
                    cal.add(Calendar.SECOND, -1 * time);
                    break;
                default:
                    break;
            }
        }

        if (!matched || matchedChars != value.length()) {
            return null;
        }

        return cal.getTime().getTime() / 1000;
    }
}
