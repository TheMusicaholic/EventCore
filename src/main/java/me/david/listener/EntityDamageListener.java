package me.david.listener;

import me.david.EventCore;
import me.david.util.Settings;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class EntityDamageListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        final Settings settings = EventCore.getInstance().getSettings();

        if (settings.isDisableFallDamage()) {
            if (event.getCause() == EntityDamageByEntityEvent.DamageCause.FALL) {
                event.setCancelled(true);
                return;
            }
        }

        if (!(EventCore.getInstance().getGameManager().isRunning())) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getEntity() instanceof Player player)) return;

        if (settings.isBorderBoostEnabled()) {
            if (event.getCause() == EntityDamageEvent.DamageCause.WORLD_BORDER) {
                WorldBorder worldBorder = player.getWorld().getWorldBorder();
                // getLocation()/getCenter() allocate a new Location on every call, so fetch them once.
                final Location location = player.getLocation();
                if (!(worldBorder.isInside(location))) {
                    double boostXZ = settings.getBorderBoostStrengthXZ();
                    double boostY = settings.getBorderBoostStrengthY();
                    final Location center = worldBorder.getCenter();
                    final double radius = worldBorder.getSize() / 2;
                    double maxX = center.getBlockX() + radius;
                    double minX = center.getBlockX() - radius;
                    double maxZ = center.getBlockZ() + radius;
                    double minZ = center.getBlockZ() - radius;
                    // Summed up and applied once: every setVelocity() sends the player a velocity packet.
                    final Vector boost = new Vector();
                    if (location.getBlockX() > maxX) {
                        boost.add(new Vector(-boostXZ, boostY, 0));
                    } else if (location.getBlockX() < minX) {
                        boost.add(new Vector(boostXZ, boostY, 0));
                    }
                    if (location.getBlockZ() > maxZ) {
                        boost.add(new Vector(0, boostY, -boostXZ));
                    } else if (location.getBlockZ() < minZ) {
                        boost.add(new Vector(0, boostY, boostXZ));
                    }
                    if (boost.lengthSquared() != 0) {
                        player.setVelocity(player.getVelocity().add(boost));
                    }
                }
            }
        }
    }

}
