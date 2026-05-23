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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * Resolves a list of material names and a list of item-tag ids into a single set of materials.
 */
public final class MaterialTagResolver {

    private MaterialTagResolver() {}

    /**
     * Resolve the given materials and item tags.
     *
     * @param materials Material names (case-insensitive), or null
     * @param tags Namespaced item-tag ids (e.g. {@code minecraft:swords}), or null
     * @param tagKey The Bukkit tag registry key (e.g. {@code items})
     * @return The resolved materials plus any unresolved material names and tag ids
     */
    public static Result resolve(List<String> materials, List<String> tags, String tagKey) {
        CustomTag<Material> resolved = new CustomTag<>(Material.class);
        List<String> invalidMaterials = new ArrayList<>();
        List<String> invalidTags = new ArrayList<>();

        if (!ListUtils.isNullOrEmpty(materials)) {
            for (String materialKey : materials) {
                try {
                    resolved.append(Material.valueOf(materialKey.toUpperCase(Locale.ENGLISH)));
                } catch (IllegalArgumentException e) {
                    invalidMaterials.add(materialKey);
                }
            }
        }

        if (!ListUtils.isNullOrEmpty(tags)) {
            for (String itemTag : tags) {
                var namespacedKey = NamespacedKey.fromString(itemTag);
                if (namespacedKey != null) {
                    var tag = Bukkit.getTag(tagKey, namespacedKey, Material.class);
                    if (tag != null) {
                        resolved.append(tag);

                        continue;
                    }
                }

                invalidTags.add(itemTag);
            }
        }

        return new Result(resolved, invalidMaterials, invalidTags);
    }

    /**
     * The outcome of a resolution.
     *
     * @param tags The resolved materials
     * @param invalidMaterials Material names that didn't match a {@link Material}
     * @param invalidTags Tag ids that didn't resolve to an item tag
     */
    public record Result(CustomTag<Material> tags, List<String> invalidMaterials, List<String> invalidTags) {}
}
