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

package org.prism_mc.prism.paper.services.airtags;

import java.security.SecureRandom;

/**
 * Generator for short, chat-typeable airtag IDs.
 */
public final class AirtagIdGenerator {

    /**
     * Alphabet for generated airtag IDs. 0, O, 1, I, and L are deliberately excluded.
     */
    static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

    /**
     * Length of a generated airtag ID, in characters.
     */
    public static final int LENGTH = 6;

    private static final SecureRandom RNG = new SecureRandom();

    private AirtagIdGenerator() {}

    /**
     * Generate a fresh airtag ID.
     *
     * @return The new ID
     */
    public static String generate() {
        char[] chars = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            chars[i] = ALPHABET.charAt(RNG.nextInt(ALPHABET.length()));
        }
        return new String(chars);
    }
}
