package me.david.listener;

import me.david.EventCore;
import me.david.manager.EventWorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        final EventWorldManager eventWorld = EventCore.getInstance().getEventWorldManager();

        // Teleports from other plugins (like /tpa to someone in the event) mustn't get around the whitelist.
        if (!eventWorld.allowTeleport(event.getPlayer(), event.getFrom(), event.getTo())) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (!EventCore.getInstance().getSettings().isDisableEnderPearlsOutsideBorder()) return;

        final Location to = event.getTo();
        final World world = to.getWorld();

        if (eventWorld.isEventWorld(world) && !(world.getWorldBorder().isInside(to))) {
            event.setCancelled(true);
        }
    }

    // Only once no one has cancelled the teleport any more.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleported(PlayerTeleportEvent event) {
        EventCore.getInstance().getEventWorldManager().onTeleport(event.getPlayer(), event.getFrom(), event.getTo());
    }

}
