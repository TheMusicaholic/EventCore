package me.david.command.impl;

import me.david.EventCore;
import me.david.command.BukkitCommand;
import me.david.manager.EventWorldManager;
import me.david.util.BorderUtil;
import me.david.util.MessageUtil;
import me.david.util.PlayerUtil;
import me.david.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventCommand extends BukkitCommand {

    private final EventCore plugin;

    public EventCommand(EventCore plugin) {
        // Everyone may join and leave the event, the other subcommands check for event.command themselves.
        super("event", null, "e");
        this.plugin = plugin;
    }

    private String getSoftware() {
        // Canvas is a Folia fork, so check for it first.
        if (FoliaScheduler.isCanvas()) return "Canvas";
        return FoliaScheduler.isFolia() ? "Folia" : "PaperMC";
    }

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        final EventWorldManager eventWorld = plugin.getEventWorldManager();

        if (args.length == 0) {
            if (!(sender instanceof final Player player)) {
                sendUsage(sender);
                return;
            }

            if (!player.hasPermission("event.join")) {
                player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cYou don't have permission to join the event!")));
                return;
            }

            eventWorld.join(player);
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {
            if (sender instanceof final Player player) {
                eventWorld.leave(player);
            }
            return;
        }

        if (!sender.hasPermission("event.command")) {
            sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event")));
            sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event leave")));
            return;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(" ");
                sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§7Running §aEventCore §7v" + plugin.getPluginMeta().getVersion() + " §7on §a" + getSoftware())));
                sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§7Download at §ahttps://github.com/VertrauterDavid")));
                sendUsage(sender);
                return;
            }

            if (args[0].equalsIgnoreCase("start")) {
                plugin.getGameManager().start();
                return;
            }

            if (args[0].equalsIgnoreCase("stop")) {
                sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event stop <winner>")));
                return;
            }

            if (args[0].equalsIgnoreCase("drop")) {
                plugin.getMapManager().drop();
                return;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                plugin.getMapManager().reset();
                return;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                double currentMS = System.currentTimeMillis();
                plugin.reloadConfig();
                double reloadMS = System.currentTimeMillis() - currentMS;

                sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§aYou successfully reloaded the config within %ms%ms!").replaceText(b -> b.matchLiteral("%ms%").replacement(Component.text(String.valueOf(reloadMS))))));
                return;
            }

            if (args[0].equalsIgnoreCase("setSpawn")) {
                if (!(sender instanceof final Player player)) return;

                if (!eventWorld.isInEventWorld(player)) {
                    player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§cThe spawn has to be in the event world §7" + plugin.getSettings().getEventWorld() + "§c! Change the world with §7Settings.EventWorld §cin the config.yml")));
                    return;
                }

                plugin.getMapManager().saveSpawnLocation(player);
                player.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§aSpawn location saved!")));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5, 5);
                player.closeInventory();
                return;
            }

            // Kicking would throw them off the whole server, so they're sent out of the event instead.
            if (args[0].equalsIgnoreCase("kickspec")) {
                for (final Player target : eventWorld.getPlayers()) {
                    if (!(target.hasPermission("event.spec")) && target.getGameMode() == GameMode.SPECTATOR) {
                        PlayerUtil.runFor(target, eventWorld::leave);
                    }
                }

                sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("All spectators have been sent out of the event!")));
                return;
            }

            if (args[0].equalsIgnoreCase("kickall")) {
                for (final Player target : eventWorld.getPlayers()) {
                    if (!(target.hasPermission("event.spec"))) {
                        PlayerUtil.runFor(target, eventWorld::leave);
                    }
                }
                sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("All players have been sent out of the event!")));
                return;
            }

            if (args[0].equalsIgnoreCase("clearall")) {
                PlayerUtil.runForEventPlayers(target -> {
                    target.getInventory().setArmorContents(null);
                    target.getInventory().clear();
                });
                sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("All players has been cleared!")));
                return;
            }
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("autoBorder")) {
                if (args[1].equalsIgnoreCase("on")) {
                    BorderUtil.setAutoBorder(true);

                    sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("AutoBorder has been §aactivated!")));
                    return;
                }

                if (args[1].equalsIgnoreCase("off")) {
                    BorderUtil.setAutoBorder(false);
                    BorderUtil.lastOptimal = BorderUtil.borderDefault;

                    sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("AutoBorder has been §cdeactivated!")));
                    return;
                }
            }

            if (args[0].equalsIgnoreCase("whitelist")) {
                if (args[1].equalsIgnoreCase("on")) {
                    eventWorld.setWhitelistEnabled(true);

                    sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("The event whitelist has been §aactivated§7! Only whitelisted players can join the event now.")));
                    return;
                }

                if (args[1].equalsIgnoreCase("off")) {
                    eventWorld.setWhitelistEnabled(false);

                    sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("The event whitelist has been §cdeactivated§7! Everyone can join the event now.")));
                    return;
                }

                if (args[1].equalsIgnoreCase("list")) {
                    final String state = eventWorld.isWhitelistEnabled() ? "§aon" : "§coff";
                    final String players = eventWorld.getWhitelist().isEmpty() ? "§7-" : "§a" + String.join("§7, §a", eventWorld.getWhitelist());

                    sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("The event whitelist is " + state + "§7. Whitelisted players (" + eventWorld.getWhitelist().size() + "): " + players)));
                    return;
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("whitelist")) {
            // The exact name if they're online, as typed otherwise.
            final Player target = Bukkit.getPlayerExact(args[2]);
            final String name = target != null ? target.getName() : args[2];

            if (args[1].equalsIgnoreCase("add")) {
                if (eventWorld.addToWhitelist(name)) {
                    sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§a" + name + " §7has been added to the event whitelist!")));
                } else {
                    sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§c" + name + " is already on the event whitelist!")));
                }
                return;
            }

            if (args[1].equalsIgnoreCase("remove")) {
                if (eventWorld.removeFromWhitelist(name)) {
                    sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§a" + name + " §7has been removed from the event whitelist!")));
                } else {
                    sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("§c" + name + " isn't on the event whitelist!")));
                }
                return;
            }
        }

        if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("stop")) {
                StringBuilder winner = null;
                for (int i = 1; i < args.length; i++) {
                    if (winner == null) {
                        winner = new StringBuilder(args[i]);
                    } else {
                        winner.append(" ").append(args[i]);
                    }
                }

                plugin.getGameManager().stop(winner.toString());
                return;
            }
        }

        sendUsage(sender);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event leave")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event start")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event stop <winner>")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event drop")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event reset")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event autoBorder <on / off>")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event whitelist <on / off / list>")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event whitelist <add / remove> <player>")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event setSpawn")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event reload")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event kickspec")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: §c/event kickall")));
        sender.sendMessage(MessageUtil.getPrefix().append(MessageUtil.translateColorCodes("Usage: &c/event clearall")));
        sender.sendMessage(" ");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof final Player player)) return new ArrayList<>();
        if (!(player.hasPermission("event.command"))) {
            return args.length == 1 && "leave".startsWith(args[0].toLowerCase()) ? List.of("leave") : new ArrayList<>();
        }

        List<String> list = new ArrayList<>();

        if (args.length == 3 && args[0].equalsIgnoreCase("whitelist")) {
            if (args[1].equalsIgnoreCase("add")) {
                list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }

            if (args[1].equalsIgnoreCase("remove")) {
                list.addAll(plugin.getEventWorldManager().getWhitelist());
            }
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("stop")) {
                list.addAll(plugin.getEventWorldManager().getPlayers().stream().map(Player::getName).toList());
            }

            if (args[0].equalsIgnoreCase("autoBorder")) {
                list.addAll(Arrays.asList("on", "off"));
            }

            if (args[0].equalsIgnoreCase("whitelist")) {
                list.addAll(Arrays.asList("on", "off", "list", "add", "remove"));
            }
        }

        if (args.length == 1) {
            list.addAll(Arrays.asList("leave", "help", "start", "stop", "drop", "reset", "autoBorder", "whitelist", "kickspec", "kickall", "clearall", "setSpawn", "reload"));
        }

        return list.stream().filter(content -> content.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).sorted().toList();
    }

}
