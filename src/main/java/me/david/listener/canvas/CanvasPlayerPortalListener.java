package me.david.listener.canvas;

import io.canvasmc.canvas.event.EntityPostPortalAsyncEvent;
import me.david.EventCore;
import me.david.util.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CanvasPlayerPortalListener implements Listener {

    // Like teleports, going through a portal (out of the event world, or into it) mustn't depend on the world change
    // event alone to put the player's own items away or give them back (updating twice does nothing).
    @EventHandler
    public void onEntityPostPortal(EntityPostPortalAsyncEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerUtil.runFor(player, EventCore.getInstance().getEventWorldManager()::update);
        }
    }

}
