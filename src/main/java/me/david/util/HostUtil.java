package me.david.util;

import lombok.experimental.UtilityClass;
import me.david.EventCore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@UtilityClass
public class HostUtil {

    // There is at most one host. Kept as a UUID so no Player object outlives its session, and atomic because
    // joins and quits run on different region threads on Folia.
    private final AtomicReference<UUID> host = new AtomicReference<>();

    public boolean isHost(final @NotNull Player player) {
        return player.getUniqueId().equals(host.get());
    }

    public void giveHost(final @NotNull Player player) {
        final Settings settings = EventCore.getInstance().getSettings();
        if (!settings.isHostRankEnabled() || !player.hasPermission(settings.getHostRankPermission())) return;
        if (!host.compareAndSet(null, player.getUniqueId())) return;

        dispatch(settings.getHostRankJoinCommand(), player);
    }

    public void removeHost(final @NotNull Player player) {
        // Always free the slot, even if the host rank was disabled by a reload in the meantime.
        if (!host.compareAndSet(player.getUniqueId(), null)) return;

        final Settings settings = EventCore.getInstance().getSettings();
        if (settings.isHostRankEnabled() && player.hasPermission(settings.getHostRankPermission())) {
            dispatch(settings.getHostRankQuitCommand(), player);
        }
    }

    private void dispatch(final @NotNull String command, final @NotNull Player player) {
        if (command.isBlank()) return;
        CommandUtil.dispatch(command.replace("%player%", player.getName()));
    }
}
