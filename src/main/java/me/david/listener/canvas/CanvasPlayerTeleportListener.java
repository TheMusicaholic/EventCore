package me.david.listener.canvas;

import io.canvasmc.canvas.event.EntityTeleportAsyncEvent;
import me.david.EventCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class CanvasPlayerTeleportListener implements Listener {

    @EventHandler
    public void onEntityTeleport(EntityTeleportAsyncEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (!EventCore.getInstance().getSettings().isDisableEnderPearlsOutsideBorder()) return;

        final Location to = event.getTo();
        final World world = to.getWorld();

        if (!(world.getWorldBorder().isInside(to))) {
            event.setCancelled(true);
        }
    }

}
