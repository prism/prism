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

package org.prism_mc.prism.paper.services.modifications;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.List;
import java.util.function.Consumer;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.services.modifications.Rollback;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.messages.MessageService;

public class PaperRollback extends AbstractWorldModificationQueue implements Rollback {

    /**
     * Construct a new rollback.
     *
     * @param loggingService The logging service
     * @param configurationService The configuration service
     * @param messageService The message service
     * @param storageAdapter The storage adapter
     * @param modificationRuleset The ruleset
     * @param owner The owner
     * @param query The query used
     * @param modifications A list of modifications
     * @param onEnd The end callback
     */
    @Inject
    public PaperRollback(
        LoggingService loggingService,
        ConfigurationService configurationService,
        MessageService messageService,
        StorageAdapter storageAdapter,
        @Assisted ModificationRuleset modificationRuleset,
        @Assisted Object owner,
        @Assisted ActivityQuery query,
        @Assisted List<Activity> modifications,
        @Assisted Consumer<ModificationQueueResult> onEnd
    ) {
        super(
            loggingService,
            configurationService,
            messageService,
            storageAdapter,
            modificationRuleset,
            owner,
            query,
            modifications,
            onEnd
        );
    }

    @Override
    protected ModificationResult applyModification(Activity activity) {
        var handler = activity.action().type().modificationHandler();
        if (handler == null) {
            handler = activity.action();
        }

        return handler.applyRollback(modificationRuleset, owner(), activity, mode);
    }

    @Override
    protected boolean markReversedState() {
        return true;
    }

    @Override
    public void preview() {
        this.mode = ModificationQueueMode.PLANNING;
        execute();
    }
}
