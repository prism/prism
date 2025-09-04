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

package org.prism_mc.prism.paper.services.messages;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import net.kyori.moonshine.receiver.IReceiverLocator;
import net.kyori.moonshine.receiver.IReceiverLocatorResolver;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ReceiverResolver implements IReceiverLocatorResolver<CommandSender> {

    @Override
    public IReceiverLocator<CommandSender> resolve(final Method method, final Type proxy) {
        return new Resolver();
    }

    private static final class Resolver implements IReceiverLocator<CommandSender> {

        @Override
        public CommandSender locate(final Method method, final Object proxy, final @Nullable Object[] parameters) {
            if (parameters.length == 0) {
                return null;
            }

            final Object parameter = parameters[0];
            if (parameter instanceof CommandSender sender) {
                return sender;
            }

            return null;
        }
    }
}
