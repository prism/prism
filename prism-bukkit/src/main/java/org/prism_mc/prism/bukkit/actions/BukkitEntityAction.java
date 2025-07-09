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
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.actions.EntityAction;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.bukkit.PrismBukkit;
import org.prism_mc.prism.bukkit.api.containers.BukkitEntityContainer;
import org.prism_mc.prism.bukkit.services.nbt.NbtService;

public class BukkitEntityAction extends BukkitAction implements EntityAction {

    @Getter
    private final BukkitEntityContainer entityContainer;

    /**
     * The read/write nbt.
     */
    private final ReadWriteNBT readWriteNbt;

    /**
     * Construct a new entity action.
     *
     * @param type The action type
     * @param entity The entity
     * @param metadata The metadata
     */
    public BukkitEntityAction(ActionType type, Entity entity, Metadata metadata) {
        this(type, entity);
        this.metadata = metadata;
    }

    /**
     * Construct a new entity action.
     *
     * @param type The action type
     * @param entity The entity
     */
    public BukkitEntityAction(ActionType type, Entity entity) {
        super(type);
        this.entityContainer = new BukkitEntityContainer(entity.getType());

        readWriteNbt = NBT.createNBTObject();

        var nbtService = PrismBukkit.instance().injectorProvider().injector().getInstance(NbtService.class);
        nbtService.processEntityNbt(entity, readWriteNbt::mergeCompound);
    }

    /**
     * Construct a new entity action with the type and nbt container.
     *
     * @param type The action type
     * @param entityType The entity type
     * @param readWriteNbt The read/write nbt
     * @param metadata The metadata
     */
    public BukkitEntityAction(ActionType type, EntityType entityType, ReadWriteNBT readWriteNbt, Metadata metadata) {
        super(type, null, metadata);
        this.entityContainer = new BukkitEntityContainer(entityType);
        this.readWriteNbt = readWriteNbt;
    }

    @Override
    public Component descriptorComponent() {
        return Component.translatable(entityContainer.translationKey());
    }

    @Override
    public boolean hasCustomData() {
        return this.readWriteNbt != null;
    }

    @Override
    public @Nullable String serializeCustomData() {
        return readWriteNbt.toString();
    }

    @Override
    public ModificationResult applyRollback(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    ) {
        return modifyEntity(true, modificationRuleset, owner, activityContext, mode);
    }

    @Override
    public ModificationResult applyRestore(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    ) {
        return modifyEntity(false, modificationRuleset, owner, activityContext, mode);
    }

    protected ModificationResult modifyEntity(
        boolean isRollback,
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    ) {
        // Skip if entity is in the blacklist
        if (modificationRuleset.entityBlacklistContainsAny(entityContainer.entityType().toString())) {
            return ModificationResult.builder()
                .activity(activityContext)
                .skipped()
                .target(entityContainer.translationKey())
                .build();
        }

        Coordinate coordinate = activityContext.coordinate();
        World world = Bukkit.getServer().getWorld(activityContext.worldUuid());
        if (world == null) {
            return ModificationResult.builder()
                .activity(activityContext)
                .skipped()
                .target(entityContainer.translationKey())
                .build();
        }

        boolean shouldSpawn =
            (isRollback && type().resultType().equals(ActionResultType.REMOVES)) ||
            (!isRollback && type().resultType().equals(ActionResultType.CREATES));

        if (shouldSpawn) {
            if (entityContainer.entityType().getEntityClass() != null) {
                Location loc = new Location(world, coordinate.x(), coordinate.y(), coordinate.z());

                world.spawn(loc, entityContainer.entityType().getEntityClass(), entity -> {
                    NBT.modify(entity, nbt -> {
                        nbt.mergeCompound(readWriteNbt);
                    });
                });

                return ModificationResult.builder().activity(activityContext).applied().build();
            }
        } else {
            UUID uuid = readWriteNbt.getUUID("UUID");
            if (uuid != null) {
                Entity entity = world.getEntity(uuid);
                if (entity != null) {
                    if (type().resultType().equals(ActionResultType.CREATES)) {
                        entity.remove();

                        return ModificationResult.builder().activity(activityContext).applied().build();
                    } else if (type().resultType().equals(ActionResultType.REPLACES)) {
                        NBT.modify(entity, nbt -> {
                            nbt.mergeCompound(readWriteNbt);
                        });

                        return ModificationResult.builder().activity(activityContext).applied().build();
                    }
                }
            }
        }

        return ModificationResult.builder()
            .activity(activityContext)
            .skipped()
            .target(entityContainer.translationKey())
            .build();
    }

    @Override
    public String toString() {
        return String.format("EntityAction{type=%s}", type);
    }
}
