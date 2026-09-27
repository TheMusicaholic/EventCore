package me.david.listener;

import me.david.EventCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class EntityExplodeListener implements Listener {

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Block#getDrops() returns a freshly computed copy (rolling the loot table for every exploded block),
        // so clearing it never removed anything. A yield of 0 is what actually stops the blocks from dropping.
        event.setYield(0F);
        event.setCancelled(!(EventCore.getInstance().getGameManager().isRunning()));
    }

}
