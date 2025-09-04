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

package org.prism_mc.prism.paper.api.containers;

import java.util.Locale;
import lombok.Getter;
import org.bukkit.entity.EntityType;
import org.prism_mc.prism.api.containers.EntityContainer;

public class PaperEntityContainer implements EntityContainer {

    /**
     * The entity type.
     */
    @Getter
    private final EntityType entityType;

    /**
     * The translation key.
     */
    @Getter
    private final String translationKey;

    /**
     * Constructor.
     *
     * @param entityType The entity type
     */
    public PaperEntityContainer(EntityType entityType) {
        this.entityType = entityType;
        this.translationKey = entityType.translationKey();
    }

    @Override
    public String serializeEntityType() {
        return entityType.toString().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String toString() {
        return String.format("EntityContainer{entityType=%s,translationKey=%s}", entityType, translationKey);
    }
}
