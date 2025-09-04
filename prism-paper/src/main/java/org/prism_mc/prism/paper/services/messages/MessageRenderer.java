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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.moonshine.message.IMessageRenderer;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.paper.services.translation.PaperTranslationService;

@Singleton
public class MessageRenderer implements IMessageRenderer<CommandSender, String, Component, Component> {

    /**
     * The translation service.
     */
    private final PaperTranslationService translationService;

    /**
     * Construct the message renderer.
     *
     * @param translationService The translation service
     */
    @Inject
    public MessageRenderer(PaperTranslationService translationService) {
        this.translationService = translationService;
    }

    @Override
    public Component render(
        final CommandSender receiver,
        String intermediateMessage,
        final Map<String, ? extends Component> resolvedPlaceholders,
        final Method method,
        final Type owner
    ) {
        TagResolver.Single headingTemplate = Placeholder.parsed(
            "prefix",
            translationService.messageOf(receiver, "prism.prefix")
        );

        TagResolver.Builder builder = TagResolver.builder();
        builder.resolver(headingTemplate);
        for (final var entry : resolvedPlaceholders.entrySet()) {
            if (entry.getKey().contains("color")) {
                intermediateMessage = intermediateMessage.replaceAll(
                    entry.getKey(),
                    entry.getValue().color().asHexString()
                );
            } else {
                builder.resolver(Placeholder.component(entry.getKey(), entry.getValue()));
            }
        }

        return MiniMessage.miniMessage().deserialize(intermediateMessage, builder.build());
    }
}
