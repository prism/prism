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

package network.darkhelmet.prism.actions;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;

import java.util.Locale;
import java.util.UUID;

import network.darkhelmet.prism.api.actions.IEntityAction;
import network.darkhelmet.prism.api.actions.types.ActionResultType;
import network.darkhelmet.prism.api.actions.types.IActionType;
import network.darkhelmet.prism.api.activities.IActivity;
import network.darkhelmet.prism.api.services.modifications.ModificationQueueMode;
import network.darkhelmet.prism.api.services.modifications.ModificationResult;
import network.darkhelmet.prism.api.services.modifications.ModificationRuleset;
import network.darkhelmet.prism.api.util.WorldCoordinate;
import network.darkhelmet.prism.utils.LocationUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class EntityAction extends Action implements IEntityAction {
    /**
     * The read/write nbt.
     */
    private final ReadWriteNBT readWriteNbt;

    /**
     * The entity type.
     */
    private final EntityType entityType;

    /**
     * Construct a new entity action.
     *
     * @param type The action type
     * @param entity The entity
     */
    public EntityAction(IActionType type, Entity entity) {
        super(type);

        this.entityType = entity.getType();

        readWriteNbt = NBT.createNBTObject();
        NBT.get(entity, readWriteNbt::mergeCompound);

        // Strip some data we don't want to track/rollback.
        String[] rejects = {
            "DeathTime",
            "Fire",
            "Health",
            "HurtByTimestamp",
            "HurtTime",
            "OnGround",
            "Pos",
            "WorldUUIDLeast",
            "WorldUUIDMost"
        };

        for (String reject : rejects) {
            readWriteNbt.removeKey(reject);
        }

        this.descriptor = entityType.toString().toLowerCase(Locale.ENGLISH).replace("_", " ");
    }

    /**
     * Construct a new entity action with the type and nbt container.
     *
     * @param type The action type
     * @param entityType The entity type
     * @param readWriteNbt The read/write nbt
     * @param descriptor The descriptor
     */
    public EntityAction(IActionType type, EntityType entityType, ReadWriteNBT readWriteNbt, String descriptor) {
        super(type, descriptor, null);

        this.entityType = entityType;
        this.readWriteNbt = readWriteNbt;
    }

    /**
     * Get the entity type.
     *
     * @return Entity type
     */
    public EntityType entityType() {
        return entityType;
    }

    @Override
    public String serializeEntityType() {
        return entityType.toString().toLowerCase(Locale.ENGLISH);
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
            IActivity activityContext,
            ModificationQueueMode mode) {
        // Skip if entity is in the blacklist
        if (modificationRuleset.entityBlacklistContainsAny(entityType.toString())) {
            return ModificationResult.builder().activity(activityContext).build();
        }

        WorldCoordinate coordinate = activityContext.location();
        World world = Bukkit.getServer().getWorld(coordinate.world().uuid());
        if (world == null) {
            return ModificationResult.builder().activity(activityContext).build();
        }

        if (type().resultType().equals(ActionResultType.REMOVES)) {
            if (entityType.getEntityClass() != null) {
                Location loc = LocationUtils.worldCoordToLocation(coordinate);

                world.spawn(loc, entityType.getEntityClass(), entity -> {
                    NBT.modify(entity, nbt -> {
                        nbt.mergeCompound(readWriteNbt);
                    });
                });

                return ModificationResult.builder().activity(activityContext).applied().build();
            }
        } else {
            UUID uuid = readWriteNbt.getUUID("UUID");
            if (uuid != null) {
                for (var entity : world.getEntities()) {
                    if (entity.getUniqueId().equals(uuid)) {
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
        }

        return ModificationResult.builder().activity(activityContext).build();
    }

    @Override
    public ModificationResult applyRestore(
            ModificationRuleset modificationRuleset,
            Object owner,
            IActivity activityContext,
            ModificationQueueMode mode) {
        return ModificationResult.builder().activity(activityContext).build();
    }

    @Override
    public String toString() {
        return String.format("EntityAction{type=%s}", type);
    }
}
