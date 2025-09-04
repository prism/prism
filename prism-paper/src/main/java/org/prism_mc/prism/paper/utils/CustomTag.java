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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;

public class CustomTag<T extends Enum<T> & Keyed> implements Tag<T> {

    /**
     * Cache the generic class.
     */
    private final Class<T> clazz;

    /**
     * Cache all values.
     */
    private final EnumSet<T> values;

    /**
     * The namespaced key.
     */
    private final NamespacedKey key = null;

    /**
     * Constructor.
     */
    public CustomTag(Class<T> clazz) {
        this.clazz = clazz;
        this.values = EnumSet.noneOf(clazz);
    }

    /**
     * Constructor.
     *
     * @param tags Tags
     */
    @SafeVarargs
    public CustomTag(Class<T> clazz, Tag<T>... tags) {
        this.clazz = clazz;
        this.values = EnumSet.noneOf(clazz);
        append(tags);
    }

    /**
     * Constructor.
     *
     * @param values Values
     */
    public CustomTag(Class<T> clazz, T... values) {
        this.clazz = clazz;
        this.values = EnumSet.noneOf(clazz);
        append(values);
    }

    /**
     * This returns null because it's never accessible to anyone but us,
     * we have absolutely zero use for the namespace key right now.
     */
    @NotNull
    @Override
    public NamespacedKey getKey() {
        return key;
    }

    /**
     * Append values.
     *
     * @param values The values
     * @return The custom tag
     */
    @SafeVarargs
    public final CustomTag<T> append(T... values) {
        this.values.addAll(Arrays.asList(values));
        return this;
    }

    /**
     * Add new Tags to the group.
     *
     * @param tags Tags
     * @return The custom tag
     */
    @SafeVarargs
    public final CustomTag<T> append(Tag<T>... tags) {
        for (Tag<T> tag : tags) {
            this.values.addAll(tag.getValues());
        }

        return this;
    }

    /**
     * Append a segment and mode.
     *
     * @param segment String
     * @param mode MatchMode
     * @return The custom tag
     */
    public CustomTag<T> append(String segment, MatchMode mode) {
        segment = segment.toUpperCase();

        switch (mode) {
            case PREFIX:
                for (var value : clazz.getEnumConstants()) {
                    if (value.name().startsWith(segment)) {
                        values.add(value);
                    }
                }
                break;
            case SUFFIX:
                for (var value : clazz.getEnumConstants()) {
                    if (value.name().endsWith(segment)) {
                        values.add(value);
                    }
                }
                break;
            case CONTAINS:
                for (var value : clazz.getEnumConstants()) {
                    if (value.name().contains(segment)) {
                        values.add(value);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException(mode.name() + " is NOT a valid rule");
        }

        return this;
    }

    /**
     * Exclude certain values.
     *
     * @param values Values to exclude
     * @return The custom tag.
     */
    public CustomTag<T> exclude(T... values) {
        for (var value : values) {
            this.values.remove(value);
        }

        return this;
    }

    /**
     * Exclude certain tags.
     *
     * @param tags Tags to exclude
     * @return The custom tag.
     */
    @SafeVarargs
    public final CustomTag<T> exclude(Tag<T>... tags) {
        for (var tag : tags) {
            this.values.removeAll(tag.getValues());
        }

        return this;
    }

    /**
     * Exclude tags from this group.
     *
     * @param segment String
     * @param mode MatchMode
     * @return MaterialTag
     */
    public CustomTag<T> exclude(String segment, MatchMode mode) {
        segment = segment.toUpperCase();

        switch (mode) {
            case PREFIX:
                for (var value : clazz.getEnumConstants()) {
                    if (value.name().startsWith(segment)) {
                        values.remove(value);
                    }
                }
                break;
            case SUFFIX:
                for (var value : clazz.getEnumConstants()) {
                    if (value.name().endsWith(segment)) {
                        values.remove(value);
                    }
                }
                break;
            case CONTAINS:
                for (var value : clazz.getEnumConstants()) {
                    if (value.name().contains(segment)) {
                        values.remove(value);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException(mode.name() + " is NOT a valid rule");
        }

        return this;
    }

    @NotNull
    @Override
    public Set<T> getValues() {
        return values;
    }

    /**
     * Check if the tag is empty.
     *
     * @return True if empty
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean isTagged(@NotNull T value) {
        return values.contains(value);
    }

    @Override
    public String toString() {
        return values.toString();
    }

    public enum MatchMode {
        PREFIX,
        SUFFIX,
        CONTAINS,
    }
}
