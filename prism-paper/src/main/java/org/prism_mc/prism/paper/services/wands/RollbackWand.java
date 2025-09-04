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
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ModificationQueueService;
import org.prism_mc.prism.api.services.wands.Wand;
import org.prism_mc.prism.api.services.wands.WandMode;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.providers.TaskChainProvider;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.modifications.PaperRollback;

public class RollbackWand extends AbstractModificationWand implements Wand {

    /**
     * Construct a new inspection wand.
     *
     * @param configurationService The configuration service
     * @param storageAdapter The storage adapter
     * @param messageService The message service
     * @param modificationQueueService The modification queue service
     * @param taskChainProvider The task chain provider
     * @param loggingService The logging service
     */
    @Inject
    public RollbackWand(
        ConfigurationService configurationService,
        StorageAdapter storageAdapter,
        MessageService messageService,
        ModificationQueueService modificationQueueService,
        TaskChainProvider taskChainProvider,
        LoggingService loggingService
    ) {
        super(
            configurationService,
            storageAdapter,
            messageService,
            modificationQueueService,
            taskChainProvider,
            loggingService
        );
    }

    @Override
    public WandMode mode() {
        return WandMode.ROLLBACK;
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
            .limit(1)
            .rollback()
            .build();

        super.use(query, PaperRollback.class);
    }
}
