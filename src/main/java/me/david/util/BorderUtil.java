package me.david.util;

import me.david.EventCore;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public class BorderUtil implements Runnable {

    public static volatile int borderDefault = 200;
    public static volatile double borderDamageBuffer = 0.0;
    public static volatile double borderDamageAmount = 0.2;
    public static volatile int lastOptimal = borderDefault;
    public static volatile boolean autoBorder;

    public BorderUtil() {
        lastOptimal = borderDefault;
    }

    public static void loadSettings(@NotNull final FileConfiguration config) {
        borderDefault = config.getInt("Settings.WorldBorder.DefaultSize", borderDefault);
        borderDamageBuffer = config.getDouble("Settings.WorldBorder.Damage.Buffer", borderDamageBuffer);
        borderDamageAmount = config.getDouble("Settings.WorldBorder.Damage.Amount", borderDamageAmount);
        autoBorder = config.getBoolean("Settings.WorldBorder.AutoBorder", false);
    }

    public static void setAutoBorder(boolean value) {
        autoBorder = value;
        EventCore.getInstance().getConfig().set("Settings.WorldBorder.AutoBorder", value);
        EventCore.getInstance().saveConfig();
    }


    // Runs on the global region, so the border can be changed directly without another scheduler hop.
    @Override
    public void run() {
        if (!EventCore.getInstance().getGameManager().isRunning() || !autoBorder) return;

        final int optimal = getOptimalSize();
        // Only shrink, and only send the (world-wide) border update when the target actually changes.
        if (lastOptimal <= optimal) return;

        lastOptimal = optimal;
        final WorldBorder worldBorder = EventCore.getInstance().getMapManager().getSpawnLocation().getWorld().getWorldBorder();
        worldBorder.changeSize(optimal, (long) (worldBorder.getSize() - optimal) * 20);
    }

    private int getOptimalSize() {
        final int alive = PlayerUtil.getAlive();
        int optimal = (int) (((Math.pow(alive, 2)) / 60 + 4 + 0.6 * alive) * 2);
        return Math.min(200, optimal);
    }

}
