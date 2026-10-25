package me.david.manager;

import me.david.EventCore;
import me.david.util.CommandUtil;
import me.david.util.HostUtil;
import me.david.util.MessageUtil;
import me.david.util.PlayerSnapshot;
import me.david.util.PlayerUtil;
import me.david.util.Settings;
import me.david.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The event takes place in one world (Settings.EventWorld), so it can share a server with other worlds. Everything
 * the plugin does is limited to that world, and players can't carry anything in or out of it: on entering it (by
 * /event or any other way), their own items and state are put away, and on leaving it (by /event leave or any other
 * way) they get them back.
 */
public class EventWorldManager {

    private static final String WHITELIST_ENABLED = "Settings.EventWhitelist.Enabled";
    private static final String WHITELIST_PLAYERS = "Settings.EventWhitelist.Players";

    // Where players teleporting into the event world came from, until they arrive: the world change event only
    // tells the world they left.
    private final Map<UUID, Location> origins = new ConcurrentHashMap<>();

    // Checked on every teleport into the event world, on region threads on Folia. The set is replaced, never changed.
    private volatile boolean whitelistEnabled;
    private volatile SortedSet<String> whitelist = Collections.emptySortedSet();

    public EventWorldManager() {
        loadSettings(EventCore.getInstance().getConfig());
    }

    public void loadSettings(@NotNull FileConfiguration config) {
        whitelistEnabled = config.getBoolean(WHITELIST_ENABLED);
        whitelist = toWhitelist(config.getStringList(WHITELIST_PLAYERS));
    }

    public boolean isEventWorld(@Nullable World world) {
        return world != null && world.getName().equalsIgnoreCase(EventCore.getInstance().getSettings().getEventWorld());
    }

    public boolean isInEventWorld(@NotNull Player player) {
        return isEventWorld(player.getWorld());
    }

    /**
     * The event world, or null if it isn't loaded.
     */
    public @Nullable World getWorld() {
        return Bukkit.getWorld(EventCore.getInstance().getSettings().getEventWorld());
    }

    /**
     * The players in the event world.
     */
    public @NotNull List<Player> getPlayers() {
        final List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInEventWorld(player)) players.add(player);
        }
        return players;
    }

    /**
     * Sends the message to the players in the event world.
     */
    public void broadcast(@NotNull Component message) {
        for (Player player : getPlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * Sends the player to the event spawn (/event).
     */
    public void join(@NotNull Player player) {
        if (isInEventWorld(player)) {
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cYou are already in the event! Use §7/event leave §cto leave it.")));
            return;
        }

        final Location spawn = EventCore.getInstance().getMapManager().getSpawnLocation();
        if (spawn == null || !spawn.isWorldLoaded() || !isEventWorld(spawn.getWorld())) {
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cThe event isn't set up yet!")));
            return;
        }

        if (!canEnter(player)) {
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cThe event is whitelisted, you can't join it right now!")));
            return;
        }

        origins.put(player.getUniqueId(), player.getLocation());
        teleport(player, spawn);
    }

    /**
     * Sends the player back to where they were before joining the event (/event leave).
     */
    public void leave(@NotNull Player player) {
        if (!isInEventWorld(player)) {
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cYou are not in the event!")));
            return;
        }

        Location location = PlayerSnapshot.getReturnLocation(player);
        if (location == null || isEventWorld(location.getWorld())) {
            location = getFallbackLocation();
        }
        if (location == null) {
            player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cThere is no other world to leave the event to!")));
            return;
        }

        teleport(player, location);
    }

    // The world change updates the player already, but don't depend on that alone: update once more when the teleport
    // is done (updating twice does nothing).
    private void teleport(@NotNull Player player, @NotNull Location location) {
        player.teleportAsync(location).thenAccept(success -> {
            if (success) {
                PlayerUtil.runFor(player, this::update);
            } else {
                origins.remove(player.getUniqueId());
            }
        });
    }

    /**
     * Brings the player in line with the world they're in: on entering the event world their own state is put away
     * and they're readied for the event, on leaving it they get their own state back. Does nothing if that's already
     * the case, so it's safe to call more than once. Has to run on the player's thread.
     */
    public void update(@NotNull Player player) {
        final boolean inEventWorld = isInEventWorld(player);
        if (inEventWorld == PlayerSnapshot.has(player)) return;

        final Location origin = origins.remove(player.getUniqueId());
        if (inEventWorld) {
            PlayerSnapshot.save(player, origin != null && !isEventWorld(origin.getWorld()) ? origin : getFallbackLocation());
            enter(player);
        } else {
            PlayerSnapshot.restore(player);
            exit(player);
        }

        // In the event world they only get the event's commands (see PlayerCommandListener), outside all of theirs again.
        player.updateCommands();
    }

    /**
     * Checks a teleport before it happens. Returns false if it has to be cancelled, because the player would enter
     * the event world while it's whitelisted without being allowed to.
     */
    public boolean allowTeleport(@NotNull Player player, @NotNull Location from, @NotNull Location to) {
        if (!isEntering(from, to) || canEnter(player)) return true;

        player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cThe event is whitelisted, you can't join it right now!")));
        return false;
    }

    /**
     * Notes where a player teleporting into the event world comes from, to send them back there when they leave.
     */
    public void onTeleport(@NotNull Player player, @NotNull Location from, @NotNull Location to) {
        if (isEntering(from, to)) {
            origins.put(player.getUniqueId(), from);
        }
    }

    private boolean isEntering(@NotNull Location from, @NotNull Location to) {
        return isEventWorld(to.getWorld()) && !isEventWorld(from.getWorld());
    }

    /**
     * Players logging in inside the event world stay in the event, everyone else is left alone.
     */
    public void handleJoin(@NotNull Player player) {
        if (!isInEventWorld(player)) {
            // They logged out in the event world, and it has been changed (in the config) since. Nothing happens if
            // there's no snapshot.
            PlayerSnapshot.restore(player);
            return;
        }

        // There already is one if they logged out in the event world. If not (they were in the world before it became
        // the event world), what they have now is what they get back.
        if (!PlayerSnapshot.has(player)) {
            PlayerSnapshot.save(player, getFallbackLocation());
        }
        enter(player);

        final EventCore plugin = EventCore.getInstance();
        FoliaScheduler.getEntityScheduler().runDelayed(player, plugin, o -> {
            final Location spawn = plugin.getMapManager().getSpawnLocation();
            if (spawn != null && spawn.isWorldLoaded()) {
                player.teleportAsync(spawn);
            }
            if (plugin.getGameManager().isRunning()) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }, null, 2);
    }

    /**
     * Players logging out inside the event world leave the event, but keep their place in the world (and their own
     * state put away) for when they log back in.
     */
    public void handleQuit(@NotNull Player player) {
        origins.remove(player.getUniqueId());
        if (isInEventWorld(player)) {
            exit(player);
        }
    }

    private void enter(@NotNull Player player) {
        final EventCore plugin = EventCore.getInstance();
        final Settings settings = plugin.getSettings();

        HostUtil.giveHost(player);

        for (String command : settings.getPlayerJoinCommands()) {
            CommandUtil.dispatch(command.replace("%player%", player.getName()));
        }

        if (settings.isJoinMessageEnabled()) {
            broadcast(MessageUtil.getPrefix().append(MessageUtil.format("Messages.PlayerJoin.Message", Map.of("%player%", Component.text(player.getName())))));
        }

        // Late joiners only spectate, so they go straight to spectator without the kit (which would be cleared again).
        PlayerUtil.resetPlayer(player, plugin.getGameManager().isRunning() ? GameMode.SPECTATOR : GameMode.SURVIVAL);
        player.getEnderChest().clear();
    }

    private void exit(@NotNull Player player) {
        final Settings settings = EventCore.getInstance().getSettings();

        HostUtil.removeHost(player);

        for (String command : settings.getPlayerQuitCommands()) {
            CommandUtil.dispatch(command.replace("%player%", player.getName()));
        }

        if (settings.isQuitMessageEnabled()) {
            final Component message = MessageUtil.format("Messages.PlayerQuit.Message", Map.of("%player%", Component.text(player.getName())));
            for (Player other : getPlayers()) {
                if (other != player) other.sendMessage(message);
            }
        }
    }

    // The spawn of the main world, or of the first other overworld if the main world is the event world (never the
    // nether or the end).
    private @Nullable Location getFallbackLocation() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL && !isEventWorld(world)) return world.getSpawnLocation();
        }
        return null;
    }

    /**
     * Whether the player may enter the event world: anyone while the whitelist is off, otherwise whitelisted players
     * and staff.
     */
    public boolean canEnter(@NotNull Player player) {
        return !whitelistEnabled
                || whitelist.contains(player.getName())
                || player.hasPermission("event.whitelist.bypass")
                || player.hasPermission("event.command");
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    /**
     * The whitelisted player names, sorted and compared ignoring case.
     */
    public @NotNull SortedSet<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelistEnabled(boolean enabled) {
        whitelistEnabled = enabled;
        EventCore.getInstance().getConfig().set(WHITELIST_ENABLED, enabled);
        EventCore.getInstance().saveConfig();
    }

    /**
     * Returns false if the player is whitelisted already.
     */
    public synchronized boolean addToWhitelist(@NotNull String name) {
        if (whitelist.contains(name)) return false;

        final List<String> names = new ArrayList<>(whitelist);
        names.add(name);
        saveWhitelist(names);
        return true;
    }

    /**
     * Returns false if the player wasn't whitelisted.
     */
    public synchronized boolean removeFromWhitelist(@NotNull String name) {
        if (!whitelist.contains(name)) return false;

        final List<String> names = new ArrayList<>(whitelist);
        names.removeIf(name::equalsIgnoreCase);
        saveWhitelist(names);
        return true;
    }

    private void saveWhitelist(@NotNull List<String> names) {
        whitelist = toWhitelist(names);
        EventCore.getInstance().getConfig().set(WHITELIST_PLAYERS, List.copyOf(whitelist));
        EventCore.getInstance().saveConfig();
    }

    private static @NotNull SortedSet<String> toWhitelist(@NotNull List<String> names) {
        final SortedSet<String> whitelist = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        whitelist.addAll(names);
        return Collections.unmodifiableSortedSet(whitelist);
    }

}
