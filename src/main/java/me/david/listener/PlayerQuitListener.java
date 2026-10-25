package me.david.listener;

import me.david.EventCore;
import me.david.util.HostUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        // Frees the host slot, even if the event world has been changed since it was given out.
        HostUtil.removeHost(player);
        EventCore.getInstance().getEventWorldManager().handleQuit(player);
    }

}
