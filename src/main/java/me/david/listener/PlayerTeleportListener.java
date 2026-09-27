package me.david.listener;

import me.david.EventCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (!EventCore.getInstance().getSettings().isDisableEnderPearlsOutsideBorder()) return;

        final Location to = event.getTo();
        final World world = to.getWorld();

        if (!(world.getWorldBorder().isInside(to))) {
            event.setCancelled(true);
        }
    }

}
