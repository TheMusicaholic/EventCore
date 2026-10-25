package me.david.listener.canvas;

import io.canvasmc.canvas.event.PlayerPostRespawnAsyncEvent;
import io.canvasmc.canvas.event.PlayerRespawnAsyncEvent;
import me.david.EventCore;
import me.david.util.PlayerUtil;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CanvasPlayerRespawnListener implements Listener {

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnAsyncEvent event) {
        // Still the world they died in.
        if (!EventCore.getInstance().getEventWorldManager().isInEventWorld(event.getPlayer())) return;

        event.setRespawnLocation(EventCore.getInstance().getMapManager().getSpawnLocation());
    }

    @EventHandler
    public void onPlayerPostRespawn(PlayerPostRespawnAsyncEvent event) {
        // An async event, so make sure the player is changed from the thread that owns them.
        PlayerUtil.runFor(event.getPlayer(), player -> {
            // In case they respawned in another world than they died in (updating twice does nothing).
            EventCore.getInstance().getEventWorldManager().update(player);

            if (EventCore.getInstance().getEventWorldManager().isInEventWorld(player)) {
                PlayerUtil.resetPlayer(player, GameMode.SPECTATOR);
            }
        });
    }

}
