package me.david;

import lombok.AccessLevel;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.david.api.EventCoreAPI;
import me.david.command.BukkitCommand;
import me.david.command.impl.*;
import me.david.listener.*;
import me.david.listener.canvas.CanvasPlayerRespawnListener;
import me.david.listener.canvas.CanvasPlayerTeleportListener;
import me.david.manager.GameManager;
import me.david.manager.KitManager;
import me.david.manager.MapManager;
import me.david.util.*;
import me.david.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class EventCore extends JavaPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger("EventCore");

    @Getter
    private static EventCore instance;
    private volatile Settings settings;
    private UpdateChecker updateChecker;
    private MapManager mapManager;
    private GameManager gameManager;
    private KitManager kitManager;

    // Neither is released by the server when the plugin is disabled, see onDisable().
    @Getter(AccessLevel.NONE)
    private PlaceholderHook placeholderHook;
    @Getter(AccessLevel.NONE)
    private Metrics metrics;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        reloadConfig();

        updateChecker = new UpdateChecker(instance, "DavidArchive", "EventCore");
        updateChecker.check();

        mapManager = new MapManager();
        gameManager = new GameManager();
        kitManager = new KitManager();
        EventCoreAPI.initialize(instance, gameManager, kitManager, mapManager);

        new AnnouncementCommand(instance);
        new EventCommand(instance);
        new KitCommand(instance);
        new ReviveCommand();
        new SpawnCommand(instance);

        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(), instance);
        Bukkit.getPluginManager().registerEvents(new BlockExplodeListener(), instance);
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(), instance);
        Bukkit.getPluginManager().registerEvents(new CreatureSpawnListener(), instance);
        Bukkit.getPluginManager().registerEvents(new EntityDamageByEntityListener(), instance);
        Bukkit.getPluginManager().registerEvents(new EntityDamageListener(), instance);
        Bukkit.getPluginManager().registerEvents(new EntityExplodeListener(), instance);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(), instance);
        Bukkit.getPluginManager().registerEvents(new PlayerDropItemListener(), instance);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(), instance);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), instance);
        Bukkit.getPluginManager().registerEvents(new PlayerPickupItemListener(), instance);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), instance);

        if (FoliaScheduler.isCanvas()) {
            Bukkit.getPluginManager().registerEvents(new CanvasPlayerRespawnListener(), instance);
            Bukkit.getPluginManager().registerEvents(new CanvasPlayerTeleportListener(), instance);
        } else {
            Bukkit.getPluginManager().registerEvents(new PlayerRespawnListener(), instance);
            Bukkit.getPluginManager().registerEvents(new PlayerTeleportListener(), instance);
        }

        final boolean placeholderApi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (placeholderApi) {
            placeholderHook = new PlaceholderHook();
            placeholderHook.register();
        }

        final BorderUtil borderUtil = new BorderUtil();
        FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(instance, o -> borderUtil.run(), 20, 10);
        final AutoBroadcast autoBroadcast = new AutoBroadcast();
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(instance, o -> autoBroadcast.run(), 20, 20 * getConfig().getLong("AutoBroadcast.Interval", 60));
        FoliaScheduler.getGlobalRegionScheduler().runDelayed(instance, o -> {
            World world = mapManager.getSpawnLocation().getWorld();
            world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
            world.setDifficulty(Difficulty.PEACEFUL);
            world.getWorldBorder().setSize(BorderUtil.borderDefault);
            world.getWorldBorder().setDamageBuffer(BorderUtil.borderDamageBuffer);
            world.getWorldBorder().setDamageAmount(BorderUtil.borderDamageAmount);
        }, 40L);

        if (settings.isActionbarEnabled()) {
            FoliaScheduler.getAsyncScheduler().runAtFixedRate(instance, o -> {
                final String raw = settings.getActionbarMessage();

                // Without placeholders the text is the same for everyone, so translate it once.
                if (!placeholderApi || raw.indexOf('%') < 0) {
                    final Component actionbar = MessageUtil.translateColorCodes(raw);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(actionbar);
                    }
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    String parsed = PlaceholderAPI.setPlaceholders(player, raw);

                    player.sendActionBar(MessageUtil.translateColorCodes(parsed));
                }
            }, 0, 20);
        }

        if (getConfig().getBoolean("Settings.Metrics")) {
            metrics = new Metrics(instance, 28277);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        // Hot paths read these snapshots instead of walking the config, so refresh them on every (re)load.
        settings = new Settings(getConfig());
        BorderUtil.loadSettings(getConfig());
        MessageUtil.clearCache();
    }

    @Override
    public void onDisable() {
        try {
            if (gameManager != null && gameManager.isRunning()) {
                gameManager.stop(null);
            }
        } finally {
            // The server cancels tasks and unregisters listeners and services on its own, but none of these. Each
            // would keep the disabled plugin in memory (and working half-way) until the server stops.
            BukkitCommand.unregisterAll();
            if (placeholderHook != null) {
                placeholderHook.unregister();
                placeholderHook = null;
            }
            if (metrics != null) {
                metrics.shutdown();
                metrics = null;
            }
            EventCoreAPI.shutdown();
        }
    }

}
