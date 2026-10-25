package me.david.listener;

import me.david.EventCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerChangedWorldListener implements Listener {

    // Catches every way into and out of the event world (/event, /event leave, other plugins' teleports, portals,
    // respawning), so nothing can be carried in or out of it.
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        EventCore.getInstance().getEventWorldManager().update(event.getPlayer());
    }

}
