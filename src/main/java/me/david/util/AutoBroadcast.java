package me.david.util;

import me.david.EventCore;
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
            // This task runs async, but commands may only be dispatched from the server (global region) thread.
            CommandUtil.dispatch(settings.getAutoBroadcastCommand().replace("%message%", message));
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
