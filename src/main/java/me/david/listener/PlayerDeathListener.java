package me.david.listener;

import me.david.EventCore;
import me.david.manager.EventWorldManager;
import me.david.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final EventWorldManager eventWorld = EventCore.getInstance().getEventWorldManager();

        if (!eventWorld.isInEventWorld(player)) return;

        // The death message would go to the whole server, so it's sent to the event world only, if at all.
        event.deathMessage(Component.empty());

        if (player.getGameMode() == GameMode.SPECTATOR) return;

        if (EventCore.getInstance().getSettings().isDeathMessageEnabled()) {
            final Player killer = player.getKiller();
            if (killer != null) {
                eventWorld.broadcast(MessageUtil.format("Messages.PlayerDeath.Message1", Map.of(
                                "%player%", Component.text(player.getName()),
                                "%killer%", Component.text(killer.getName()))
                ));
            } else {
                eventWorld.broadcast(MessageUtil.format(
                        "Messages.PlayerDeath.Message2", Map.of("%player%", Component.text(player.getName()))
                ));
            }
        }

        event.setKeepLevel(true);
        event.setDroppedExp(0);
        // The player is turned into a spectator once they've respawned (see the respawn listeners). Changing their
        // health or game mode here, while they're still dead, desyncs them from the client: after respawning they
        // can't move until they rejoin.
    }

}
