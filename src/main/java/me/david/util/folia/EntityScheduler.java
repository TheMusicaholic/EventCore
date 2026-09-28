package me.david.util.folia;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

// Credits @ PacketEvents

public class EntityScheduler {
    private BukkitScheduler bukkitScheduler;

    protected EntityScheduler() {
        if (!FoliaScheduler.isFolia) {
            bukkitScheduler = Bukkit.getScheduler();
        }
    }

    public void execute(@NotNull Entity entity, @NotNull Plugin plugin, @NotNull Runnable run, @Nullable Runnable retired, long delay) {
        if (!FoliaScheduler.isFolia) {
            bukkitScheduler.runTaskLater(plugin, run, delay);
            return;
        }

        entity.getScheduler().execute(plugin, run, retired, delay);
    }

    /**
     * Runs the task on the thread that owns the entity: right away when called from it (always the case on Paper's
     * main thread), otherwise on the entity's next tick. Nothing can be scheduled while the plugin is being disabled,
     * so the task is skipped then unless it can run right away.
     */
    public void runNowOrSchedule(@NotNull Entity entity, @NotNull Plugin plugin, @NotNull Runnable run) {
        if (FoliaScheduler.isFolia ? Bukkit.isOwnedByCurrentRegion(entity) : Bukkit.isPrimaryThread()) {
            run.run();
        } else if (plugin.isEnabled()) {
            execute(entity, plugin, run, null, 1);
        }
    }

    public TaskWrapper run(@NotNull Entity entity, @NotNull Plugin plugin, @NotNull Consumer<Object> task, @Nullable Runnable retired) {
        if (!FoliaScheduler.isFolia) {
            return new TaskWrapper(bukkitScheduler.runTask(plugin, () -> task.accept(null)));
        }

        return new TaskWrapper(entity.getScheduler().run(plugin, (o) -> task.accept(null), retired));
    }

    public TaskWrapper runDelayed(@NotNull Entity entity, @NotNull Plugin plugin, @NotNull Consumer<Object> task, @Nullable Runnable retired, long delayTicks) {
        if (delayTicks < 1) delayTicks = 1;

        if (!FoliaScheduler.isFolia) {
            return new TaskWrapper(bukkitScheduler.runTaskLater(plugin, () -> task.accept(null), delayTicks));
        }

        return new TaskWrapper(entity.getScheduler().runDelayed(plugin, (o) -> task.accept(null), retired, delayTicks));
    }

    public TaskWrapper runAtFixedRate(@NotNull Entity entity, @NotNull Plugin plugin, @NotNull Consumer<Object> task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        if (initialDelayTicks < 1) initialDelayTicks = 1;
        if (periodTicks < 1) periodTicks = 1;

        if (!FoliaScheduler.isFolia) {
            return new TaskWrapper(bukkitScheduler.runTaskTimer(plugin, () -> task.accept(null), initialDelayTicks, periodTicks));
        }

        return new TaskWrapper(entity.getScheduler().runAtFixedRate(plugin, (o) -> task.accept(null), retired, initialDelayTicks, periodTicks));
    }
}