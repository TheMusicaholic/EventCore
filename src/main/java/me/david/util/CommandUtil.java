package me.david.util;

import lombok.experimental.UtilityClass;
import me.david.EventCore;
import me.david.util.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class CommandUtil {

    /**
     * Runs a configured command as the console on the next tick of the global region (the main thread on Paper),
     * where console commands have to run.
     */
    public void dispatch(@NotNull String command) {
        final String commandLine = toCommandLine(command);
        final Runnable dispatch = () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
        final EventCore plugin = EventCore.getInstance();

        if (plugin.isEnabled()) {
            FoliaScheduler.getGlobalRegionScheduler().execute(plugin, dispatch);
        } else {
            // Being disabled (a running game is stopped on shutdown): nothing can be scheduled any more.
            FoliaScheduler.getGlobalRegionScheduler().runNowOrSchedule(plugin, dispatch);
        }
    }

    /**
     * Runs a configured command as the console right away. Only for code that already runs on the global region.
     */
    public void dispatchNow(@NotNull String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toCommandLine(command));
    }

    // Commands in config.yml are written with a leading slash, which dispatchCommand doesn't accept.
    private String toCommandLine(@NotNull String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

}
