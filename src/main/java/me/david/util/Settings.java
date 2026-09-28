package me.david.util;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of the config values read at runtime (event listeners, repeating tasks and game flow).
 * Every {@link FileConfiguration} lookup walks the section tree and allocates, and the config isn't thread-safe
 * (listeners and tasks run on several threads on Folia), so the values are read once here and the snapshot is
 * rebuilt whenever the config is reloaded.
 */
@Getter
public final class Settings {

    private final long maxBuildHeight;
    private final boolean disableFallDamage;
    private final boolean disableItemExplosions;
    private final boolean allowItemDropBeforeStart;
    private final boolean disableEnderPearlsOutsideBorder;
    private final boolean borderBoostEnabled;
    private final double borderBoostStrengthXZ;
    private final double borderBoostStrengthY;
    private final boolean notifyUpdatesOnJoin;
    private final boolean logUpdatesInConsole;
    private final List<String> playerJoinCommands;
    private final List<String> playerQuitCommands;
    private final boolean hostRankEnabled;
    private final String hostRankPermission;
    private final String hostRankJoinCommand;
    private final String hostRankQuitCommand;
    private final boolean joinMessageEnabled;
    private final boolean quitMessageEnabled;
    private final boolean deathMessageEnabled;
    private final int startTimerSeconds;
    @Getter(AccessLevel.NONE)
    private final String[] startTimerColors;
    private final List<String> startCommands;
    private final List<String> stopCommands;
    private final boolean autoStopOnOnePlayer;
    private final boolean dropOnPlayerCountEnabled;
    private final long dropOnPlayerCount;
    private final long dropBorderExtra;
    private final List<String> dropCommands;
    private final boolean mapAutoReset;
    private final List<String> mapResetCommands;
    private final boolean ingameTimerEnabled;
    private final String ingameTimerFormat;
    private final boolean actionbarEnabled;
    private final String actionbarMessage;
    private final boolean announcementTitleEnabled;
    private final boolean autoBroadcastEnabled;
    private final List<String> autoBroadcastMessages;
    private final boolean autoBroadcastUseCommand;
    private final String autoBroadcastCommand;

    public Settings(@NotNull final FileConfiguration config) {
        maxBuildHeight = config.getLong("Settings.MaxBuildHeight", 0L);
        disableFallDamage = config.getBoolean("Settings.DisableFallDamage", true);
        disableItemExplosions = config.getBoolean("Settings.DisableItemExplosions", true);
        allowItemDropBeforeStart = config.getBoolean("Settings.AllowItemDropBeforeStart");
        disableEnderPearlsOutsideBorder = config.getBoolean("Settings.WorldBorder.DisableEnderPeals");
        borderBoostEnabled = config.getBoolean("Settings.WorldBorder.Boost.Enabled");
        borderBoostStrengthXZ = config.getDouble("Settings.WorldBorder.Boost.StrengthXZ", 1.3);
        borderBoostStrengthY = config.getDouble("Settings.WorldBorder.Boost.StrengthY", 0.1);
        notifyUpdatesOnJoin = config.getBoolean("Settings.Updates.NotifyOnJoin");
        logUpdatesInConsole = config.getBoolean("Settings.Updates.LogInConsole");
        playerJoinCommands = List.copyOf(config.getStringList("Settings.PlayerJoin.Commands"));
        playerQuitCommands = List.copyOf(config.getStringList("Settings.PlayerQuit.Commands"));
        hostRankEnabled = config.getBoolean("Settings.HostRank.Enabled");
        hostRankPermission = Objects.requireNonNullElse(config.getString("Settings.HostRank.Permission"), "event.host");
        hostRankJoinCommand = Objects.requireNonNullElse(config.getString("Settings.HostRank.JoinCommand"), "");
        hostRankQuitCommand = Objects.requireNonNullElse(config.getString("Settings.HostRank.QuitCommand"), "");
        joinMessageEnabled = config.getBoolean("Messages.PlayerJoin.Enabled");
        quitMessageEnabled = config.getBoolean("Messages.PlayerQuit.Enabled");
        deathMessageEnabled = config.getBoolean("Messages.PlayerDeath.Enabled");
        startTimerSeconds = config.getInt("Messages.StartTimer.Timer", 5);
        startTimerColors = new String[Math.max(startTimerSeconds, 0) + 1];
        for (int second = 1; second < startTimerColors.length; second++) {
            startTimerColors[second] = Objects.requireNonNullElse(config.getString("Messages.StartTimer.Colors." + second + "sec"), "");
        }
        startCommands = List.copyOf(config.getStringList("Settings.Start.CustomCommands"));
        stopCommands = List.copyOf(config.getStringList("Settings.Stop.CustomCommands"));
        autoStopOnOnePlayer = config.getBoolean("Settings.AutoStop1Player");
        dropOnPlayerCountEnabled = config.getBoolean("Settings.DropOnPlayerCount.Enabled");
        dropOnPlayerCount = config.getLong("Settings.DropOnPlayerCount.Count");
        dropBorderExtra = config.getLong("Settings.Drop.BorderExtra", 3);
        dropCommands = List.copyOf(config.getStringList("Settings.Drop.CustomCommands"));
        mapAutoReset = config.getBoolean("Settings.MapReset.AutoReset");
        mapResetCommands = List.copyOf(config.getStringList("Settings.MapReset.Commands"));
        ingameTimerEnabled = config.getBoolean("Settings.IngameTimer.Enabled");
        ingameTimerFormat = Objects.requireNonNullElse(config.getString("Settings.IngameTimer.Format"), "");
        actionbarEnabled = config.getBoolean("Messages.Actionbar.Enabled");
        actionbarMessage = config.getString("Messages.Actionbar.Message", "&aYou are playing the best Event!");
        announcementTitleEnabled = config.getBoolean("Messages.AnnoucementCommand.Title.Enabled");
        autoBroadcastEnabled = config.getBoolean("AutoBroadcast.Enabled");
        autoBroadcastMessages = List.copyOf(config.getStringList("AutoBroadcast.Messages"));
        autoBroadcastUseCommand = config.getBoolean("AutoBroadcast.UseBroadcastCommand");
        autoBroadcastCommand = config.getString("AutoBroadcast.BroadcastCommand", "");
    }

    /**
     * The color code for the given second of the start countdown ({@code Messages.StartTimer.Colors.<second>sec}),
     * or an empty string if none is configured.
     */
    public @NotNull String getStartTimerColor(final int second) {
        return second > 0 && second < startTimerColors.length ? startTimerColors[second] : "";
    }

}
