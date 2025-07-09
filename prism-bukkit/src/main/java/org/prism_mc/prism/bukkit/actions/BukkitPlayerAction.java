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

package org.prism_mc.prism.bukkit.actions;

import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.actions.PlayerAction;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.containers.PlayerContainer;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;

public class BukkitPlayerAction extends BukkitAction implements PlayerAction {

    /**
     * The player container.
     */
    @Getter
    private final PlayerContainer playerContainer;

    /**
     * Construct a new item stack action.
     *
     * @param type The action type
     * @param player The player
     */
    public BukkitPlayerAction(ActionType type, Player player) {
        this(type, player.getUniqueId(), player.getName());
    }

    /**
     * Construct a new item stack action.
     *
     * @param type The action type
     * @param uuid The uuid
     * @param name The name
     */
    public BukkitPlayerAction(ActionType type, UUID uuid, String name) {
        super(type);
        this.playerContainer = new PlayerContainer(name, uuid);
    }

    @Override
    public Component descriptorComponent() {
        return Component.translatable(playerContainer.name());
    }

    @Override
    public ModificationResult applyRollback(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    ) {
        return ModificationResult.builder().activity(activityContext).skipped().target(playerContainer.name()).build();
    }

    @Override
    public ModificationResult applyRestore(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    ) {
        return ModificationResult.builder().activity(activityContext).skipped().target(playerContainer.name()).build();
    }

    @Override
    public String toString() {
        return String.format("PlayerAction{playerContainer=%s}", playerContainer);
    }
}
