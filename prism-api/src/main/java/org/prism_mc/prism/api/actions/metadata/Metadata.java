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

package org.prism_mc.prism.api.actions.metadata;

import java.util.Locale;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {

    private enum MetadataKey {
        SIGN_LINE_1,
        SIGN_LINE_2,
        SIGN_LINE_3,
        SIGN_LINE_4,
        USING,
        ORIGINAL_MESSAGE,
    }

    @Getter
    @Singular("entry")
    public Map<String, String> data;

    public static class MetadataBuilder {

        /**
         * Set the sign text metadata.
         *
         * @param lines The lines
         * @return The builder
         */
        public MetadataBuilder signText(final String[] lines) {
            if (lines.length > 0) {
                entry(MetadataKey.SIGN_LINE_1.toString().toLowerCase(Locale.ENGLISH), lines[0]);
            }
            if (lines.length > 1) {
                entry(MetadataKey.SIGN_LINE_2.toString().toLowerCase(Locale.ENGLISH), lines[1]);
            }
            if (lines.length > 2) {
                entry(MetadataKey.SIGN_LINE_3.toString().toLowerCase(Locale.ENGLISH), lines[2]);
            }
            if (lines.length > 3) {
                entry(MetadataKey.SIGN_LINE_4.toString().toLowerCase(Locale.ENGLISH), lines[3]);
            }

            return this;
        }

        /**
         * Set the using metadata.
         *
         * @param value Using
         * @return The builder
         */
        public MetadataBuilder using(final String value) {
            entry(MetadataKey.USING.toString().toLowerCase(Locale.ENGLISH), value);
            return this;
        }

        /**
         * Set the original message metadata.
         *
         * @param value The original message
         * @return The builder
         */
        public MetadataBuilder originalMessage(final String value) {
            entry(MetadataKey.ORIGINAL_MESSAGE.toString().toLowerCase(Locale.ENGLISH), value);
            return this;
        }
    }
}
