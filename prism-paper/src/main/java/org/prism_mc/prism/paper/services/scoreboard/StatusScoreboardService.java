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

package org.prism_mc.prism.paper.services.scoreboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.prism_mc.prism.api.services.modifications.ModificationQueue;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueService;
import org.prism_mc.prism.api.services.recording.RecordingService;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.storage.StorageConnectionStatus;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.commands.StatusLabel;
import org.prism_mc.prism.paper.services.modifications.PaperRestore;
import org.prism_mc.prism.paper.services.modifications.PaperRollback;
import org.prism_mc.prism.paper.services.purge.PurgeService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;
import org.prism_mc.prism.paper.services.translation.PaperTranslationService;

/**
 * Maintains a per-player sidebar scoreboard that displays live Prism status
 * information (storage, connection pool, queue, WAL mode, purge state and any
 * in-flight modification). Players opt in via the status command; the service
 * schedules a per-entity tick that refreshes line content until the player
 * disables it or disconnects.
 */
@Singleton
public class StatusScoreboardService {

    /**
     * Objective name used for the sidebar scoreboard.
     */
    private static final String OBJECTIVE_NAME = "prism_status";

    /**
     * Update period in ticks (20 ticks = 1 second).
     */
    private static final long UPDATE_PERIOD_TICKS = 20L;

    /**
     * Stable invisible entry strings for each line. Each is a unique color code
     * sequence so entries are distinct without showing visible characters.
     */
    private static final List<String> LINE_ENTRIES = List.of(
        "§0§r",
        "§1§r",
        "§2§r",
        "§3§r",
        "§4§r",
        "§5§r",
        "§6§r",
        "§7§r",
        "§8§r",
        "§9§r",
        "§a§r",
        "§b§r",
        "§c§r"
    );

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The modification queue service.
     */
    private final ModificationQueueService modificationQueueService;

    /**
     * The translation service.
     */
    private final PaperTranslationService translationService;

    /**
     * The scheduler.
     */
    private final PrismScheduler prismScheduler;

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
     * The plugin version string.
     */
    private final String version;

    /**
     * Per-player active scoreboards keyed by player UUID.
     */
    private final Map<UUID, ActiveScoreboard> activeScoreboards = new ConcurrentHashMap<>();

    /**
     * Construct the status scoreboard service.
     *
     * @param configurationService The configuration service
     * @param modificationQueueService The modification queue service
     * @param translationService The translation service
     * @param prismScheduler The scheduler
     * @param purgeService The purge service
     * @param recordingService The recording service
     * @param storageAdapter The storage adapter
     * @param version The plugin version
     */
    @Inject
    public StatusScoreboardService(
        ConfigurationService configurationService,
        ModificationQueueService modificationQueueService,
        PaperTranslationService translationService,
        PrismScheduler prismScheduler,
        PurgeService purgeService,
        RecordingService recordingService,
        StorageAdapter storageAdapter,
        @Named("version") String version
    ) {
        this.configurationService = configurationService;
        this.modificationQueueService = modificationQueueService;
        this.translationService = translationService;
        this.prismScheduler = prismScheduler;
        this.purgeService = purgeService;
        this.recordingService = recordingService;
        this.storageAdapter = storageAdapter;
        this.version = version;
    }

    /**
     * Check if a player currently has the status scoreboard active.
     *
     * @param player The player
     * @return True if active
     */
    public boolean isActive(Player player) {
        return activeScoreboards.containsKey(player.getUniqueId());
    }

    /**
     * Toggle the status scoreboard for a player.
     *
     * @param player The player
     */
    public void toggle(Player player) {
        if (isActive(player)) {
            remove(player);
        } else {
            show(player);
        }
    }

    /**
     * Show the status scoreboard for a player.
     *
     * @param player The player
     */
    public void show(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective(
            OBJECTIVE_NAME,
            Criteria.DUMMY,
            translate(player, "prism.status.scoreboard.title")
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Register one team per line with a stable invisible entry, then set the
        // line content via the team prefix on every update (avoids flicker).
        for (int i = 0; i < LINE_ENTRIES.size(); i++) {
            String entry = LINE_ENTRIES.get(i);
            Team team = scoreboard.registerNewTeam("line" + i);
            team.addEntry(entry);
            // Top line gets the highest score so it sorts to the top of the sidebar.
            objective.getScore(entry).setScore(LINE_ENTRIES.size() - i);
        }

        player.setScoreboard(scoreboard);

        ActiveScoreboard active = new ActiveScoreboard(scoreboard, null);
        activeScoreboards.put(player.getUniqueId(), active);

        // Initial paint so the board has content immediately.
        update(player, active);

        ScheduledTask task = prismScheduler.runForEntityFixedRate(
            player,
            handle -> {
                if (!player.isOnline() || !activeScoreboards.containsKey(player.getUniqueId())) {
                    handle.cancel();
                    return;
                }

                update(player, active);
            },
            UPDATE_PERIOD_TICKS,
            UPDATE_PERIOD_TICKS
        );

        active.task = task;
    }

    /**
     * Remove the status scoreboard for a player.
     *
     * @param player The player
     */
    public void remove(Player player) {
        ActiveScoreboard active = activeScoreboards.remove(player.getUniqueId());
        if (active == null) {
            return;
        }

        if (active.task != null) {
            active.task.cancel();
        }

        if (player.isOnline()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /**
     * Update all line teams on the player's scoreboard with current status.
     *
     * @param player The player
     * @param active The active scoreboard
     */
    private void update(Player player, ActiveScoreboard active) {
        StorageConnectionStatus connectionStatus = storageAdapter.connectionStatus();
        String storageType = configurationService.storageConfig().primaryStorageType().name().toLowerCase();
        StatusLabel readyLabel = storageAdapter.ready() ? StatusLabel.READY : StatusLabel.NOT_READY;
        StatusLabel connectedLabel = connectionStatus.connected() ? StatusLabel.CONNECTED : StatusLabel.DISCONNECTED;
        int queueSize = recordingService.queue().size();
        int queueCapacity = configurationService.prismConfig().recording().queueMaxCapacity();
        String walMode = configurationService.prismConfig().recording().walMode();
        StatusLabel purgeLabel = purgeService.queueFree() ? StatusLabel.INACTIVE : StatusLabel.ACTIVE;
        ModificationQueue modQueue = modificationQueueService.currentQueue();

        Component[] lines = new Component[] {
            translate(player, "prism.status.scoreboard.version", Placeholder.unparsed("version", version)),
            translate(player, "prism.status.scoreboard.storage", Placeholder.unparsed("storage", storageType)),
            translate(player, "prism.status.scoreboard.storage-ready", statusLabel("ready", readyLabel)),
            translate(player, "prism.status.scoreboard.pool", statusLabel("connected", connectedLabel)),
            translate(
                player,
                "prism.status.scoreboard.pool-active",
                Placeholder.unparsed("active", Integer.toString(connectionStatus.activeConnections()))
            ),
            translate(
                player,
                "prism.status.scoreboard.pool-idle",
                Placeholder.unparsed("idle", Integer.toString(connectionStatus.idleConnections()))
            ),
            translate(
                player,
                "prism.status.scoreboard.pool-total",
                Placeholder.unparsed("total", Integer.toString(connectionStatus.totalConnections())),
                Placeholder.unparsed("max", Integer.toString(connectionStatus.maxConnections()))
            ),
            translate(
                player,
                "prism.status.scoreboard.pool-waiting",
                Placeholder.unparsed("awaiting", Integer.toString(connectionStatus.threadsAwaitingConnection()))
            ),
            translate(
                player,
                "prism.status.scoreboard.queue",
                Placeholder.unparsed("size", Integer.toString(queueSize)),
                Placeholder.unparsed("capacity", Integer.toString(queueCapacity))
            ),
            translate(player, "prism.status.scoreboard.wal", Placeholder.unparsed("mode", walMode)),
            translate(player, "prism.status.scoreboard.purge", statusLabel("active", purgeLabel)),
            translate(
                player,
                "prism.status.scoreboard.modification",
                Placeholder.component("type", modificationTypeLabel(player, modQueue))
            ),
            translate(
                player,
                "prism.status.scoreboard.modification-progress",
                Placeholder.unparsed("progress", modificationProgress(modQueue))
            ),
        };

        for (int i = 0; i < lines.length; i++) {
            Team team = active.scoreboard.getTeam("line" + i);
            if (team != null) {
                team.prefix(lines[i]);
            }
        }
    }

    /**
     * Build a placeholder that renders a StatusLabel via its translation key.
     *
     * @param name The placeholder name referenced in the MiniMessage template
     * @param label The status label to render
     * @return A tag resolver that emits the translated label component
     */
    private TagResolver statusLabel(String name, StatusLabel label) {
        return Placeholder.component(name, Component.translatable(label.translationKey()));
    }

    /**
     * Build a translated label describing the current modification queue type.
     *
     * @param player The player whose locale is used for translation
     * @param queue The current modification queue, or null if none is active
     * @return The translated label component
     */
    private Component modificationTypeLabel(Player player, ModificationQueue queue) {
        String key;
        if (queue == null) {
            key = "prism.status.scoreboard.mod-type-none";
        } else if (queue.mode() == ModificationQueueMode.PLANNING) {
            key = "prism.status.scoreboard.mod-type-preview";
        } else if (queue instanceof PaperRollback) {
            key = "prism.status.scoreboard.mod-type-rollback";
        } else if (queue instanceof PaperRestore) {
            key = "prism.status.scoreboard.mod-type-restore";
        } else {
            key = "prism.status.scoreboard.mod-type-none";
        }
        return translate(player, key);
    }

    /**
     * Build a short progress string for the current modification queue.
     *
     * @param queue The current modification queue, or null if none is active
     * @return A "processed/total (percent%)" string, or an em dash placeholder when unavailable
     */
    private String modificationProgress(ModificationQueue queue) {
        if (queue == null) {
            return "—";
        }

        int total = queue.total();
        if (total <= 0) {
            return "—";
        }

        int processed = Math.min(queue.processed(), total);
        int percent = (int) (((long) processed * 100L) / total);
        return processed + "/" + total + " (" + percent + "%)";
    }

    /**
     * Render a translated MiniMessage template for the player.
     *
     * @param player The player whose locale drives template lookup
     * @param key The translation key
     * @param resolvers Tag resolvers used to substitute placeholders in the template
     * @return The deserialized component
     */
    private Component translate(Player player, String key, TagResolver... resolvers) {
        String template = translationService.messageOf(player, key);
        return MiniMessage.miniMessage().deserialize(template, TagResolver.resolver(resolvers));
    }

    /**
     * Holder for a player's active scoreboard and its update task.
     */
    private static final class ActiveScoreboard {

        /**
         * The player-scoped scoreboard.
         */
        private final Scoreboard scoreboard;

        /**
         * The scheduled update task.
         */
        private ScheduledTask task;

        /**
         * Construct a holder.
         *
         * @param scoreboard The player-scoped scoreboard
         * @param task The scheduled update task, or null if not yet scheduled
         */
        private ActiveScoreboard(Scoreboard scoreboard, ScheduledTask task) {
            this.scoreboard = scoreboard;
            this.task = task;
        }
    }
}
