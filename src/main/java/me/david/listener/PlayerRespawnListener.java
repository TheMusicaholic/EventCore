package me.david.listener;

import me.david.EventCore;
import me.david.util.PlayerUtil;
import me.david.util.folia.FoliaScheduler;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        event.setRespawnLocation(EventCore.getInstance().getMapManager().getSpawnLocation());
        // The player isn't fully respawned while this event runs, so changing them now gets lost or leaves them stuck.
        // Wait until the next tick, when they're back in the world.
        FoliaScheduler.getEntityScheduler().runDelayed(player, EventCore.getInstance(), o -> {
            if (player.isOnline()) PlayerUtil.resetPlayer(player, GameMode.SPECTATOR);
        }, null, 1);
    }

}
