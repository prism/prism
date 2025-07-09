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

package org.prism_mc.prism.api.activities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.experimental.Tolerate;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.util.Coordinate;

@SuperBuilder(toBuilder = true)
@Getter
@ToString
public class ActivityQuery {

    /**
     * The action type keys.
     */
    @Singular
    private Collection<String> actionTypeKeys;

    /**
     * The action types.
     */
    @Singular
    private Collection<ActionType> actionTypes;

    /**
     * The activity ids.
     */
    private Collection<Integer> activityIds;

    /**
     * The lower-bound timestamp.
     */
    private Long after;

    /**
     * The upper-bound timestamp.
     */
    private Long before;

    /**
     * The affected blocks.
     */
    @Singular
    private Collection<String> affectedBlocks;

    /**
     * The cause blocks.
     */
    @Singular
    private Collection<String> causeBlocks;

    /**
     * The named cause.
     */
    private String namedCause;

    /**
     * The default parameters used.
     */
    @Singular("defaultUsed")
    private Collection<String> defaultsUsed;

    /**
     * The affected entity types.
     */
    @Singular
    private Collection<String> affectedEntityTypes;

    /**
     * The cause entity types.
     */
    @Singular
    private Collection<String> causeEntityTypes;

    /**
     * Grouped.
     */
    @Builder.Default
    private boolean grouped = true;

    /**
     * Limit the number of records.
     */
    private int limit;

    /**
     * The coordinate.
     */
    private Coordinate coordinate;

    /**
     * Is lookup.
     */
    @Builder.Default
    private boolean lookup = true;

    /**
     * The affected materials.
     */
    @Singular
    private Collection<String> affectedMaterials;

    /**
     * The max x coordinate.
     */
    private Coordinate maxCoordinate;

    /**
     * The min z coordinate.
     */
    private Coordinate minCoordinate;

    /**
     * The record index offset.
     */
    @Builder.Default
    private int offset = 0;

    /**
     * The affected player names.
     */
    @Singular
    private Collection<String> affectedPlayerNames;

    /**
     * The cause player names.
     */
    @Singular
    private Collection<String> causePlayerNames;

    /**
     * A generic query string for searching text.
     */
    private String query;

    /**
     * The reference coordinate.
     * If defined, this location will be used as the center for the Radius, In, and World parameters.
     */
    private Coordinate referenceCoordinate;

    /**
     * The reversed state.
     */
    private Boolean reversed;

    /**
     * The sort direction.
     */
    @Builder.Default
    private Sort sort = Sort.DESCENDING;

    /**
     * The world uuid.
     */
    private UUID worldUuid;

    /**
     * Describe the sort directions.
     */
    public enum Sort {
        ASCENDING,
        DESCENDING,
    }

    /**
     * Get whether this query is for a modifier.
     *
     * @return True if lookup and grouped are false
     */
    public boolean modification() {
        return !lookup && !grouped;
    }

    /**
     * Get all action type keys for this query.
     *
     * <p>This combines the keys of any defined action types,
     * and any action type keys defined as strings.</p>
     *
     * @return Set of keys
     */
    public Set<String> allActionTypeKeys() {
        var all = new HashSet<>(actionTypeKeys);

        for (var actionType : actionTypes()) {
            all.add(actionType.key());
        }

        return all;
    }

    public abstract static class ActivityQueryBuilder<C extends ActivityQuery, B extends ActivityQueryBuilder<C, B>> {

        /**
         * Add a single activity id.
         *
         * @param activityId Activity id
         * @return The builder
         */
        public B activityId(int activityId) {
            if (activityIds == null) {
                activityIds = new ArrayList<>();
            }

            activityIds.add(activityId);

            return self();
        }

        /**
         * Set the coordinate corners of a bounding box.
         *
         * @param minCoordinate The min coordinate
         * @param maxCoordinate The max coordinate
         * @return The builder
         */
        public B boundingCoordinates(Coordinate minCoordinate, Coordinate maxCoordinate) {
            this.minCoordinate = minCoordinate;
            this.maxCoordinate = maxCoordinate;

            return self();
        }

        /**
         * Set the coordinate.
         *
         * @param x The x coordinate
         * @param y The y coordinate
         * @param z The z coordinate
         * @return The builder
         */
        @Tolerate
        public B coordinate(double x, double y, double z) {
            this.coordinate = new Coordinate(x, y, z);

            return self();
        }

        /**
         * Use the reference coordinate as the search location.
         *
         * @return The builder
         */
        public B coordinateFromReferenceCoordinate() {
            this.coordinate = referenceCoordinate;

            return self();
        }

        /**
         * Indicate this query is for use with modifiers.
         *
         * <p>Sets lookup and grouped to false.</p>
         *
         * @return The builder
         */
        public B modification() {
            this.lookup(false);
            this.grouped(false);

            return self();
        }

        /**
         * Set the radius around the current reference coordinate.
         *
         * @param radius The radius
         * @return The builder
         */
        public B radius(int radius) {
            Coordinate minCoordinate = new Coordinate(
                referenceCoordinate.intX() - radius,
                referenceCoordinate.intY() - radius,
                referenceCoordinate.intZ() - radius
            );

            Coordinate maxCoordinate = new Coordinate(
                referenceCoordinate.intX() + radius,
                referenceCoordinate.intY() + radius,
                referenceCoordinate.intZ() + radius
            );

            this.boundingCoordinates(minCoordinate, maxCoordinate);

            return self();
        }

        /**
         * Indicate this query is for use with a restore modifier.
         *
         * @return The builder
         */
        public B restore() {
            this.lookup(false);
            this.grouped(false);
            this.sort(Sort.ASCENDING);

            return self();
        }

        /**
         * Indicate this query is for use with modifiers.
         *
         * @return The builder
         */
        public B rollback() {
            this.lookup(false);
            this.grouped(false);
            this.sort(Sort.ASCENDING);

            return self();
        }
    }
}
