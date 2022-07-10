/*
 * Prism (Refracted)
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

package network.darkhelmet.prism.commands;

import com.google.inject.Inject;

import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.SubCommand;

import network.darkhelmet.prism.api.activities.ActivityQuery;
import network.darkhelmet.prism.api.util.Coordinate;
import network.darkhelmet.prism.core.services.configuration.ConfigurationService;
import network.darkhelmet.prism.services.lookup.LookupService;
import network.darkhelmet.prism.utils.LocationUtils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

@Command(value = "prism", alias = {"pr"})
public class NearCommand extends BaseCommand {
    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The lookup service.
     */
    private final LookupService lookupService;

    /**
     * Construct the near command.
     *
     * @param configurationService The configuration service
     * @param lookupService The lookup service
     */
    @Inject
    public NearCommand(
            ConfigurationService configurationService,
            LookupService lookupService) {
        this.configurationService = configurationService;
        this.lookupService = lookupService;
    }

    /**
     * Run the near command. Searches for records nearby the player.
     *
     * @param player The player
     */
    @SubCommand("near")
    public void onNear(final Player player) {
        Location loc = player.getLocation();
        Coordinate minCoordinate = LocationUtils.getMinCoordinate(loc, configurationService.prismConfig().nearRadius());
        Coordinate maxCoordinate = LocationUtils.getMaxCoordinate(loc, configurationService.prismConfig().nearRadius());

        final ActivityQuery query = new ActivityQuery().worldUuid(loc.getWorld().getUID())
            .minCoordinate(minCoordinate).maxCoordinate(maxCoordinate)
            .limit(configurationService.prismConfig().perPage());
        lookupService.lookup(player, query);
    }
}