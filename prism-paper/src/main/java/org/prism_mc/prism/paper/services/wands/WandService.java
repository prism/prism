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

package org.prism_mc.prism.paper.services.wands;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.services.wands.Wand;
import org.prism_mc.prism.api.services.wands.WandMode;
import org.prism_mc.prism.paper.services.messages.MessageService;

@Singleton
public class WandService {

    /**
     * Cache all players with active wands.
     */
    private final Map<Player, Wand> activeWands = new HashMap<>();

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * Wand providers.
     */
    private final Map<WandMode, Provider<Wand>> wandProviders;

    /**
     * Construct the wand service.
     *
     * @param messageService The message service
     * @param wandProviders The wand providers
     */
    @Inject
    public WandService(MessageService messageService, Map<WandMode, Provider<Wand>> wandProviders) {
        this.messageService = messageService;
        this.wandProviders = wandProviders;
    }

    /**
     * Activate a wand for a player.
     *
     * @param player The player
     * @param wandMode The wand mode
     */
    public void activateWand(Player player, WandMode wandMode) {
        if (hasActiveWand(player)) {
            messageService.wandSwitched(player, wandMode);
        } else {
            messageService.wandActivated(player, wandMode);
        }

        Wand wand = wandProviders.get(wandMode).get();
        wand.setOwner(player);

        activeWands.put(player, wand);
    }

    /**
     * Deactivate the current wand.
     *
     * @param player The player
     */
    public void deactivateWand(Player player) {
        Optional<Wand> optionalWand = getWand(player);
        if (optionalWand.isPresent()) {
            messageService.wandDeactivated(player, optionalWand.get().mode());

            activeWands.remove(player);
        }
    }

    /**
     * Get the wand, if any.
     *
     * @param player The player
     * @return The wand, if any.
     */
    public Optional<Wand> getWand(Player player) {
        return Optional.ofNullable(activeWands.get(player));
    }

    /**
     * Check if a player has an active wand.
     *
     * @param player The player
     * @return True if any wand active
     */
    public boolean hasActiveWand(Player player) {
        return activeWands.containsKey(player);
    }

    /**
     * Switch modes.
     *
     * <p>This is simply an alias for activateWand, but is clearer to API users.</p>
     *
     * @param player The player
     * @param wandMode The new wand mode
     */
    public void switchMode(Player player, WandMode wandMode) {
        activateWand(player, wandMode);
    }
}
