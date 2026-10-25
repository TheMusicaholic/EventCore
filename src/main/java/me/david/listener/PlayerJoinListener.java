package me.david.listener;

import me.david.EventCore;
import me.david.util.*;
import me.david.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final Settings settings = EventCore.getInstance().getSettings();

        // Joining the server isn't joining the event (that's /event): only players logging in inside the event world
        // are readied for it, everyone else is left alone.
        EventCore.getInstance().getEventWorldManager().handleJoin(player);

        if (settings.isNotifyUpdatesOnJoin() && player.hasPermission("event.notify")) {
            // Shared checker: re-queries GitHub at most once an hour instead of on every join.
            final UpdateChecker updateChecker = EventCore.getInstance().getUpdateChecker();
            updateChecker.check();

            FoliaScheduler.getEntityScheduler().runDelayed(player, EventCore.getInstance(), o -> {
                if (updateChecker.isHasUpdate()) {
                    player.sendMessage(Component.empty());
                    player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("You're running an outdated version of EventCore. Please update to the latest version:")));
                    player.sendMessage(updateChecker.getUpdateComponent());
                    player.sendMessage(Component.empty());
                }
            }, null, 20L);
        }
    }

}
