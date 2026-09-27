package me.david.listener;

import me.david.EventCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();

        // Same outcome as checking the bypass permission first, but skips the permission lookup
        // for the common case (game running, below the build limit).
        final boolean restricted = !EventCore.getInstance().getGameManager().isRunning()
                || event.getBlock().getY() > EventCore.getInstance().getSettings().getMaxBuildHeight();

        event.setCancelled(restricted && !player.hasPermission("event.bypass"));
    }

}
