package me.david.listener;

import me.david.EventCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class PlayerItemDamageListener implements Listener {

    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (!EventCore.getInstance().getSettings().isUnbreakableArmor()) return;

        // Armor only loses durability through this event, so cancelling it keeps it from ever wearing down or breaking.
        if (event.getItem().getType().getEquipmentSlot().isArmor()) {
            event.setCancelled(true);
        }
    }

}
