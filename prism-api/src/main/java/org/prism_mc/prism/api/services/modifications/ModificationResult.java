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

package org.prism_mc.prism.api.services.modifications;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.prism_mc.prism.api.activities.Activity;

@Builder
@Getter
public final class ModificationResult {

    /**
     * The original activity.
     */
    @NonNull
    private final Activity activity;

    /**
     * The reason for a partial modification. Unused if status != PARTIAL.
     */
    @Builder.Default
    private final ModificationPartialReason partialReason = ModificationPartialReason.BLACKLISTED;

    /**
     * The state changes.
     */
    private final StateChange<?> stateChange;

    /**
     * The modification result status.
     */
    @NonNull
    @Builder.Default
    private final ModificationResultStatus status = ModificationResultStatus.SKIPPED;

    /**
     * The reason for a skip. Unused if status != SKIPPED.
     */
    @Builder.Default
    private final ModificationSkipReason skipReason = ModificationSkipReason.NOT_IMPLEMENTED;

    /**
     * The target of the result if result != applied.
     */
    private final String target;

    public static class ModificationResultBuilder {

        /**
         * Set the status to APPLIED.
         *
         * @return The builder
         */
        public ModificationResultBuilder applied() {
            status(ModificationResultStatus.APPLIED);
            return this;
        }

        /**
         * Set the status to ERRORED.
         *
         * @return The builder
         */
        public ModificationResultBuilder errored() {
            status(ModificationResultStatus.ERRORED);
            return this;
        }

        /**
         * Set the status to PARTIAL.
         *
         * @return The builder
         */
        public ModificationResultBuilder partial() {
            status(ModificationResultStatus.PARTIAL);
            return this;
        }

        /**
         * Set the status to PLANNED.
         *
         * @return The builder
         */
        public ModificationResultBuilder planned() {
            status(ModificationResultStatus.PLANNED);
            return this;
        }

        /**
         * Set the status to SKIPPED.
         *
         * @return The builder
         */
        public ModificationResultBuilder skipped() {
            status(ModificationResultStatus.SKIPPED);
            return this;
        }

        /**
         * Set the status based on the mode.
         *
         * <p>Planning mode = Planned status
         * Completing mode = Applied status
         * Undecided mode = Skipped status</p>
         *
         * @param mode The mode
         * @return The builder
         */
        public ModificationResultBuilder statusFromMode(ModificationQueueMode mode) {
            if (mode.equals(ModificationQueueMode.PLANNING)) {
                planned();
            } else if (mode.equals(ModificationQueueMode.COMPLETING)) {
                applied();
            } else {
                skipped();
            }

            return this;
        }
    }
}
