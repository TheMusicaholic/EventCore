package me.david.util;

import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.Getter;
import me.david.EventCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Getter
public class UpdateChecker {

    private static final long RECHECK_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);

    @Getter
    private final JavaPlugin plugin;
    private final String apiUrl;
    private final String currentVer;

    // Written by the checker thread, read by the server threads.
    private volatile String latestVer;
    private volatile boolean hasUpdate;
    private volatile String downloadUrl;

    @Getter(AccessLevel.NONE)
    private long lastCheckMillis;

    @SuppressWarnings("deprecation")
    public UpdateChecker(JavaPlugin plugin, String owner, String repo) {
        this.plugin = plugin;
        this.apiUrl = String.format("https://api.github.com/repos/%s/%s/releases/latest", owner, repo);
        this.currentVer = normalize(plugin.getDescription().getVersion());
    }

    /**
     * Fetches the latest release in the background. Calls within an hour of the previous check are ignored,
     * so this is cheap enough to call on every join instead of querying the GitHub API each time.
     */
    @SuppressWarnings("deprecation")
    public synchronized void check() {
        final long now = System.currentTimeMillis();
        if (lastCheckMillis != 0 && now - lastCheckMillis < RECHECK_INTERVAL_MILLIS) return;
        lastCheckMillis = now;

        Thread.ofVirtual().start(() -> {
            try {
                var connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() != 200) {
                    plugin.getLogger().warning("Update check failed: HTTP " + connection.getResponseCode());
                    return;
                }

                try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    var json = JsonParser.parseReader(reader).getAsJsonObject();
                    latestVer = normalize(json.get("tag_name").getAsString());
                    downloadUrl = json.get("html_url").getAsString();
                    hasUpdate = compare(currentVer, latestVer) < 0;

                    // The logger is thread-safe, so there's no need to hop to the main thread
                    // (Bukkit.getScheduler() isn't available on Folia anyway).
                    if (EventCore.getInstance().getConfig().getBoolean("Settings.Updates.LogInConsole")) {
                        log();
                    }
                }
            } catch (Exception exception) {
                EventCore.LOGGER.warn("Failed to check for updates: {}", exception.getMessage());
            }
        });
    }

    private void log() {
        if (hasUpdate) {
            EventCore.LOGGER.info(" ");
            EventCore.LOGGER.info("New version available for EventCore!");
            EventCore.LOGGER.info("Current: {}", currentVer);
            EventCore.LOGGER.info("Latest: {}", latestVer);
            EventCore.LOGGER.info("Download: {}", downloadUrl);
            EventCore.LOGGER.info(" ");
        } else {
            EventCore.LOGGER.info("No updates available for EventCore");
        }
    }

    public @NotNull Component getUpdateComponent() {
        if (downloadUrl == null) {
            return Component.text("Update check in progress...", NamedTextColor.GRAY);
        }

        return Component.text("Click to download", NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                .hoverEvent(HoverEvent.showText(Component.text("Click to Open", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.openUrl(downloadUrl));

    }

    private String normalize(String v) {
        if (v == null) return "0.0.0";

        v = v.replaceFirst("^.*?-[vV]?", "");
        v = v.replaceFirst("^[vV]", "");

        return v.isEmpty() ? "0.0.0" : v;
    }

    private int compare(String current, String latest) {
        var curr = current.split("\\.");
        var last = latest.split("\\.");

        for (int i = 0; i < Math.max(curr.length, last.length); i++) {
            int c = i < curr.length ? parse(curr[i]) : 0;
            int l = i < last.length ? parse(last[i]) : 0;
            if (c != l) return Integer.compare(c, l);
        }
        return 0;
    }

    private int parse(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}