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

package org.prism_mc.prism.api;

import org.prism_mc.prism.api.actions.ActionFactory;
import org.prism_mc.prism.api.actions.types.ActionTypeRegistry;
import org.prism_mc.prism.api.services.recording.RecordingService;
import org.prism_mc.prism.api.storage.StorageAdapter;

public interface Prism {
    /**
     * Get the action factory.
     *
     * @return The action factory
     */
    ActionFactory actionFactory();

    /**
     * Get the action type registry.
     *
     * @return The action type registry
     */
    ActionTypeRegistry actionTypeRegistry();

    /**
     * Get the recording service.
     *
     * @return The recording service
     */
    RecordingService recordingService();

    /**
     * Get the storage adapter.
     *
     * @return Storage adapter
     */
    StorageAdapter storageAdapter();
}
