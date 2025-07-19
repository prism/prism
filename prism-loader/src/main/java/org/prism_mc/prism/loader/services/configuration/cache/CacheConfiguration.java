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

package org.prism_mc.prism.loader.services.configuration.cache;

import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class CacheConfiguration {

    @Comment("Enable stats recording. Ideally not used on live servers.")
    private boolean recordStats = false;

    @Comment("Cache settings for alerts.")
    private CacheBuilderConfiguration alerts = new CacheBuilderConfiguration(
        1000,
        new DurationConfiguration(5, TimeUnit.MINUTES)
    );

    @Comment("Cache settings for alerted vein locations.")
    private CacheBuilderConfiguration alertedLocations = new CacheBuilderConfiguration(
        1000,
        new DurationConfiguration(5, TimeUnit.MINUTES)
    );

    @Comment("Activity queries (lookups) are cached so that they can be re-used or paginated.")
    private CacheBuilderConfiguration lookupExpiration = new CacheBuilderConfiguration(
        3,
        new DurationConfiguration(5, TimeUnit.MINUTES)
    );

    @Comment("Cache settings for default entity nbt data.")
    private CacheBuilderConfiguration nbtEntityDefaults = new CacheBuilderConfiguration(
        200,
        new DurationConfiguration(15, TimeUnit.MINUTES)
    );

    @Comment("Cache settings for action key/primary keys.")
    private CacheBuilderConfiguration pkCacheActionKey = new CacheBuilderConfiguration(100);

    @Comment("Cache settings for block data/primary keys.")
    private CacheBuilderConfiguration pkCacheBlockData = new CacheBuilderConfiguration(
        500,
        new DurationConfiguration(15, TimeUnit.MINUTES)
    );

    @Comment("Cache settings for entity types/primary keys.")
    private CacheBuilderConfiguration pkCacheEntityType = new CacheBuilderConfiguration(200);

    @Comment("Cache settings for item data/primary keys.")
    private CacheBuilderConfiguration pkCacheItemData = new CacheBuilderConfiguration(
        1000,
        new DurationConfiguration(15, TimeUnit.MINUTES)
    );

    @Comment("Cache settings for named causes/primary keys.")
    private CacheBuilderConfiguration pkCacheNamedCause = new CacheBuilderConfiguration(
        200,
        new DurationConfiguration(15, TimeUnit.MINUTES)
    );

    @Comment("Cache settings for players/primary keys.")
    private CacheBuilderConfiguration pkCachePlayer = new CacheBuilderConfiguration(
        100,
        new DurationConfiguration(15, TimeUnit.MINUTES)
    );

    @Comment("Cache settings for world/primary keys.")
    private CacheBuilderConfiguration pkCacheWorld = new CacheBuilderConfiguration(20);
}
