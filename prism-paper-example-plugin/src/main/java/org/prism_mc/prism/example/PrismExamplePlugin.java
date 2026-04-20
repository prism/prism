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

package org.prism_mc.prism.example;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.modifications.ModificationHandler;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.paper.api.PrismPaperApi;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;

public class PrismExamplePlugin extends JavaPlugin implements Listener {

    private PrismPaperApi prism;
    private ActionType sprintToggle;
    private ActionType customBreak;

    @Override
    public void onEnable() {
        // 1. Get the Prism API via Bukkit's ServicesManager
        RegisteredServiceProvider<PrismPaperApi> provider = Bukkit.getServicesManager()
            .getRegistration(PrismPaperApi.class);

        if (provider == null) {
            getLogger().severe("Prism not found!");
            setEnabled(false);

            return;
        }

        prism = provider.getProvider();

        // 2. Register a custom generic action type with a past tense string.
        //    The past tense is the verb shown in lookup results
        //    (e.g. "Player toggled sprint on (started sprinting)").
        //    Server owners can override by adding prism.past-tense.sprint-toggle to their locale file.
        sprintToggle = prism.actionTypeRegistry().registerGenericAction("sprint-toggle", "toggled sprint");

        // 3. Register a custom block action with a custom rollback/restore handler.
        //    When rolled back, this places a diamond block instead of restoring the original,
        //    proving the custom handler runs instead of built-in logic.
        customBreak = prism
            .actionTypeRegistry()
            .registerBlockAction(
                "custom-break",
                ActionResultType.REMOVES,
                true,
                new CustomBreakHandler(),
                "custom-broke"
            );

        getLogger().info("Registered custom action types: sprint-toggle, custom-break");

        // 4. Listen for events and record activities
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onSprintToggle(PlayerToggleSprintEvent event) {
        String desc = event.isSprinting() ? "on (started sprinting)" : "off (stopped sprinting)";
        var action = prism.actionFactory().createGenericAction(sprintToggle, desc);

        var activity = PaperActivity.builder()
            .action(action)
            .location(event.getPlayer().getLocation())
            .cause(event.getPlayer())
            .build();

        prism.recordingService().addToQueue(activity);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Only track gold block breaks with the custom action
        if (event.getBlock().getType() != Material.GOLD_BLOCK) {
            return;
        }

        var action = prism.actionFactory().createBlockAction(customBreak, event.getBlock().getState());

        var activity = PaperActivity.builder()
            .action(action)
            .location(event.getBlock().getLocation())
            .cause(event.getPlayer())
            .build();

        prism.recordingService().addToQueue(activity);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("prismexample")) {
            return false;
        }

        // Query the last 5 sprint-toggle activities
        Bukkit.getAsyncScheduler()
            .runNow(this, task -> {
                try {
                    var query = PaperActivityQuery.builder()
                        .actionTypeKeys(List.of("sprint-toggle"))
                        .lookup(true)
                        .grouped(false)
                        .limit(5)
                        .build();

                    List<Activity> results = prism.storageAdapter().queryActivities(query);

                    if (results.isEmpty()) {
                        sender.sendMessage("No sprint-toggle activities found.");
                        return;
                    }

                    sender.sendMessage("Last " + results.size() + " sprint-toggle activities:");
                    for (Activity activity : results) {
                        sender.sendMessage(" - " + activity.action().descriptor() + " at " + activity.coordinate());
                    }
                } catch (Exception e) {
                    sender.sendMessage("Query failed: " + e.getMessage());
                    getLogger().warning("Failed to query activities: " + e.getMessage());
                }
            });

        return true;
    }

    /**
     * Custom rollback/restore handler for gold block breaks.
     *
     * <p>On rollback, places a diamond block instead of restoring the original gold block.
     * On restore, removes the block (sets to air). The diamond block on rollback serves
     * as visual proof that the custom handler ran instead of Prism's built-in logic.</p>
     */
    private static class CustomBreakHandler implements ModificationHandler {

        @Override
        public ModificationResult applyRollback(
            ModificationRuleset modificationRuleset,
            Object owner,
            Activity activityContext,
            ModificationQueueMode mode
        ) {
            if (mode == ModificationQueueMode.COMPLETING) {
                var world = Bukkit.getWorld(activityContext.worldUuid());
                if (world != null) {
                    var coord = activityContext.coordinate();
                    var block = world.getBlockAt(coord.intX(), coord.intY(), coord.intZ());
                    block.setType(Material.DIAMOND_BLOCK);
                }
            }

            return ModificationResult.builder().activity(activityContext).statusFromMode(mode).build();
        }

        @Override
        public ModificationResult applyRestore(
            ModificationRuleset modificationRuleset,
            Object owner,
            Activity activityContext,
            ModificationQueueMode mode
        ) {
            if (mode == ModificationQueueMode.COMPLETING) {
                var world = Bukkit.getWorld(activityContext.worldUuid());
                if (world != null) {
                    var coord = activityContext.coordinate();
                    var block = world.getBlockAt(coord.intX(), coord.intY(), coord.intZ());
                    block.setType(Material.AIR);
                }
            }

            return ModificationResult.builder().activity(activityContext).statusFromMode(mode).build();
        }
    }
}
