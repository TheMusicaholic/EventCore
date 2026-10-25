package me.david.command;

import me.david.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class BukkitCommand extends Command {

    private static final String FALLBACK_PREFIX = "eventcore";
    private static final Component NO_PERMISSION = MessageUtil.translateColorCodes("&cYou don't have permission to use this command!");

    // The server never removes commands registered straight into its CommandMap, so they're tracked for unregisterAll().
    private static final List<BukkitCommand> REGISTERED = new ArrayList<>();
    // Every label of the plugin's commands, plain and with the fallback prefix (like spawn and eventcore:spawn). Read
    // by the command listener on every region's thread on Folia.
    private static final Set<String> LABELS = ConcurrentHashMap.newKeySet();

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
        for (String label : command.labels) {
            LABELS.add(label.toLowerCase(Locale.ROOT));
            LABELS.add(FALLBACK_PREFIX + ":" + label.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Whether the label (lower case, without the slash) is one of the plugin's commands, like {@code spawn} or
     * {@code eventcore:spawn}.
     */
    public static boolean isOwnLabel(String label) {
        return LABELS.contains(label);
    }

    /**
     * The label that runs the plugin's own command for one of its labels: the label itself, or the label with the
     * fallback prefix if another plugin got the plain one first (like /spawn, which EssentialsX has too). Null if it
     * isn't one of the plugin's labels.
     */
    public static @Nullable String toOwnLabel(String label) {
        if (!LABELS.contains(label)) return null;
        return Bukkit.getCommandMap().getCommand(label) instanceof BukkitCommand ? label : FALLBACK_PREFIX + ":" + label;
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
        LABELS.clear();
    }

    private static void removeIfOwned(Map<String, Command> knownCommands, String label, Command command) {
        if (knownCommands.get(label) == command) {
            knownCommands.remove(label);
        }
    }
}
