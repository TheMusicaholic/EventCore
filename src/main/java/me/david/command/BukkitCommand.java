package me.david.command;

import me.david.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class BukkitCommand extends Command {

    private static final String FALLBACK_PREFIX = "eventcore";
    private static final Component NO_PERMISSION = MessageUtil.translateColorCodes("&cYou don't have permission to use this command!");

    // The server never removes commands registered straight into its CommandMap, so they're tracked for unregisterAll().
    private static final List<BukkitCommand> REGISTERED = new ArrayList<>();

    private final String permission;
    private final List<String> labels;

    public BukkitCommand(String name, String permission, String... aliases) {
        super(name, name + " command", "/" + name, aliases[0].isEmpty() ? Collections.singletonList(name) : Stream.concat(Stream.of(name), Arrays.stream(aliases)).toList());
        this.permission = permission;
        this.labels = Stream.concat(Stream.of(name), Arrays.stream(aliases).filter(alias -> !alias.isEmpty())).toList();
        registerCommand(this);
    }

    public BukkitCommand(String name, String permission) {
        this(name, permission, "");
    }

    public abstract void onCommand(CommandSender sender, String label, String[] args);

    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return super.tabComplete(sender, label, args);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        if (permission == null || sender.hasPermission(permission)) {
            onCommand(sender, label, args);
        } else {
            sender.sendMessage(NO_PERMISSION);
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) throws IllegalArgumentException {
        return onTabComplete(sender, alias, args);
    }


    private void registerCommand(BukkitCommand command) {
        Bukkit.getCommandMap().register(FALLBACK_PREFIX, command);
        REGISTERED.add(command);
    }

    /**
     * Removes all of the plugin's commands from the server's CommandMap. Left there, they would keep the disabled
     * plugin in memory, and after a reload the plain labels (like /event) would still point at the old instances.
     */
    public static void unregisterAll() {
        final CommandMap commandMap = Bukkit.getCommandMap();
        final Map<String, Command> knownCommands = commandMap.getKnownCommands();

        for (BukkitCommand command : REGISTERED) {
            command.unregister(commandMap);

            // Each label is registered both plain and with the fallback prefix. Only remove the entries that are
            // really ours, in case another plugin owns the plain label.
            for (String label : command.labels) {
                removeIfOwned(knownCommands, label, command);
                removeIfOwned(knownCommands, FALLBACK_PREFIX + ":" + label, command);
            }
        }
        REGISTERED.clear();
    }

    private static void removeIfOwned(Map<String, Command> knownCommands, String label, Command command) {
        if (knownCommands.get(label) == command) {
            knownCommands.remove(label);
        }
    }
}
