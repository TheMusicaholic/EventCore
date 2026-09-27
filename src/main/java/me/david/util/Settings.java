package me.david.util;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of the config values read on hot paths (event listeners and repeating tasks).
 * Every {@link FileConfiguration} lookup walks the section tree and allocates, so the values are read once here
 * and the snapshot is rebuilt whenever the config is reloaded.
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
    private final List<String> playerJoinCommands;
    private final List<String> playerQuitCommands;
    private final boolean joinMessageEnabled;
    private final boolean quitMessageEnabled;
    private final boolean deathMessageEnabled;
    private final long dropOnPlayerCount;
    private final String ingameTimerFormat;
    private final String actionbarMessage;
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
        playerJoinCommands = List.copyOf(config.getStringList("Settings.PlayerJoin.Commands"));
        playerQuitCommands = List.copyOf(config.getStringList("Settings.PlayerQuit.Commands"));
        joinMessageEnabled = config.getBoolean("Messages.PlayerJoin.Enabled");
        quitMessageEnabled = config.getBoolean("Messages.PlayerQuit.Enabled");
        deathMessageEnabled = config.getBoolean("Messages.PlayerDeath.Enabled");
        dropOnPlayerCount = config.getLong("Settings.DropOnPlayerCount.Count");
        ingameTimerFormat = Objects.requireNonNullElse(config.getString("Settings.IngameTimer.Format"), "");
        actionbarMessage = config.getString("Messages.Actionbar.Message", "&aYou are playing the best Event!");
        autoBroadcastEnabled = config.getBoolean("AutoBroadcast.Enabled");
        autoBroadcastMessages = List.copyOf(config.getStringList("AutoBroadcast.Messages"));
        autoBroadcastUseCommand = config.getBoolean("AutoBroadcast.UseBroadcastCommand");
        autoBroadcastCommand = config.getString("AutoBroadcast.BroadcastCommand", "");
    }

}
