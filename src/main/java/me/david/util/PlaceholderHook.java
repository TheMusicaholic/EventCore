package me.david.util;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.david.EventCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;

public final class PlaceholderHook extends PlaceholderExpansion {

    // DecimalFormat isn't thread-safe, and placeholders are requested from several threads at once (async
    // scoreboards, region threads on Folia), so each thread gets its own.
    private static final ThreadLocal<DecimalFormat> KD_FORMAT = ThreadLocal.withInitial(() -> new DecimalFormat("#0.00"));

    @Override
    public @NotNull String getIdentifier() {
        return "eventcore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "VertrauterDavid & JavaMio";
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull String getVersion() {
        return EventCore.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return List.of(
                "total",
                "alive",
                "kills",
                "deaths",
                "kd",
                "totems",
                "border",
                "ping",
                "tps"
        );
    }

    @Override
    public @NotNull String onPlaceholderRequest(final Player player, final @NotNull String params) {
        if (player == null) return "";

        return switch (params) {
            case "total" -> String.valueOf(PlayerUtil.getTotal());
            case "alive" -> String.valueOf(PlayerUtil.getAlive());
            case "kills" -> String.valueOf(player.getStatistic(Statistic.PLAYER_KILLS));
            case "deaths" -> String.valueOf(player.getStatistic(Statistic.DEATHS));
            case "kd" -> formatKD(player);
            case "totems" -> String.valueOf(countTotems(player));
            case "border" -> String.valueOf((int) (player.getWorld().getWorldBorder().getSize() / 2));
            case "ping" -> String.valueOf(player.isOnline() ? (int) (player.getPing() * 0.8) : 0);
            case "tps" -> formatTPS();
            default -> "";
        };
    }

    @Contract(pure = true)
    private static @NotNull String formatKD(final @NotNull Player player) {
        final double kills = player.getStatistic(Statistic.PLAYER_KILLS);
        final double deaths = player.getStatistic(Statistic.DEATHS);
        final double ratio = deaths == 0 ? kills : kills / deaths;
        return KD_FORMAT.get().format(Math.max(0, ratio));
    }

    private static int countTotems(final @NotNull Player player) {
        int totems = 0;
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) totems++;
        }
        return totems;
    }

    @SuppressWarnings("deprecation")
    private static @NotNull String formatTPS() {
        final String raw = PlaceholderAPI.setPlaceholders(null, "%spark_tps_5m%");
        return ChatColor.stripColor(raw.replace("*", "").split("\\.")[0]);
    }
}
