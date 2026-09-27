package me.david.listener;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.Set;

public class CreatureSpawnListener implements Listener {

    private static final Set<CreatureSpawnEvent.SpawnReason> BLOCKED_REASONS = EnumSet.of(
            CreatureSpawnEvent.SpawnReason.BREEDING,
            CreatureSpawnEvent.SpawnReason.EGG,
            CreatureSpawnEvent.SpawnReason.NATURAL,
            CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION,
            CreatureSpawnEvent.SpawnReason.RAID,
            CreatureSpawnEvent.SpawnReason.ENDER_PEARL
    );

    // Paper calls this before the mob is even created, so natural spawns are rejected without constructing
    // (and then discarding) an entity. Aborting also stops the spawner from retrying the rest of the pack.
    @EventHandler(ignoreCancelled = true)
    public void onPreCreatureSpawn(PreCreatureSpawnEvent event) {
        if (event.getReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            event.setCancelled(true);
            event.setShouldAbortSpawn(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (BLOCKED_REASONS.contains(event.getSpawnReason())) {
            event.setCancelled(true);
        }
    }

}
