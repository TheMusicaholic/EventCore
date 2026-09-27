package me.david.listener;

import me.david.EventCore;
import me.david.util.Settings;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageByEntityListener implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        final Settings settings = EventCore.getInstance().getSettings();
        final Entity entity = event.getEntity();
        final Entity damager = event.getDamager();

        if (settings.isDisableItemExplosions()) {
            if (entity.getType() == EntityType.ITEM && damager.getType() == EntityType.END_CRYSTAL) {
                if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                    event.setCancelled(true);
                    event.setDamage(0);
                    return;
                }
            }
        }

        if (settings.isDisableFallDamage()) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
                return;
            }
        }

        // Same outcome as checking the bypass permission first, but skips the permission lookup while the game is running.
        event.setCancelled(!EventCore.getInstance().getGameManager().isRunning() && !damager.hasPermission("event.bypass"));
    }

}
