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

package org.prism_mc.prism.paper.services.alerts;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.alerts.AlertConfiguration;
import org.prism_mc.prism.loader.services.configuration.alerts.BlockAlertConfiguration;
import org.prism_mc.prism.loader.services.configuration.cache.CacheConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;
import org.prism_mc.prism.paper.services.lookup.LookupService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.utils.BlockUtils;
import org.prism_mc.prism.paper.utils.CustomTag;
import org.prism_mc.prism.paper.utils.ListUtils;
import org.prism_mc.prism.paper.utils.VeinScanner;

@Singleton
public class PaperAlertService {

    /**
     * Cache all block break alerts.
     */
    private final List<BlockBreakAlert> blockBreakAlerts = new ArrayList<>();

    /**
     * Cache all block place alerts.
     */
    private final List<BlockAlert> blockPlaceAlerts = new ArrayList<>();

    /**
     * Cache all item use alerts.
     */
    private final List<ItemAlert> itemUseAlerts = new ArrayList<>();

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The lookup service.
     */
    private final LookupService lookupService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * Cache alerts and their counts.
     */
    private final Cache<String, Integer> alerts;

    /**
     * Cache alerted vein locations
     */
    private final Cache<Location, Player> locations;

    /**
     * Constructor.
     *
     * @param cacheService The cache service
     * @param configurationService The configuration service
     * @param loggingService The logging service
     * @param lookupService The lookup service
     * @param messageService The message service
     */
    @Inject
    public PaperAlertService(
        CacheService cacheService,
        ConfigurationService configurationService,
        LoggingService loggingService,
        LookupService lookupService,
        MessageService messageService
    ) {
        this.configurationService = configurationService;
        this.loggingService = loggingService;
        this.lookupService = lookupService;
        this.messageService = messageService;

        CacheConfiguration cacheConfiguration = configurationService.prismConfig().cache();

        var alertCacheBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.alerts().maxSize())
            .expireAfterAccess(
                cacheConfiguration.alerts().expiresAfterAccess().duration(),
                cacheConfiguration.alerts().expiresAfterAccess().timeUnit()
            )
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting alert from cache: Key: %s, Value: %s, Removal Cause: %s";
                loggingService.debug(String.format(msg, key, value, cause));
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing alert from cache: Key: %s, Value: %s, Removal Cause: %s";
                loggingService.debug(String.format(msg, key, value, cause));
            });

        if (cacheConfiguration.recordStats()) {
            alertCacheBuilder.recordStats();
        }

        alerts = alertCacheBuilder.build();
        cacheService.caches().put("alerts", alerts);

        var locationCacheBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.alertedLocations().maxSize())
            .expireAfterAccess(
                cacheConfiguration.alertedLocations().expiresAfterAccess().duration(),
                cacheConfiguration.alertedLocations().expiresAfterAccess().timeUnit()
            )
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting alerted location from cache: Key: %s, Value: %s, Removal Cause: %s";
                loggingService.debug(String.format(msg, key, value, cause));
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing alerted location from cache: Key: %s, Value: %s, Removal Cause: %s";
                loggingService.debug(String.format(msg, key, value, cause));
            });

        if (cacheConfiguration.recordStats()) {
            locationCacheBuilder.recordStats();
        }

        locations = locationCacheBuilder.build();
        cacheService.caches().put("alerts", alerts);

        loadAlerts();
    }

    /**
     * Trigger any configured alerts for a block break.
     *
     * @param block The block
     * @param player The player
     */
    public void alertBlockBreak(Block block, Player player) {
        var alertKey = createAlertKey(player, "block-break", block.getType().name());

        if (!shouldAlert(player, alertKey)) {
            return;
        }

        // Location was part of a prior vein, ignore
        if (locations.getIfPresent(block.getLocation()) != null) {
            return;
        }

        // Find the alert for this block type
        var alert = getBlockBreakAlert(block.getType());
        if (alert == null) {
            return;
        }

        // Apply light level bounds
        int lightLevel = BlockUtils.getLightLevel(block);
        if (
            lightLevel < configurationService.prismConfig().alerts().blockBreakAlerts().minLightLevel() ||
            lightLevel > configurationService.prismConfig().alerts().blockBreakAlerts().maxLightLevel()
        ) {
            return;
        }

        // Cache the block state as it's being changed
        var blockState = block.getState();
        String blockTranslationKey = blockState.getType().getBlockTranslationKey();

        var query = PaperActivityQuery.builder()
            .grouped(false)
            .actionType(PaperActionTypeRegistry.BLOCK_PLACE)
            .affectedMaterial(blockState.getType().toString().toLowerCase(Locale.ENGLISH))
            .location(blockState.getLocation())
            .limit(1)
            .build();

        lookupService.lookup(query, results -> {
            if (!results.isEmpty()) {
                return;
            }

            VeinScanner veinScanner = new VeinScanner(blockState, alert.materialTag(), alert.config().maxScanCount());
            List<Location> vein = veinScanner.scan();

            // Cache the vein locations
            for (var blockLocation : vein) {
                locations.put(blockLocation, player);
            }

            TextColor color = TextColor.fromCSSHexString(alert.config().hexColor());
            String count = vein.size() + (vein.size() >= alert.config().maxScanCount() ? "+" : "");

            boolean usingNightVision = false;
            for (PotionEffect effect : player.getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.NIGHT_VISION)) {
                    usingNightVision = true;
                    break;
                }
            }

            var data = new BlockBreakAlertData(
                player.getName(),
                blockTranslationKey,
                color,
                count,
                lightLevel,
                Key.key(blockState.getType().getKey().toString())
            );

            for (CommandSender receiver : getReceivers(player)) {
                if (usingNightVision) {
                    messageService.alertBlockBreakNightVision(receiver, data);
                } else {
                    messageService.alertBlockBreak(receiver, data);
                }
            }
        });
    }

    /**
     * Trigger any configured alerts for a block place.
     *
     * @param block The block
     * @param player The player
     */
    public void alertBlockPlace(Block block, Player player) {
        var alertKey = createAlertKey(player, "block-place", block.getType().name());

        if (!shouldAlertWithMaximum(player, alertKey)) {
            return;
        }

        // Find the alert for this block type
        var alert = getBlockPlaceAlert(block.getType());
        if (alert == null) {
            return;
        }

        var blockState = block.getState();
        String blockTranslationKey = blockState.getType().getBlockTranslationKey();
        addAlert(alertKey);

        TextColor color = TextColor.fromCSSHexString(alert.config().hexColor());

        var data = new ItemAlertData(
            player.getName(),
            blockTranslationKey,
            color,
            Key.key(blockState.getType().getKey().toString())
        );

        if (!alertMeetsMaximum(player, alertKey)) {
            for (CommandSender receiver : getReceivers(player)) {
                messageService.alertBlockPlace(receiver, data);
            }
        }
    }

    /**
     * Trigger any configured alerts for an item's use.
     *
     * @param player The player
     * @param itemStack The item stack
     */
    public void alertItemUse(Player player, ItemStack itemStack) {
        var alertKey = createAlertKey(player, "item-use", itemStack.getType().name());

        if (!shouldAlertWithMaximum(player, alertKey)) {
            return;
        }

        // Find the alert for this item type
        var alert = getItemUseAlert(itemStack.getType());
        if (alert == null) {
            return;
        }

        addAlert(alertKey);

        TextColor color = TextColor.fromCSSHexString(alert.config().hexColor());

        var data = new ItemAlertData(
            player.getName(),
            itemStack.translationKey(),
            color,
            Key.key(itemStack.getType().getKey().toString())
        );

        if (!alertMeetsMaximum(player, alertKey)) {
            for (CommandSender receiver : getReceivers(player)) {
                messageService.alertItemUse(receiver, data);
            }
        }
    }

    /**
     * Load all alerts from the config.
     */
    public void loadAlerts() {
        blockBreakAlerts.clear();
        blockPlaceAlerts.clear();

        if (configurationService.prismConfig().alerts().blockBreakAlerts().enabled()) {
            for (var config : configurationService.prismConfig().alerts().blockBreakAlerts().alerts()) {
                blockBreakAlerts.add(new BlockBreakAlert(config, loadMaterialTags(config)));
            }
        }

        if (configurationService.prismConfig().alerts().blockPlaceAlerts().enabled()) {
            for (var config : configurationService.prismConfig().alerts().blockPlaceAlerts().alerts()) {
                blockPlaceAlerts.add(new BlockAlert(config, loadMaterialTags(config)));
            }
        }

        if (configurationService.prismConfig().alerts().itemUseAlerts().enabled()) {
            for (var config : configurationService.prismConfig().alerts().itemUseAlerts().alerts()) {
                itemUseAlerts.add(new ItemAlert(config, loadMaterialTags(config)));
            }
        }
    }

    /**
     * Adds or increments an alert.
     *
     * @param alertKey The alert key
     */
    protected void addAlert(String alertKey) {
        Integer count = alerts.getIfPresent(alertKey);

        alerts.put(alertKey, count == null ? 1 : count + 1);
    }

    /**
     * Alert staff if this player/alert have hit the maximum.
     *
     * @param player Player
     * @param alertKey Alert key
     * @return True if the alert meets the maximum count
     */
    protected boolean alertMeetsMaximum(Player player, String alertKey) {
        var count = alerts.getIfPresent(alertKey);

        // Alert receivers that the user continues
        if (count != null && count == configurationService.prismConfig().alerts().maxAlertsPerEvent()) {
            for (CommandSender receiver : getReceivers(player)) {
                messageService.alertsExceedMaximum(receiver, player.getName());
            }

            return true;
        }

        return false;
    }

    /**
     * Creates an alert key for cache use including the player, location, and base key.
     *
     * @param player Player
     * @param alertType Alert type
     * @param objectKey Base key
     * @return Alert key
     */
    protected String createAlertKey(Player player, String alertType, String objectKey) {
        return String.format("%s-%s-%s", player.getUniqueId(), alertType, objectKey);
    }

    /**
     * Get a block break alert for this material, if any.
     *
     * @param material The material
     * @return The alert, if any
     */
    protected BlockBreakAlert getBlockBreakAlert(Material material) {
        for (var alert : blockBreakAlerts) {
            if (alert.materialTag().isTagged(material)) {
                return alert;
            }
        }

        return null;
    }

    /**
     * Get a block place alert for this material, if any.
     *
     * @param material The material
     * @return The alert, if any
     */
    protected BlockAlert getBlockPlaceAlert(Material material) {
        for (var alert : blockPlaceAlerts) {
            if (alert.materialTag().isTagged(material)) {
                return alert;
            }
        }

        return null;
    }

    /**
     * Get an item use alert for this material, if any.
     *
     * @param material The material
     * @return The alert, if any
     */
    protected ItemAlert getItemUseAlert(Material material) {
        for (var alert : itemUseAlerts) {
            if (alert.materialTag().isTagged(material)) {
                return alert;
            }
        }

        return null;
    }

    /**
     * Load material tags.
     */
    protected Tag<Material> loadMaterialTags(AlertConfiguration config) {
        CustomTag<Material> materialTag = new CustomTag<>(Material.class);

        // Materials
        if (!ListUtils.isNullOrEmpty(config.materials())) {
            for (String materialKey : config.materials()) {
                try {
                    materialTag.append(Material.valueOf(materialKey.toUpperCase(Locale.ENGLISH)));
                } catch (IllegalArgumentException e) {
                    loggingService.warn("Alert config error: No material matching {0}", materialKey);
                }
            }
        }

        // Block material tags
        if (config instanceof BlockAlertConfiguration blockAlertConfiguration) {
            if (!ListUtils.isNullOrEmpty(blockAlertConfiguration.blockTags())) {
                for (String blockTag : blockAlertConfiguration.blockTags()) {
                    var namespacedKey = NamespacedKey.fromString(blockTag);
                    if (namespacedKey != null) {
                        var tag = Bukkit.getTag("blocks", namespacedKey, Material.class);
                        if (tag != null) {
                            materialTag.append(tag);

                            continue;
                        }
                    }

                    loggingService.warn("Alert config error: Invalid block tag {0}", blockTag);
                }
            }
        }

        return materialTag;
    }

    /**
     * Get all receivers of an alert.
     *
     * @param causingPlayer The player who triggered the alert
     * @return All receivers
     */
    protected List<CommandSender> getReceivers(Player causingPlayer) {
        List<CommandSender> receivers = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (
                !player.hasPermission("prism.alerts.receive") ||
                (configurationService.prismConfig().alerts().ignoreSelf() && player.equals(causingPlayer))
            ) {
                continue;
            }

            receivers.add(player);
        }

        return receivers;
    }

    /**
     * Check if we should alert for this alert key.
     *
     * @param player Player
     * @param alertKey The alert key
     * @return True if the alert is valid
     */
    protected boolean shouldAlert(Player player, String alertKey) {
        // Ignore creative
        if (
            configurationService.prismConfig().alerts().ignoreCreative() &&
            player.getGameMode().equals(GameMode.CREATIVE)
        ) {
            return false;
        }

        // Let players bypass
        if (player.hasPermission("prism.alert.bypass")) {
            return false;
        }

        return true;
    }

    /**
     * Check if we should alert for this alert key.
     *
     * @param player Player
     * @param alertKey The alert key
     * @return True if the alert is valid
     */
    protected boolean shouldAlertWithMaximum(Player player, String alertKey) {
        if (!shouldAlert(player, alertKey)) {
            return false;
        }

        // Check prior alert counts
        var count = alerts.getIfPresent(alertKey);

        return !(count != null && count >= configurationService.prismConfig().alerts().maxAlertsPerEvent());
    }
}
