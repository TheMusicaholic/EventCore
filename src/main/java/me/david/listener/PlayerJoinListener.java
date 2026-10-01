package me.david.listener;

import me.david.EventCore;
import me.david.util.*;
import me.david.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final Settings settings = EventCore.getInstance().getSettings();

        HostUtil.giveHost(player);

        for (String command : settings.getPlayerJoinCommands()) {
            CommandUtil.dispatch(command.replace("%player%", player.getName()));
        }

        if (settings.isJoinMessageEnabled()) {
            Component message = MessageUtil.getPrefix().append(MessageUtil.format("Messages.PlayerJoin.Message", Map.of("%player%", Component.text(player.getName()))));
            event.joinMessage(message);
        } else {
            event.joinMessage(Component.empty());
        }

        // Late joiners only spectate, so they go straight to spectator without the kit (which would be cleared again).
        PlayerUtil.resetPlayer(player, EventCore.getInstance().getGameManager().isRunning() ? GameMode.SPECTATOR : GameMode.SURVIVAL);
        FoliaScheduler.getEntityScheduler().runDelayed(player, EventCore.getInstance(), o -> {
            // On Paper the task still runs when the player left in the meantime.
            if (!player.isOnline()) return;
            player.teleportAsync(EventCore.getInstance().getMapManager().getSpawnLocation());
            if (EventCore.getInstance().getGameManager().isRunning()) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }, null, 2);

        if (settings.isNotifyUpdatesOnJoin() && player.hasPermission("event.notify")) {
            // Shared checker: re-queries GitHub at most once an hour instead of on every join.
            final UpdateChecker updateChecker = EventCore.getInstance().getUpdateChecker();
            updateChecker.check();

            FoliaScheduler.getEntityScheduler().runDelayed(player, EventCore.getInstance(), o -> {
                if (player.isOnline() && updateChecker.isHasUpdate()) {
                    player.sendMessage(Component.empty());
                    player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("You're running an outdated version of EventCore. Please update to the latest version:")));
                    player.sendMessage(updateChecker.getUpdateComponent());
                    player.sendMessage(Component.empty());
                }
            }, null, 20L);
        }
    }

}
