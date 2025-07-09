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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.moonshine.placeholder.ConclusionValue;
import net.kyori.moonshine.placeholder.ContinuanceValue;
import net.kyori.moonshine.placeholder.IPlaceholderResolver;
import net.kyori.moonshine.util.Either;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.activities.AbstractActivity;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.Cause;
import org.prism_mc.prism.api.activities.GroupedActivity;
import org.prism_mc.prism.api.containers.PlayerContainer;
import org.prism_mc.prism.api.containers.StringContainer;
import org.prism_mc.prism.api.containers.TranslatableContainer;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.api.util.Pair;
import org.prism_mc.prism.bukkit.services.translation.BukkitTranslationService;

@Singleton
public class ActivityPlaceholderResolver implements IPlaceholderResolver<CommandSender, AbstractActivity, Component> {

    /**
     * The translation service.
     */
    private final BukkitTranslationService translationService;

    /**
     * Construct an activity placeholder resolver.
     *
     * @param translationService The translation service
     */
    @Inject
    public ActivityPlaceholderResolver(BukkitTranslationService translationService) {
        this.translationService = translationService;
    }

    @Override
    public @NonNull Map<String, Either<ConclusionValue<? extends Component>, ContinuanceValue<?>>> resolve(
        final String placeholderName,
        final AbstractActivity value,
        final CommandSender receiver,
        final Type owner,
        final Method method,
        final @Nullable Object[] parameters
    ) {
        Component action = Component.text(value.action().type().key());
        Component actionPastTense = actionPastTense(value.action().type());
        Component cause = cause(receiver, value.cause());
        Component since = since(receiver, value.timestamp());
        Component descriptor = descriptor(receiver, value);
        Component location = location(receiver, value.world(), value.coordinate());

        Component count = Component.text("1");
        if (value instanceof GroupedActivity grouped) {
            count = Component.text(grouped.count());
        }

        Component activityId = Component.empty();
        if (value instanceof Activity activity) {
            activityId = Component.text(activity.primaryKey().toString());
        }

        Component sign;
        if (value.action().type().resultType().equals(ActionResultType.REMOVES)) {
            sign = Component.translatable("prism.sign-minus");
        } else {
            sign = Component.translatable("prism.sign-plus");
        }

        return Map.of(
            placeholderName + "_action",
            Either.left(ConclusionValue.conclusionValue(action)),
            placeholderName + "_action_past_tense",
            Either.left(ConclusionValue.conclusionValue(actionPastTense)),
            placeholderName + "_id",
            Either.left(ConclusionValue.conclusionValue(activityId)),
            placeholderName + "_cause",
            Either.left(ConclusionValue.conclusionValue(cause)),
            placeholderName + "_count",
            Either.left(ConclusionValue.conclusionValue(count)),
            placeholderName + "_sign",
            Either.left(ConclusionValue.conclusionValue(sign)),
            placeholderName + "_location",
            Either.left(ConclusionValue.conclusionValue(location)),
            placeholderName + "_since",
            Either.left(ConclusionValue.conclusionValue(since)),
            placeholderName + "_descriptor",
            Either.left(ConclusionValue.conclusionValue(descriptor))
        );
    }

    /**
     * Build the action past tense component.
     *
     * @param actionType The action type
     * @return The component
     */
    protected Component actionPastTense(ActionType actionType) {
        Component actionHover = Component.text()
            .append(Component.text("a:", NamedTextColor.GRAY))
            .append(Component.text(actionType.key(), TextColor.fromCSSHexString("#ffd782")))
            .append(Component.text(" or ", NamedTextColor.WHITE))
            .append(Component.text("a:", NamedTextColor.GRAY))
            .append(Component.text(actionType.familyKey(), TextColor.fromCSSHexString("#ffd782")))
            .build();

        return Component.text()
            .append(Component.translatable(actionType.pastTenseTranslationKey()))
            .hoverEvent(HoverEvent.showText(actionHover))
            .build();
    }

    /**
     * Convert the cause into a text string.
     *
     * @param cause The cause
     * @return The cause name/string
     */
    protected Component cause(CommandSender receiver, Cause cause) {
        if (cause == null) {
            return Component.translatable("prism.unknown-cause");
        } else if (cause.container() instanceof StringContainer stringContainer) {
            return Component.text().append(Component.text(stringContainer.value())).build();
        } else if (cause.container() instanceof PlayerContainer playerContainer) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerContainer.uuid());

            Component playerHeading = MiniMessage.miniMessage()
                .deserialize(translationService.messageOf(receiver, "prism.player-hover-header"));

            Component uuid = MiniMessage.miniMessage()
                .deserialize(translationService.messageOf(receiver, "prism.player-hover-uuid"));

            Component online = MiniMessage.miniMessage()
                .deserialize(translationService.messageOf(receiver, "prism.player-hover-online"));

            Component banned = MiniMessage.miniMessage()
                .deserialize(translationService.messageOf(receiver, "prism.player-hover-banned"));

            String yes = translationService.messageOf(receiver, "prism.player-hover-yes");
            String no = translationService.messageOf(receiver, "prism.player-hover-no");

            Component hover = Component.text()
                .append(playerHeading)
                .append(Component.text("\n"))
                .append(uuid)
                .append(Component.text(" "))
                .append(Component.text(playerContainer.uuid().toString(), NamedTextColor.WHITE))
                .append(Component.text("\n"))
                .append(online)
                .append(Component.text(" "))
                .append(Component.text(offlinePlayer.isOnline() ? yes : no, NamedTextColor.WHITE))
                .append(Component.text("\n"))
                .append(banned)
                .append(Component.text(" "))
                .append(Component.text(offlinePlayer.isBanned() ? yes : no, NamedTextColor.WHITE))
                .build();

            return Component.text()
                .append(Component.text(playerContainer.name()))
                .hoverEvent(HoverEvent.showText(hover))
                .build();
        } else if (cause.container() instanceof TranslatableContainer translatableContainer) {
            return Component.translatable(translatableContainer.translationKey());
        } else {
            return Component.translatable("prism.unknown-cause");
        }
    }

    /**
     * Get the descriptor.
     *
     * @param receiver The receiver
     * @param value The activity
     * @return The descriptor component
     */
    protected Component descriptor(CommandSender receiver, AbstractActivity value) {
        if (!value.action().type().usesDescriptor()) {
            return Component.empty();
        }

        var builder = Component.text().append(value.action().descriptorComponent());

        if (
            value.action().metadata() != null &&
            value.action().metadata().data() != null &&
            !value.action().metadata().data().isEmpty()
        ) {
            var metadataBuilder = Component.text();

            int size = value.action().metadata().data().entrySet().size();
            int i = 0;
            for (var entry : value.action().metadata().data().entrySet()) {
                var key = translationService.messageOf(
                    receiver,
                    String.format("prism.metadata-hover-%s", entry.getKey().toLowerCase(Locale.ROOT))
                );

                metadataBuilder
                    .append(Component.text(key + ": ", NamedTextColor.GRAY))
                    .append(Component.text(entry.getValue(), NamedTextColor.WHITE));

                if (i < size - 1) {
                    metadataBuilder.appendNewline();
                }

                i++;
            }

            builder.hoverEvent(HoverEvent.showText(metadataBuilder.build()));
        }

        return builder.build();
    }

    /**
     * Get the location.
     *
     * @param receiver The receiver
     * @param world The world
     * @param coordinate The coordinate
     * @return The location
     */
    protected Component location(CommandSender receiver, Pair<UUID, String> world, Coordinate coordinate) {
        Component hover = Component.text(translationService.messageOf(receiver, "prism.click-to-teleport"));

        var builder = Component.text()
            .append(Component.text(coordinate.intX()))
            .append(Component.text(" "))
            .append(Component.text(coordinate.intY()))
            .append(Component.text(" "))
            .append(Component.text(coordinate.intZ()))
            .hoverEvent(HoverEvent.showText(hover));

        if (receiver instanceof Player player) {
            var command = String.format(
                "/prism teleport loc %s %s %d %d %d",
                player.getName(),
                world.value(),
                coordinate.intX(),
                coordinate.intY(),
                coordinate.intZ()
            );
            builder.clickEvent(ClickEvent.runCommand(command));
        }

        return builder.build();
    }

    /**
     * Get the shorthand syntax for time since.
     *
     * @param receiver The receiver
     * @param timestamp The timestamp
     * @return The time since
     */
    protected Component since(CommandSender receiver, long timestamp) {
        long diffInSeconds = System.currentTimeMillis() / 1000 - timestamp;

        if (diffInSeconds < 60) {
            return Component.translatable("prism.just-now");
        }

        long period = 24 * 60 * 60;

        final long[] diff = {
            diffInSeconds / period,
            (diffInSeconds / (period /= 24)) % 24,
            (diffInSeconds / (period / 60)) % 60,
        };

        StringBuilder timeAgo = new StringBuilder();

        if (diff[0] > 0) {
            timeAgo.append(diff[0]).append('d');
        }

        if (diff[1] > 0) {
            timeAgo.append(diff[1]).append('h');
        }

        if (diff[2] > 0) {
            timeAgo.append(diff[2]).append('m');
        }

        return Component.translatable(
            "prism.time-ago",
            Argument.component("time_ago", Component.text(timeAgo.toString()))
        );
    }
}
