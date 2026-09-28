package me.david.manager;

import lombok.Getter;
import me.david.EventCore;
import me.david.api.events.game.GameStartEvent;
import me.david.api.events.game.GameStopEvent;
import me.david.api.events.game.GameTimerTickEvent;
import me.david.api.events.game.InGameTimerTickEvent;
import me.david.util.BorderUtil;
import me.david.util.CommandUtil;
import me.david.util.MessageUtil;
import me.david.util.PlayerUtil;
import me.david.util.Settings;
import me.david.util.folia.FoliaScheduler;
import me.david.util.folia.TaskWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class GameManager implements me.david.api.manager.GameManager {

    // Read by listeners and tasks on other threads (region threads on Folia).
    private volatile boolean running = false;
    private volatile boolean timerRunning = false;

    // Replaced and cancelled from different threads on Folia (commands, and the countdown on the global region).
    private volatile TaskWrapper startTask;
    private volatile TaskWrapper autoStopTask;
    private volatile TaskWrapper autoDropTask;
    private volatile TaskWrapper timerTask;

    private volatile AtomicInteger timer;
    private volatile long inGameTimer;
    private volatile boolean autoDropped = false;

    public void start() {
        stopAllTimers();
        if (timerRunning) return;

        final Settings settings = EventCore.getInstance().getSettings();

        running = false;
        autoDropped = false;
        timerRunning = true;

        timer = new AtomicInteger(settings.getStartTimerSeconds());

        final GameStartEvent gameStartEvent = new GameStartEvent(timer.get());
        Bukkit.getPluginManager().callEvent(gameStartEvent);

        if (gameStartEvent.isCancelled()) {
            timerRunning = false;
            return;
        }

        final AtomicReference<TaskWrapper> countdown = new AtomicReference<>();
        countdown.set(FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), o -> {
            if (!timerRunning || running) return;

            int current = timer.get();

            Bukkit.getPluginManager().callEvent(new GameTimerTickEvent(current));

            // The message and title are the same for every player, so build them once per tick.
            final Component message;
            final Title title;
            final Sound sound;
            if (current > 0) {
                String timerText = settings.getStartTimerColor(current) + current + "§7";

                final var replacements = Map.of(
                        "%timer%", MessageUtil.translateColorCodes(timerText),
                        "%prefix%", MessageUtil.getPrefix()
                );

                message = MessageUtil.getPrefix().append(MessageUtil.format("Messages.StartTimer.Message", replacements));
                title = Title.title(MessageUtil.format("Messages.StartTimer.Title", replacements), MessageUtil.format("Messages.StartTimer.SubTitle", replacements));
                sound = Sound.ENTITY_CHICKEN_EGG;
            } else {
                message = MessageUtil.getPrefix().append(MessageUtil.get("Messages.Start.Message"));
                title = Title.title(MessageUtil.get("Messages.Start.Title"), MessageUtil.get("Messages.Start.SubTitle"));
                sound = Sound.ENTITY_PLAYER_LEVELUP;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
                player.showTitle(title);
                // Played at the player entity: no Location to allocate, and no reading another region's player position.
                player.playSound(player, sound, 5, 5);
            }

            if (current <= 0) {
                for (World world : Bukkit.getWorlds()) {
                    world.setDifficulty(Difficulty.HARD);
                }

                if (settings.isIngameTimerEnabled() && !settings.isActionbarEnabled()) {
                    startInGameTimer();
                }

                for (String command : settings.getStartCommands()) {
                    CommandUtil.dispatch(command);
                }

                running = true;
                timerRunning = false;

                // Cancel through this countdown's own handle: startTask may already belong to a newer countdown.
                final TaskWrapper self = countdown.get();
                if (self != null) {
                    self.cancel();
                }
            } else {
                timer.decrementAndGet();
            }
        }, 0, 20));
        startTask = countdown.get();

        if (settings.isAutoStopOnOnePlayer()) {
            autoStopTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), o -> {
                if (running && PlayerUtil.getAlive() == 1) {
                    running = false;
                    FoliaScheduler.getGlobalRegionScheduler().execute(EventCore.getInstance(), () -> stop(
                            Bukkit.getOnlinePlayers().stream()
                                    .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
                                    .findFirst()
                                    .map(Player::getName)
                                    .orElse("Unknown")
                    ));
                }
            }, 0, 20);
        }

        if (settings.isDropOnPlayerCountEnabled()) {
            autoDropTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), o -> {
                if (running && !autoDropped && PlayerUtil.getAlive() <= EventCore.getInstance().getSettings().getDropOnPlayerCount()) {
                    autoDropped = true;
                    EventCore.getInstance().getMapManager().drop();
                }
            }, 0, 20);
        }
    }

    public void stop(final String winner) {
        final GameStopEvent gameStopEvent = new GameStopEvent(winner);
        Bukkit.getPluginManager().callEvent(gameStopEvent);

        if (gameStopEvent.isCancelled()) {
            return;
        }

        final EventCore plugin = EventCore.getInstance();
        final Settings settings = plugin.getSettings();

        running = false;
        timerRunning = false;
        BorderUtil.lastOptimal = BorderUtil.borderDefault;

        stopInGameTimer();
        stopAllTimers();

        final var replacements = Map.of(
                // There's no winner when stopped without one (through the API, or on shutdown).
                "%winner%", MessageUtil.translateColorCodes(winner != null ? winner : "Unknown"),
                "%prefix%", MessageUtil.getPrefix()
        );

        final Component message = MessageUtil.getPrefix().append(MessageUtil.format("Messages.Stop.Message", replacements));
        final Title title = Title.title(MessageUtil.format("Messages.Stop.Title", replacements), MessageUtil.format("Messages.Stop.SubTitle", replacements));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            player.showTitle(title);
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 5, 5);
            PlayerUtil.runFor(player, PlayerUtil::cleanPlayer);
        }

        // On Folia worlds may only be changed from the global region, and /event stop runs on the sender's region.
        FoliaScheduler.getGlobalRegionScheduler().runNowOrSchedule(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                world.setDifficulty(Difficulty.PEACEFUL);
                world.getWorldBorder().setSize(BorderUtil.borderDefault);
            }
        });

        for (String command : settings.getStopCommands()) {
            CommandUtil.dispatch(command);
        }

        if (settings.isMapAutoReset()) {
            plugin.getMapManager().reset();
        }
    }

    public void startInGameTimer() {
        inGameTimer = 0;

        timerTask = cancel(timerTask);

        timerTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(EventCore.getInstance(), o -> {
            inGameTimer++;

            Bukkit.getPluginManager().callEvent(new InGameTimerTickEvent(inGameTimer));

            String raw = EventCore.getInstance().getSettings().getIngameTimerFormat()
                    .replace("hh", String.format("%02d", (inGameTimer / 3600)))
                    .replace("mm", String.format("%02d", ((inGameTimer % 3600) / 60)))
                    .replace("ss", String.format("%02d", (inGameTimer % 60)));

            // Same text for everyone, so translate it once per tick instead of once per player.
            final Component actionbar = MessageUtil.translateColorCodes(raw);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(actionbar);
            }
        }, 0, 20);
    }

    public void stopInGameTimer() {
        inGameTimer = 0;
        timerTask = cancel(timerTask);
    }

    private void stopAllTimers() {
        timerRunning = false;

        startTask = cancel(startTask);
        autoStopTask = cancel(autoStopTask);
        autoDropTask = cancel(autoDropTask);
        timerTask = cancel(timerTask);
    }

    // Takes the task as an argument so the field is read only once (another thread may clear it in between).
    // Returns null, to clear the field with.
    private static TaskWrapper cancel(final TaskWrapper task) {
        if (task != null) task.cancel();
        return null;
    }
}
