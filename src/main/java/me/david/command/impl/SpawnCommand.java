package me.david.command.impl;

import me.david.EventCore;
import me.david.command.BukkitCommand;
import me.david.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class SpawnCommand extends BukkitCommand {

    private final EventCore plugin;

    public SpawnCommand(EventCore plugin) {
        super("spawn", "event.command.spawn");
        this.plugin = plugin;
    }

    // This is the event's spawn. The rest of the server usually has its own /spawn (from EssentialsX, for example),
    // and whichever plugin loads first gets the plain label. Outside the event world, hand the command to that one.
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        final Command other = sender instanceof Player player && !plugin.getEventWorldManager().isInEventWorld(player) ? getOtherSpawnCommand() : null;
        return other != null ? other.execute(sender, label, args) : super.execute(sender, label, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) throws IllegalArgumentException {
        final Command other = sender instanceof Player player && !plugin.getEventWorldManager().isInEventWorld(player) ? getOtherSpawnCommand() : null;
        return other != null ? other.tabComplete(sender, alias, args) : super.tabComplete(sender, alias, args);
    }

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof final Player player)) return;

        if (!plugin.getEventWorldManager().isInEventWorld(player)) {
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cYou are not in the event! Use §7/event §cto join it.")));
            return;
        }

        if (plugin.getMapManager().getSpawnLocation() == null) {
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cThere isn't a spawn location yet. Set one using the command /event setspawn")));
            return;
        }

        if (plugin.getGameManager().isRunning() && !player.hasPermission("event.bypass")) {
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cYou cannot teleport to the spawn while the event is running")));
            return;
        }

        player.teleportAsync(plugin.getMapManager().getSpawnLocation());
    }

    // Another plugin's /spawn, registered as <plugin>:spawn whether or not it got the plain label.
    private @Nullable Command getOtherSpawnCommand() {
        for (Map.Entry<String, Command> entry : Bukkit.getCommandMap().getKnownCommands().entrySet()) {
            if (entry.getKey().endsWith(":spawn") && !(entry.getValue() instanceof BukkitCommand)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
