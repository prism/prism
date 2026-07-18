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

package org.prism_mc.prism.paper.permissions;

import dev.triumphteam.cmd.core.argument.keyed.Flag;
import java.util.List;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

/**
 * Single source of truth for command flag definitions.
 *
 * <p>Each {@link Definition} holds the short alias, long name, and optional
 * argument type. {@link #toFlag()} converts to the triumph-cmd {@link Flag}
 * the command manager registers, while the lists themselves drive
 * {@link PrismPermissions}' permission tree and the tab-complete listener's
 * short→long resolution. Add or rename a flag here and every consumer
 * follows automatically.
 */
@UtilityClass
public class PrismFlags {

    /**
     * @param shortName     The single-letter / short flag (e.g. {@code "c"})
     * @param longName      The long flag name (e.g. {@code "count"})
     * @param argumentType  The flag's value type, or {@code null} for a switch
     */
    public record Definition(String shortName, String longName, Class<?> argumentType) {
        /** Build the triumph-cmd {@link Flag} for command-manager registration. */
        public Flag toFlag() {
            if (argumentType == null) {
                return Flag.flag(shortName).longFlag(longName).build();
            }

            return Flag.flag(shortName).longFlag(longName).argument(argumentType).build();
        }
    }

    /** Flags shared by lookup, modify, vault, and purge commands. */
    public static final List<Definition> QUERY = List.of(
        new Definition("dl", "drainlava", Boolean.class),
        new Definition("ow", "overwrite", null),
        new Definition("nd", "nodefaults", null),
        new Definition("ng", "nogroup", null),
        new Definition("ph", "physics", Boolean.class),
        new Definition("rd", "removedrops", Boolean.class),
        new Definition("c", "count", null),
        new Definition("s", "sort", String.class),
        new Definition("sh", "share", String.class)
    );

    /** Flags exposed only by {@code /pr purge start}. */
    public static final List<Definition> PURGE = List.of(
        new Definition("nd", "nodefaults", null),
        new Definition("v", "verbose", null)
    );

    /**
     * Every distinct long flag name across all flag sets. A query command and a
     * purge command register different flag sets, so a single permission sweep
     * over this deduplicated list covers whichever set the sender actually used.
     */
    public static final List<String> LONG_NAMES = Stream.concat(QUERY.stream(), PURGE.stream())
        .map(Definition::longName)
        .distinct()
        .toList();

    /** Convert a definition list into the {@link Flag}[] varargs the command manager wants. */
    public static Flag[] toFlagArray(List<Definition> defs) {
        return defs.stream().map(Definition::toFlag).toArray(Flag[]::new);
    }

    /**
     * Resolve a typed flag token (short or long) to its long name, falling
     * back to the input if unrecognized. Used by tab-complete filtering to
     * look up the registered permission node.
     */
    public static String resolveLongName(String token) {
        for (var def : QUERY) {
            if (def.shortName().equals(token) || def.longName().equals(token)) {
                return def.longName();
            }
        }

        for (var def : PURGE) {
            if (def.shortName().equals(token) || def.longName().equals(token)) {
                return def.longName();
            }
        }

        return token;
    }
}
