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

package org.prism_mc.prism.bukkit.services.messages.resolvers;

import static org.prism_mc.prism.bukkit.api.activities.BukkitActivity.enumNameToString;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.moonshine.placeholder.ConclusionValue;
import net.kyori.moonshine.placeholder.ContinuanceValue;
import net.kyori.moonshine.placeholder.IPlaceholderResolver;
import net.kyori.moonshine.util.Either;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationResultStatus;

public class ModificationResultPlaceholderResolver
    implements IPlaceholderResolver<CommandSender, ModificationResult, Component> {

    @Override
    public @NonNull Map<String, Either<ConclusionValue<? extends Component>, ContinuanceValue<?>>> resolve(
        final String placeholderName,
        final ModificationResult value,
        final CommandSender receiver,
        final Type owner,
        final Method method,
        final @Nullable Object[] parameters
    ) {
        Component reason = reason(value);
        Component target = target(value);

        return Map.of(
            placeholderName + "_reason",
            Either.left(ConclusionValue.conclusionValue(reason)),
            placeholderName + "_target",
            Either.left(ConclusionValue.conclusionValue(target))
        );
    }

    /**
     * Format the reason.
     *
     * @param result The result
     * @return Component
     */
    protected Component reason(ModificationResult result) {
        if (result.status().equals(ModificationResultStatus.PARTIAL)) {
            return Component.text(enumNameToString(result.partialReason().toString()));
        } else if (result.status().equals(ModificationResultStatus.SKIPPED)) {
            return Component.text(enumNameToString(result.skipReason().toString()));
        }

        return Component.empty();
    }

    /**
     * Get the target.
     *
     * @param result The result
     * @return Component
     */
    protected Component target(ModificationResult result) {
        if (result.target() != null) {
            return Component.translatable(result.target());
        } else if (result.activity().action().descriptor() != null) {
            return Component.text(result.activity().action().descriptor());
        } else {
            return Component.text("-");
        }
    }
}
