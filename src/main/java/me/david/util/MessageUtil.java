package me.david.util;

import lombok.experimental.UtilityClass;
import me.david.EventCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@UtilityClass
public class MessageUtil {

    private final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // Translated config messages. Swapped for a fresh map on config reload, so a lookup racing
    // the reload can only ever write into the discarded map.
    private volatile Map<String, Component> cache = new ConcurrentHashMap<>();

    // Patterns matching any placeholder of a set, by set. The code only uses a handful of different sets.
    private final Map<Set<String>, Pattern> PLACEHOLDER_PATTERNS = new ConcurrentHashMap<>();

    public Component get(@NotNull String key) {
        return cache.computeIfAbsent(key, k -> translateColorCodes(EventCore.getInstance().getConfig().getString(k, "")));
    }

    public Component getPrefix() {
        return get("Messages.Prefix");
    }

    public void clearCache() {
        cache = new ConcurrentHashMap<>();
    }

    public @NotNull Component format(@NotNull String key, @NotNull Map<String, ? extends ComponentLike> replacements) {
        final Component component = get(key);
        if (replacements.isEmpty()) return component;

        // Replaces all placeholders in a single pass over the component, instead of one pass (with a freshly
        // compiled pattern) per placeholder.
        return component.replaceText(TextReplacementConfig.builder()
                .match(placeholderPattern(replacements.keySet()))
                .replacement((match, original) -> replacements.get(match.group()))
                .build());
    }

    private Pattern placeholderPattern(@NotNull Set<String> placeholders) {
        Pattern pattern = PLACEHOLDER_PATTERNS.get(placeholders);
        if (pattern == null) {
            final StringJoiner alternatives = new StringJoiner("|");
            for (String placeholder : placeholders) {
                alternatives.add(Pattern.quote(placeholder));
            }
            pattern = Pattern.compile(alternatives.toString());
            PLACEHOLDER_PATTERNS.put(Set.copyOf(placeholders), pattern);
        }
        return pattern;
    }

    @NotNull
    public Component translateColorCodes(@NotNull String message) {
        if (message.indexOf('&') >= 0) {
            message = LEGACY_COLOR_PATTERN.matcher(message).replaceAll("§$1");
            message = HEX_COLOR_PATTERN.matcher(message).replaceAll(match -> {
                final String hex = match.group(1);
                final StringBuilder builder = new StringBuilder(14).append("§x");
                for (int i = 0; i < hex.length(); i++) {
                    builder.append('§').append(hex.charAt(i));
                }
                return builder.toString();
            });
        }
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }

}
