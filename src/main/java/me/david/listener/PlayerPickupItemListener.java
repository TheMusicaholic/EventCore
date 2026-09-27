package me.david.listener;

import me.david.EventCore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class PlayerPickupItemListener implements Listener {

    @EventHandler
    public void onPlayerPickUpItem(EntityPickupItemEvent event) {
        final Entity entity = event.getEntity();

        if (!(entity instanceof Player player)) return;
        if (EventCore.getInstance().getSettings().isAllowItemDropBeforeStart()) return;

        // Same outcome as checking the bypass permission first, but skips the permission lookup while the game is running.
        event.setCancelled(!EventCore.getInstance().getGameManager().isRunning() && !player.hasPermission("event.bypass"));
    }

}
