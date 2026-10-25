package me.david.listener;

import me.david.EventCore;
import me.david.command.BukkitCommand;
import me.david.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Locale;
import java.util.Set;

/**
 * Players in the event world can only use the event's commands (and the ones allowed in the config): other plugins'
 * commands (like /home, /tpa or /ec) would let them get around the event. Staff can use all commands.
 */
public class PlayerCommandListener implements Listener {

    // As early as possible, so no other plugin acts on a command that's blocked.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        if (!isRestricted(player)) return;

        // The message is the command line with its slash.
        final String commandLine = event.getMessage().substring(1).stripLeading();
        final int space = commandLine.indexOf(' ');
        final String label = (space < 0 ? commandLine : commandLine.substring(0, space)).toLowerCase(Locale.ROOT);
        if (label.isEmpty() || EventCore.getInstance().getSettings().getAllowedCommands().contains(label)) return;

        final String ownLabel = BukkitCommand.toOwnLabel(label);
        if (ownLabel == null) {
            event.setCancelled(true);
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.get("Messages.BlockedCommand")));
            return;
        }

        // In the event world, the event's commands come first: /spawn is the event's spawn, even if another plugin
        // has /spawn too.
        if (!ownLabel.equals(label)) {
            event.setMessage("/" + ownLabel + (space < 0 ? "" : commandLine.substring(space)));
        }
    }

    // Hides the commands they can't use from their tab completion. The commands are sent again whenever they enter or
    // leave the event world.
    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        if (!isRestricted(event.getPlayer())) return;

        final Set<String> allowed = EventCore.getInstance().getSettings().getAllowedCommands();
        event.getCommands().removeIf(command -> {
            final String label = command.toLowerCase(Locale.ROOT);
            return !allowed.contains(label) && !BukkitCommand.isOwnLabel(label);
        });
    }

    private static boolean isRestricted(Player player) {
        return EventCore.getInstance().getSettings().isBlockCommandsEnabled()
                && EventCore.getInstance().getEventWorldManager().isInEventWorld(player)
                && !player.hasPermission("event.commands.bypass")
                && !player.hasPermission("event.command");
    }

}
