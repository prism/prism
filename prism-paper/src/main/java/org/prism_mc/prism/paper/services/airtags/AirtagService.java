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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.utils.CustomTag;
import org.prism_mc.prism.paper.utils.MaterialTagResolver;

@Singleton
public class AirtagService {

    /**
     * Permission node prefix granting a numeric airtag limit, e.g. {@code prism.airtag.limit.5}.
     */
    public static final String LIMIT_PERMISSION_PREFIX = "prism.airtag.limit.";

    /**
     * Sentinel limit value meaning no cap on the number of airtags.
     */
    public static final int UNLIMITED = -1;

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * Construct the airtag service.
     *
     * @param configurationService The configuration service
     * @param loggingService The logging service
     */
    @Inject
    public AirtagService(ConfigurationService configurationService, LoggingService loggingService) {
        this.configurationService = configurationService;
        this.loggingService = loggingService;
    }

    /**
     * Whether the given item is allowed to be airtagged.
     *
     * @param itemStack The item stack
     * @return True if the item may be airtagged
     */
    public boolean isAirtaggable(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        CustomTag<Material> allowed = resolveAllowed();

        return allowed.isEmpty() || allowed.isTagged(itemStack.getType());
    }

    /**
     * Resolve how many airtags a player is allowed to own.
     *
     * @param player The player
     * @return The airtag limit, or {@link #UNLIMITED} for no cap
     */
    public int airtagLimit(Player player) {
        int highest = Integer.MIN_VALUE;

        for (var permission : player.getEffectivePermissions()) {
            if (!permission.getValue()) {
                continue;
            }

            String node = permission.getPermission().toLowerCase(Locale.ROOT);
            if (!node.startsWith(LIMIT_PERMISSION_PREFIX)) {
                continue;
            }

            String suffix = node.substring(LIMIT_PERMISSION_PREFIX.length());
            if (suffix.equals("unlimited")) {
                return UNLIMITED;
            }

            try {
                int value = Integer.parseInt(suffix);
                if (value < 0) {
                    return UNLIMITED;
                }

                highest = Math.max(highest, value);
            } catch (NumberFormatException ignored) {
                // Not a numeric limit node; ignore.
            }
        }

        if (highest != Integer.MIN_VALUE) {
            return highest;
        }

        return configurationService.prismConfig().airtags().defaultLimit();
    }

    /**
     * Whether a player who currently owns {@code currentCount} airtags may create another.
     *
     * @param limit The player's limit (see {@link #airtagLimit(Player)})
     * @param currentCount The number of airtags the player currently owns
     * @return True if another airtag is allowed
     */
    public boolean withinLimit(int limit, int currentCount) {
        return limit == UNLIMITED || currentCount < limit;
    }

    /**
     * Resolve the configured materials and item tags into a single set of allowed materials.
     *
     * @return The allowed materials, empty if no restriction is configured
     */
    private CustomTag<Material> resolveAllowed() {
        var config = configurationService.prismConfig().airtags();
        var result = MaterialTagResolver.resolve(config.materials(), config.tags(), "items");

        for (String material : result.invalidMaterials()) {
            loggingService.warn("Airtag config error: no material matching {0}", material);
        }

        for (String itemTag : result.invalidTags()) {
            loggingService.warn("Airtag config error: invalid item tag {0}", itemTag);
        }

        return result.tags();
    }
}
