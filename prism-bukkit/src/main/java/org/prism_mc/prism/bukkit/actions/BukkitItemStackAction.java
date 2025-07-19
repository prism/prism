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

package org.prism_mc.prism.bukkit.actions;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.prism_mc.prism.api.actions.ItemAction;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.containers.PlayerContainer;
import org.prism_mc.prism.api.services.modifications.ModificationPartialReason;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.services.modifications.ModificationSkipReason;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.services.modifications.state.ItemStackStateChange;

public class BukkitItemStackAction extends BukkitMaterialAction implements ItemAction {

    /**
     * The item stack.
     */
    @Getter
    private final ItemStack itemStack;

    /**
     * The read/write nbt.
     */
    private final ReadWriteNBT readWriteNbt;

    /**
     * Construct a new item stack action.
     *
     * @param type The action type
     * @param itemStack The item stack
     */
    public BukkitItemStackAction(ActionType type, ItemStack itemStack) {
        this(type, itemStack, itemStack.getAmount(), null);
        this.descriptor = PlainTextComponentSerializer.plainText().serialize(descriptorComponent());
    }

    /**
     * Construct a new item stack action.
     *
     * @param type The action type
     * @param itemStack The item stack
     * @param quantity The quantity
     * @param descriptor The descriptor
     */
    public BukkitItemStackAction(ActionType type, ItemStack itemStack, int quantity, String descriptor) {
        super(type, itemStack.getType(), descriptor);
        this.itemStack = itemStack;
        this.readWriteNbt = NBT.itemStackToNBT(itemStack);
        this.readWriteNbt.removeKey("count");

        if (quantity <= itemStack.getMaxStackSize()) {
            itemStack.setAmount(quantity);
        }
    }

    @Override
    public Component descriptorComponent() {
        var meta = itemStack.getItemMeta();
        var complete = Component.text();

        if (itemStack.getAmount() > 1) {
            complete
                .append(
                    Component.translatable(
                        "prism.quantity",
                        Argument.component("quantity", Component.text(itemStack.getAmount()))
                    )
                )
                .append(Component.space());
        }

        Component itemName = Component.translatable(itemStack.translationKey());
        if (meta != null && meta.hasItemName()) {
            // Strip custom color/format codes out of item name for consistency
            itemName = Component.text(
                PlainTextComponentSerializer.plainText()
                    .serialize(LegacyComponentSerializer.legacySection().deserialize(meta.getItemName()))
            );
        } else if (meta instanceof SkullMeta skullMeta && skullMeta.hasOwner()) {
            Component name = Component.text("unknown");
            if (skullMeta.getOwningPlayer() != null && skullMeta.getOwningPlayer().getName() != null) {
                name = Component.text(skullMeta.getOwningPlayer().getName());
            }

            itemName = Component.translatable("block.minecraft.player_head.named", name);
        }

        complete.append(itemName);

        if (meta != null && meta.hasDisplayName() && !meta.getDisplayName().isEmpty()) {
            complete
                .append(Component.space())
                .append(Component.text("\""))
                .append(Component.text(meta.getDisplayName()))
                .append(Component.text("\""));
        }

        if (meta instanceof PotionMeta potionMeta) {
            if (
                potionMeta.getBasePotionType() != null && !potionMeta.getBasePotionType().getPotionEffects().isEmpty()
            ) {
                var effect = potionMeta.getBasePotionType().getPotionEffects().getFirst();

                complete
                    .append(Component.space())
                    .append(Component.text("("))
                    .append(Component.translatable(effect.getType().translationKey()))
                    .append(Component.text(")"));
            }
        } else if (
            itemStack.getType().equals(Material.ENCHANTED_BOOK) &&
            meta instanceof EnchantmentStorageMeta enchantmentStorageMeta
        ) {
            if (!enchantmentStorageMeta.getStoredEnchants().isEmpty()) {
                var entry = enchantmentStorageMeta.getStoredEnchants().entrySet().iterator().next();
                complete
                    .append(Component.space())
                    .append(Component.text("("))
                    .append(entry.getKey().displayName(entry.getValue()))
                    .append(Component.text(")"));
            }
        } else if (meta instanceof BookMeta bookMeta && (bookMeta.hasTitle() || bookMeta.hasAuthor())) {
            complete.append(Component.space()).append(Component.text("("));

            if (bookMeta.hasTitle()) {
                complete.append(Component.text(bookMeta.getTitle()));
            }

            if (bookMeta.hasAuthor()) {
                complete.append(Component.text(" by ")).append(Component.text(bookMeta.getAuthor()));
            }

            complete.append(Component.text(")"));
        }

        return complete.build();
    }

    @Override
    public int quantity() {
        return itemStack.getAmount();
    }

    @Override
    public String serializeItemData() {
        return readWriteNbt.toString();
    }

    @Override
    public ModificationResult applyRollback(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    ) {
        // Ignore non-player item rollbacks because they should always be serialized as contents
        // of a broken inventory holder, killed entity, etc.
        if (
            (type().equals(BukkitActionTypeRegistry.ITEM_REMOVE) ||
                type().equals(BukkitActionTypeRegistry.ITEM_INSERT)) &&
            (activityContext.cause() == null || !(activityContext.cause().container() instanceof PlayerContainer))
        ) {
            return ModificationResult.builder()
                .activity(activityContext)
                .skipped()
                .target(itemStack.translationKey())
                .skipReason(ModificationSkipReason.NOT_APPLICABLE)
                .build();
        }

        // The only time we give items back to a player's personal inventory is when they dropped it
        if (type().equals(BukkitActionTypeRegistry.ITEM_DROP)) {
            if (activityContext.cause().container() instanceof PlayerContainer playerContainer) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerContainer.uuid());
                if (offlinePlayer.isOnline()) {
                    return addItem(activityContext, ((Player) offlinePlayer).getInventory());
                }
            }
        } else if (type().resultType().equals(ActionResultType.REMOVES)) {
            var world = Bukkit.getServer().getWorld(activityContext.world().key());
            var location = new Location(
                world,
                activityContext.coordinate().intX(),
                activityContext.coordinate().intY(),
                activityContext.coordinate().intZ()
            );

            if (location.getBlock().getState() instanceof InventoryHolder holder) {
                return addItem(activityContext, holder.getInventory());
            }
        }

        return ModificationResult.builder()
            .skipped()
            .target(itemStack.translationKey())
            .activity(activityContext)
            .build();
    }

    @Override
    public ModificationResult applyRestore(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    ) {
        return ModificationResult.builder()
            .activity(activityContext)
            .skipped()
            .target(itemStack.translationKey())
            .build();
    }

    /**
     * Attempts to add an item to an inventory. If full, returns a partial result.
     *
     * @param activityContext Activity context
     * @param inventory Inventory
     * @return Modification result
     */
    private ModificationResult addItem(Activity activityContext, Inventory inventory) {
        var remainderMap = inventory.addItem(itemStack.clone());

        if (remainderMap.isEmpty()) {
            ItemStackStateChange stateChange = new ItemStackStateChange(itemStack.clone(), null);

            return ModificationResult.builder().activity(activityContext).applied().stateChange(stateChange).build();
        } else {
            var remainder = remainderMap.values().stream().findFirst();

            // If no items delivered, mark this as skipped
            if (remainder.get().getAmount() == itemStack.getAmount()) {
                return ModificationResult.builder()
                    .activity(activityContext)
                    .skipped()
                    .skipReason(ModificationSkipReason.FULL_INVENTORY)
                    .build();
            }

            // If some items delivered, mark this as partial
            var itemClone = itemStack.clone();
            itemClone.setAmount(itemClone.getAmount() - remainder.get().getAmount());

            ItemStackStateChange stateChange = new ItemStackStateChange(itemClone, null);

            return ModificationResult.builder()
                .activity(activityContext)
                .partial()
                .partialReason(ModificationPartialReason.FULL_INVENTORY)
                .stateChange(stateChange)
                .build();
        }
    }

    @Override
    public String toString() {
        return String.format("ItemStackAction{type=%s,material=%s,itemStack=%s}", type, material, itemStack);
    }
}
