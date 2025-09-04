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

package org.prism_mc.prism.paper.services.messages.resolvers;

import com.google.inject.Singleton;
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
import org.prism_mc.prism.api.services.purges.PurgeCycleResult;

@Singleton
public class PurgeCycleResultPlaceholderResolver
    implements IPlaceholderResolver<CommandSender, PurgeCycleResult, Component> {

    @Override
    public @NonNull Map<String, Either<ConclusionValue<? extends Component>, ContinuanceValue<?>>> resolve(
        final String placeholderName,
        final PurgeCycleResult value,
        final CommandSender receiver,
        final Type owner,
        final Method method,
        final @Nullable Object[] parameters
    ) {
        Component count = Component.text(value.deleted());
        Component lowerbound = Component.text(value.minPrimaryKey());
        Component upperbound = Component.text(value.maxPrimaryKey());

        return Map.of(
            placeholderName + "_count",
            Either.left(ConclusionValue.conclusionValue(count)),
            placeholderName + "_lowerbound",
            Either.left(ConclusionValue.conclusionValue(lowerbound)),
            placeholderName + "_upperbound",
            Either.left(ConclusionValue.conclusionValue(upperbound))
        );
    }
}
