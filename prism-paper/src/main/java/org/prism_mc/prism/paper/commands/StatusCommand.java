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

package org.prism_mc.prism.paper.commands;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.annotations.Command;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.services.recording.RecordingService;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.storage.StorageConnectionStatus;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.purge.PurgeService;
import org.prism_mc.prism.paper.services.recording.wal.WalService;

@Command(value = "prism", alias = { "pr" })
public class StatusCommand {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The purge service.
     */
    private final PurgeService purgeService;

    /**
     * The recording service.
     */
    private final RecordingService recordingService;

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * The version.
     */
    private final String version;

    /**
     * The WAL service.
     */
    private final WalService walService;

    /**
     * Construct the status command.
     *
     * @param configurationService The configuration service
     * @param messageService The message service
     * @param purgeService The purge service
     * @param recordingService The recording service
     * @param storageAdapter The storage adapter
     * @param version The prism version
     * @param walService The WAL service
     */
    @Inject
    public StatusCommand(
        ConfigurationService configurationService,
        MessageService messageService,
        PurgeService purgeService,
        RecordingService recordingService,
        StorageAdapter storageAdapter,
        @Named("version") String version,
        WalService walService
    ) {
        this.configurationService = configurationService;
        this.messageService = messageService;
        this.purgeService = purgeService;
        this.recordingService = recordingService;
        this.storageAdapter = storageAdapter;
        this.version = version;
        this.walService = walService;
    }

    /**
     * Run the status command.
     *
     * @param sender The command sender
     */
    @Command("status")
    @Permission("prism.admin")
    public void onStatus(final CommandSender sender) {
        messageService.statusHeader(sender);

        // Version
        messageService.statusVersion(sender, version);

        // Storage
        String storageType = configurationService.storageConfig().primaryStorageType().name().toLowerCase();
        StatusLabel readyLabel = storageAdapter.ready() ? StatusLabel.READY : StatusLabel.NOT_READY;
        messageService.statusStorage(sender, storageType, readyLabel);

        // Connection pool
        StorageConnectionStatus connectionStatus = storageAdapter.connectionStatus();
        StatusLabel connectedLabel = connectionStatus.connected() ? StatusLabel.CONNECTED : StatusLabel.DISCONNECTED;
        messageService.statusConnection(
            sender,
            connectedLabel,
            connectionStatus.activeConnections(),
            connectionStatus.idleConnections(),
            connectionStatus.totalConnections(),
            connectionStatus.maxConnections(),
            connectionStatus.threadsAwaitingConnection()
        );

        // Recording queue
        int queueSize = recordingService.queue().size();
        int queueCapacity = configurationService.prismConfig().recording().queueMaxCapacity();
        messageService.statusQueue(sender, queueSize, queueCapacity);

        // WAL
        String walMode = configurationService.prismConfig().recording().walMode();
        messageService.statusWal(sender, walMode);

        // Purge
        StatusLabel purgeLabel = purgeService.queueFree() ? StatusLabel.INACTIVE : StatusLabel.ACTIVE;
        messageService.statusPurge(sender, purgeLabel);
    }
}
