package me.david.util;

import me.david.EventCore;
import me.david.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class AutoBroadcast implements Runnable {

    private int index = 0;

    @Override
    public void run() {
        final Settings settings = EventCore.getInstance().getSettings();
        final List<String> messages = settings.getAutoBroadcastMessages();

        if (!settings.isAutoBroadcastEnabled() || messages.isEmpty()) {
            return;
        }

        if (index >= messages.size()) {
            index = 0;
        }

        String message = messages.get(index);
        if (settings.isAutoBroadcastUseCommand()) {
            final String command = settings.getAutoBroadcastCommand().replace("%message%", message);
            // Configured with a leading slash like the other commands in config.yml, but dispatchCommand expects none.
            final String commandLine = command.startsWith("/") ? command.substring(1) : command;
            // This task runs async, but commands may only be dispatched from the server (global region) thread.
            FoliaScheduler.getGlobalRegionScheduler().execute(EventCore.getInstance(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine));
        } else {
            final Component component = MessageUtil.translateColorCodes(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Component.empty());
                player.sendMessage(Component.empty());
                player.sendMessage(component);
                player.sendMessage(Component.empty());
                player.sendMessage(Component.empty());
            }
        }

        index++;
    }

}
