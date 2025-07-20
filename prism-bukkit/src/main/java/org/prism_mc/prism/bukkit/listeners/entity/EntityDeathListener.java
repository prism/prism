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

package org.prism_mc.prism.bukkit.listeners.entity;

import com.google.inject.Inject;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.projectiles.BlockProjectileSource;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.bukkit.actions.BukkitEntityAction;
import org.prism_mc.prism.bukkit.actions.BukkitPlayerAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.listeners.AbstractListener;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class EntityDeathListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public EntityDeathListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listen to entity death events.
     *
     * <p>Note: Player deaths are handled via the PlayerDeathListener.</p>
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        Action action;
        if (event.getEntity() instanceof Player player) {
            if ((!configurationService.prismConfig().actions().playerDeath())) {
                return;
            }

            action = new BukkitPlayerAction(BukkitActionTypeRegistry.PLAYER_DEATH, player);
        } else {
            if (!configurationService.prismConfig().actions().entityDeath()) {
                return;
            }

            action = new BukkitEntityAction(BukkitActionTypeRegistry.ENTITY_DEATH, entity);
        }

        // Resolve cause using last damage
        Object causeObj = null;
        EntityDamageEvent damageEvent = entity.getLastDamageCause();

        if (damageEvent != null && !damageEvent.isCancelled()) {
            if (damageEvent instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {
                causeObj = entityDamageByEntityEvent.getDamager();

                if (causeObj instanceof Projectile projectile) {
                    causeObj = projectile.getShooter();

                    if (causeObj instanceof BlockProjectileSource blockProjectileSource) {
                        causeObj = blockProjectileSource.getBlock();
                    }
                }
            } else if (damageEvent instanceof EntityDamageByBlockEvent) {
                causeObj = ((EntityDamageByBlockEvent) damageEvent).getDamager();
            } else {
                causeObj = damageEvent.getCause();
            }
        }

        var builder = BukkitActivity.builder().action(action).location(entity.getLocation()).cause(causeObj);
        recordingService.addToQueue(builder.build());

        // Log inventory drops
        // While event.getDrops() would include these items, it would also list loot which we do not track
        if (entity instanceof InventoryHolder inventoryHolder) {
            recordItemDropFromInventory(
                inventoryHolder.getInventory(),
                event.getEntity().getLocation(),
                event.getEntity()
            );
        }

        // Log drops from specific entities. We can ignore most as they're only dropping loot.
        // No need to check keepInventory settings because event.getDrops will be empty if it's true
        if (entity instanceof Player || entity instanceof ArmorStand) {
            for (var item : event.getDrops()) {
                recordItemDropActivity(event.getEntity().getLocation(), event.getEntity(), item);
            }
        }
    }
}
