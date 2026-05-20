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

package org.prism_mc.prism.loader.services.configuration;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class ModificationConfiguration {

    @Comment("List materials that should be excluded from modifications.")
    private List<String> blockBlacklist = new ArrayList<>();

    @Comment(
        """
        Whether to allow neighbor physics, light updates, and tile-tick scheduling
        when applying rollback/restore block changes. Setting to false suppresses
        cascading effects (sand falling, water flowing, redstone propagating) so a
        restored snapshot is not altered after the fact, and can drastically reduce
        TPS impact on large operations. Override per-command with --physics true/false."""
    )
    private boolean applyPhysics = true;

    @Comment(
        """
        Batch size used when cancelling a preview. The original query is
        re-streamed so the player can be shown live block data without
        retaining state snapshots in memory; larger values mean fewer
        database round-trips at the cost of more transient heap per batch."""
    )
    private int cancelPreviewBatchSize = 10000;

    @Comment(
        """
        When enabled, various processes dispatch /spark heapdump commands.

        REQUIRES the Spark plugin to be installed. Otherwise, command is
        silently rejected. Every call writes a multi-MB .hprof file."""
    )
    private boolean debugMemory = false;

    @Comment("Enables draining lava from the modification area.")
    private boolean drainLava = true;

    @Comment("List entities that should be excluded from modifications.")
    private List<String> entityBlacklist = new ArrayList<>();

    @Comment(
        """
        Hard limit on the number of modifications a single rollback/restore
        operation can query. Activities are streamed in batches so memory stays
        bounded regardless of size, but the cap still guards against runaway
        operations from overly broad queries. Set to 0 for unlimited (not recommended)."""
    )
    private int maxPerOperation = 1000000;

    @Comment(
        """
        Set a maximum number of modifications per task. Splitting up world changes
        can help avoid overloading individual ticks and causing lag.
        This can also reduce client lag as fewer changes are sent to clients at once."""
    )
    private int maxPerTask = 1000;

    @Comment(
        """
        The max length of any dimension of the query bounding box used in pre or post modifications.
        This prevents entity teleports from taking place server-wide if
        you do a rollback without a coordinate bounding box.
        """
    )
    private int maxQueryBoundingBoxLength = 200;

    @Comment("Teleport entities out of the way.")
    private boolean moveEntities = true;

    @Comment(
        """
        Percentage step between progress messages sent to the owner during a
        rollback/restore/preview. With 5, the owner sees at most 19 updates
        (5%, 10%, ..., 95%) before the completion message."""
    )
    private int progressReportStepPercent = 5;

    @Comment(
        """
        Minimum queue size before progress messages are sent to the owner during a
        rollback/restore/preview. Smaller queues finish quickly enough that progress
        reporting is just noise."""
    )
    private int progressReportThreshold = 5000;

    @Comment("A list of (typically unsafe) blocks to remove before a modification occurs.")
    private List<String> removeBlocks = new ArrayList<>();

    @Comment("Enables clearing item/xp drops from a modification area.")
    private boolean removeDrops = true;

    @Comment("The delay in ticks between modification tasks.")
    private long taskDelay = 5;

    /**
     * Constructor.
     */
    public ModificationConfiguration() {
        blockBlacklist.add("bedrock");
        blockBlacklist.add("fire");
        blockBlacklist.add("lava");
        blockBlacklist.add("tnt");

        entityBlacklist.add("creeper");
        entityBlacklist.add("tnt_minecart");
        entityBlacklist.add("wither");

        removeBlocks.add("tnt");
        removeBlocks.add("fire");
    }

    /**
     * Create a modification ruleset builder based on this configuration.
     *
     * @return The builder
     */
    public ModificationRuleset.ModificationRulesetBuilder toRulesetBuilder() {
        return ModificationRuleset.builder()
            .applyPhysics(applyPhysics)
            .blockBlacklist(blockBlacklist)
            .drainLava(drainLava)
            .entityBlacklist(entityBlacklist)
            .maxPerTask(maxPerTask)
            .moveEntities(moveEntities)
            .removeBlocks(removeBlocks)
            .removeDrops(removeDrops)
            .taskDelay(taskDelay);
    }
}
