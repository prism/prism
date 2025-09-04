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
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.wands.Wand;
import org.prism_mc.prism.api.services.wands.WandMode;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.services.lookup.LookupService;

public class InspectionWand implements Wand {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The lookup service.
     */
    private final LookupService lookupService;

    /**
     * The owner.
     */
    private Object owner;

    /**
     * Construct a new inspection wand.
     *
     * @param configurationService The configuration service
     * @param lookupService The lookup server
     */
    @Inject
    public InspectionWand(ConfigurationService configurationService, LookupService lookupService) {
        this.configurationService = configurationService;
        this.lookupService = lookupService;
    }

    @Override
    public WandMode mode() {
        return WandMode.INSPECT;
    }

    @Override
    public void setOwner(Object owner) {
        this.owner = owner;
    }

    @Override
    public void use(UUID worldUuid, Coordinate coordinate) {
        final ActivityQuery query = ActivityQuery.builder()
            .worldUuid(worldUuid)
            .coordinate(coordinate)
            .limit(configurationService.prismConfig().defaults().perPage())
            .build();

        lookupService.lookup((CommandSender) owner, query);
    }
}
