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

package org.prism_mc.prism.bukkit.services.messages;

import net.kyori.moonshine.annotation.Message;
import net.kyori.moonshine.annotation.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.PaginatedResults;
import org.prism_mc.prism.api.activities.AbstractActivity;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.purges.PurgeCycleResult;
import org.prism_mc.prism.api.services.wands.WandMode;
import org.prism_mc.prism.bukkit.services.alerts.BlockAlertData;
import org.prism_mc.prism.bukkit.services.alerts.BlockBreakAlertData;

public interface MessageService {
    @Message("prism.about")
    void about(CommandSender receiver, @Placeholder String version);

    @Message("prism.alert-block-break")
    void alertBlockBreak(CommandSender receiver, @Placeholder BlockBreakAlertData data);

    @Message("prism.alert-block-break-night-vision")
    void alertBlockBreakNightVision(CommandSender receiver, @Placeholder BlockBreakAlertData data);

    @Message("prism.alert-block-place")
    void alertBlockPlace(CommandSender receiver, @Placeholder BlockAlertData data);

    @Message("prism.cache-list-entry")
    void cacheListEntry(
        CommandSender receiver,
        @Placeholder String name,
        @Placeholder Long size,
        @Placeholder Long hits
    );

    @Message("prism.cache-list-header")
    void cacheListHeader(CommandSender receiver);

    @Message("prism.defaults-used")
    void defaultsUsed(CommandSender receiver, @Placeholder String defaults);

    @Message("prism.error.console-only")
    void errorConsoleOnly(CommandSender receiver);

    @Message("prism.error.insufficient-permission")
    void errorInsufficientPermission(CommandSender receiver);

    @Message("prism.error.invalid-page")
    void errorInvalidPage(CommandSender receiver);

    @Message("prism.error.invalid-parameter")
    void errorInvalidParameter(CommandSender receiver);

    @Message("prism.error.no-blocks-removed")
    void errorNoBlocksRemoved(CommandSender receiver);

    @Message("prism.error.no-last-query")
    void errorNoLastQuery(CommandSender receiver);

    @Message("prism.error.non-item-action")
    void errorNonItemAction(CommandSender receiver);

    @Message("prism.error.not-previewable")
    void errorNotPreviewable(CommandSender receiver);

    @Message("prism.error.param-at-invalid-loc")
    void errorParamAtInvalidLocation(CommandSender receiver);

    @Message("prism.error.param-at-no-world")
    void errorParamAtNoWorld(CommandSender receiver);

    @Message("prism.error.param-bounds-invalid-format")
    void errorParamBoundsInvalid(CommandSender receiver);

    @Message("prism.error.param-console-bounds")
    void errorParamConsoleBounds(CommandSender receiver);

    @Message("prism.error.param-console-in")
    void errorParamConsoleIn(CommandSender receiver);

    @Message("prism.error.param-console-radius")
    void errorParamConsoleRadius(CommandSender receiver);

    @Message("prism.error.param-invalid-block-tag")
    void errorParamInvalidBlockTag(CommandSender receiver);

    @Message("prism.error.param-invalid-entity-type-tag")
    void errorParamInvalidEntityTypeTag(CommandSender receiver);

    @Message("prism.error.param-invalid-item-tag")
    void errorParamInvalidItemTag(CommandSender receiver);

    @Message("prism.error.param-invalid-namespace")
    void errorParamInvalidNamespace(CommandSender receiver);

    @Message("prism.error.param-invalid-world")
    void errorParamInvalidWorld(CommandSender receiver);

    @Message("prism.error.param-r-and-in")
    void errorParamRadiusAndIn(CommandSender receiver);

    @Message("prism.error.player-only")
    void errorPlayerOnly(CommandSender receiver);

    @Message("prism.error.purge-queue-not-free")
    void errorPurgeQueryNotFree(CommandSender receiver);

    @Message("prism.error.queue-report-empty")
    void errorQueueReportEmpty(CommandSender receiver);

    @Message("prism.error.queue-missing")
    void errorQueueMissing(CommandSender receiver);

    @Message("prism.error.queue-not-free")
    void errorQueueNotFree(CommandSender receiver);

    @Message("prism.error.queue-result-missing")
    void errorQueueResultMissing(CommandSender receiver);

    @Message("prism.error.query-exec")
    void errorQueryExec(CommandSender receiver);

    @Message("prism.error.record-stats")
    void errorRecordStats(CommandSender receiver);

    @Message("prism.error.reload-locale")
    void errorReloadLocale(CommandSender receiver);

    @Message("prism.error.unknown-command")
    void errorUnknownCommand(CommandSender receiver);

    @Message("prism.error.world-edit-missing")
    void errorWorldEditMissing(CommandSender receiver);

    @Message("prism.error.world-edit-missing-selection")
    void errorWorldEditMissingSelection(CommandSender receiver);

    @Message("prism.error.write-hikari")
    void errorWriteHikari(CommandSender receiver);

    @Message("prism.activity-row-grouped")
    void listActivityRowGrouped(CommandSender receiver, @Placeholder AbstractActivity activity);

    @Message("prism.activity-row-grouped-no-descriptor")
    void listActivityRowGroupedNoDescriptor(CommandSender receiver, @Placeholder AbstractActivity activity);

    @Message("prism.activity-row-grouped-no-quantity")
    void listActivityRowGroupedNoQuantity(CommandSender receiver, @Placeholder AbstractActivity activity);

    @Message("prism.activity-row-grouped-no-descriptor-no-quantity")
    void listActivityRowGroupedNoDescriptorNoQuantity(CommandSender receiver, @Placeholder AbstractActivity activity);

    @Message("prism.activity-row-ungrouped")
    void listActivityRowUngrouped(CommandSender receiver, @Placeholder AbstractActivity activity);

    @Message("prism.activity-row-ungrouped-no-descriptor")
    void listActivityRowUngroupedNoDescriptor(CommandSender receiver, @Placeholder AbstractActivity activity);

    @Message("prism.hikari-file-written")
    void hikariFileWritten(CommandSender receiver);

    @Message("prism.modifications-applied")
    void modificationsApplied(CommandSender receiver, @Placeholder Integer count);

    @Message("prism.modifications-drained-lava")
    void modificationsDrainedLava(CommandSender receiver, @Placeholder Integer count);

    @Message("prism.modifications-moved-entities")
    void modificationsMovedEntities(CommandSender receiver, @Placeholder Integer count);

    @Message("prism.modifications-removed-blocks")
    void modificationsRemovedBlocks(CommandSender receiver, @Placeholder Integer count);

    @Message("prism.modifications-removed-drops")
    void modificationsRemovedDrops(CommandSender receiver, @Placeholder Integer count);

    @Message("prism.modifications-report-queue-header")
    void modificationsReportQueueHeader(CommandSender receiver);

    @Message("prism.modifications-report-queue-entry")
    void modificationsReportQueueEntry(CommandSender receiver, @Placeholder Integer size, @Placeholder String owner);

    @Message("prism.modifications-report-skipped-header")
    void modificationsReportSkippedHeader(CommandSender receiver);

    @Message("prism.modifications-report-skipped-activity")
    void modificationsReportSkippedActivity(
        CommandSender receiver,
        @Placeholder AbstractActivity activity,
        @Placeholder String skipreason
    );

    @Message("prism.modifications-skipped")
    void modificationsSkipped(CommandSender receiver, @Placeholder ModificationQueueResult result);

    @Message("prism.modifications-applied-success")
    void modificationsAppliedSuccess(CommandSender receiver);

    @Message("prism.modifications-planned-success")
    void modificationsAppliedSuccess(CommandSender receiver, @Placeholder Integer count);

    @Message("prism.no-results")
    void noResults(CommandSender receiver);

    @Message("prism.pagination-header")
    void paginationHeader(CommandSender receiver, @Placeholder PaginatedResults<?> pagination);

    @Message("prism.preview-applied")
    void previewApplied(CommandSender receiver);

    @Message("prism.preview-cancelled")
    void previewCancelled(CommandSender receiver);

    @Message("prism.purge-complete")
    void purgeComplete(CommandSender receiver, @Placeholder Integer count);

    @Message("prism.purge-cycle")
    void purgeCycle(CommandSender receiver, @Placeholder PurgeCycleResult result);

    @Message("prism.purge-starting")
    void purgeStarting(CommandSender receiver);

    @Message("prism.reloaded-config")
    void reloadedConfig(CommandSender receiver);

    @Message("prism.reloaded-locales")
    void reloadedLocales(CommandSender receiver);

    @Message("prism.teleporting-to-activity")
    void teleportingToActivity(CommandSender receiver, @Placeholder AbstractActivity activity);

    @Message("prism.teleporting-to")
    void teleportingTo(
        CommandSender receiver,
        @Placeholder String worldname,
        @Placeholder Integer x,
        @Placeholder Integer y,
        @Placeholder Integer z
    );

    @Message("prism.vault-header")
    void vaultHeader(CommandSender receiver, @Placeholder Integer results);

    @Message("prism.wand-activated")
    void wandActivated(Player player, @Placeholder WandMode wandmode);

    @Message("prism.wand-deactivated")
    void wandDeactivated(Player player, @Placeholder WandMode wandmode);

    @Message("prism.wand-switched")
    void wandSwitched(Player player, @Placeholder WandMode wandmode);
}
