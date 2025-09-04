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

import com.google.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.moonshine.placeholder.ConclusionValue;
import net.kyori.moonshine.placeholder.ContinuanceValue;
import net.kyori.moonshine.placeholder.IPlaceholderResolver;
import net.kyori.moonshine.util.Either;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.paper.services.translation.PaperTranslationService;

public class ModificationQueueResultPlaceholderResolver
    implements IPlaceholderResolver<CommandSender, ModificationQueueResult, Component> {

    /**
     * The translation service.
     */
    private final PaperTranslationService translationService;

    /**
     * Construct the resolver.
     *
     * @param translationService The translation service
     */
    @Inject
    public ModificationQueueResultPlaceholderResolver(PaperTranslationService translationService) {
        this.translationService = translationService;
    }

    @Override
    public @NonNull Map<String, Either<ConclusionValue<? extends Component>, ContinuanceValue<?>>> resolve(
        final String placeholderName,
        final ModificationQueueResult value,
        final CommandSender receiver,
        final Type owner,
        final Method method,
        final @Nullable Object[] parameters
    ) {
        Component skipped = skipped(receiver, value.skipped());
        Component partial = partial(receiver, value.partial());

        return Map.of(
            placeholderName + "_partial",
            Either.left(ConclusionValue.conclusionValue(partial)),
            placeholderName + "_skipped",
            Either.left(ConclusionValue.conclusionValue(skipped))
        );
    }

    /**
     * Format the partial count.
     *
     * @param receiver The receiver
     * @param partial The partial
     * @return Component
     */
    protected Component partial(CommandSender receiver, int partial) {
        var builder = Component.text().append(Component.text(partial));

        if (receiver instanceof Player) {
            builder
                .hoverEvent(
                    HoverEvent.showText(
                        Component.text(translationService.messageOf(receiver, "prism.click-to-view-partial"))
                    )
                )
                .clickEvent(ClickEvent.runCommand("/prism report partial"));
        }

        return builder.build();
    }

    /**
     * Format the skipped count.
     *
     * @param receiver The receiver
     * @param skipped The skipped
     * @return Component
     */
    protected Component skipped(CommandSender receiver, int skipped) {
        var builder = Component.text().append(Component.text(skipped));

        if (receiver instanceof Player) {
            builder
                .hoverEvent(
                    HoverEvent.showText(
                        Component.text(translationService.messageOf(receiver, "prism.click-to-view-skips"))
                    )
                )
                .clickEvent(ClickEvent.runCommand("/prism report skips"));
        }

        return builder.build();
    }
}
