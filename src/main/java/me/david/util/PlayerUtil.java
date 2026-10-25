package me.david.util;

import lombok.experimental.UtilityClass;
import me.david.EventCore;
import me.david.manager.EventWorldManager;
import me.david.util.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@UtilityClass
public class PlayerUtil {

    // Scoreboards and tab lists can request %eventcore_alive% and %eventcore_total% for every player on every refresh,
    // and counting all players each time makes a refresh O(players²). The counts are reused for one tick instead.
    private final long COUNT_CACHE_NANOS = TimeUnit.MILLISECONDS.toNanos(50);
    private volatile int aliveCount;
    private volatile int totalCount;
    private volatile long countExpiry = System.nanoTime();

    /**
     * The players in the event world who are still in the game (in survival mode).
     */
    public int getAlive() {
        refreshCounts();
        return aliveCount;
    }

    /**
     * The players in the event world.
     */
    public int getTotal() {
        refreshCounts();
        return totalCount;
    }

    private void refreshCounts() {
        final long now = System.nanoTime();
        if (now - countExpiry < 0) return;

        final EventWorldManager eventWorld = EventCore.getInstance().getEventWorldManager();
        int alive = 0;
        int total = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!eventWorld.isInEventWorld(player)) continue;

            total++;
            if (player.getGameMode() == GameMode.SURVIVAL) alive++;
        }
        // Written before the expiry, so a caller that sees the new expiry also sees these counts.
        aliveCount = alive;
        totalCount = total;
        countExpiry = now + COUNT_CACHE_NANOS;
    }

    /**
     * Readies the player to play: survival mode, full health and food, no fire or effects, and the enabled kit.
     */
    public void cleanPlayer(@NotNull Player player) {
        resetPlayer(player, GameMode.SURVIVAL);
    }

    /**
     * Resets the player like {@link #cleanPlayer(Player)}, but into the given game mode. Only survival players get
     * the kit: spectators can't use it, and it is replaced anyway when they're revived or the game stops.
     */
    public void resetPlayer(@NotNull Player player, @NotNull GameMode gameMode) {
        // Switching straight to the final mode (instead of to survival first) sends one game mode update to every
        // player instead of two.
        player.setGameMode(gameMode);
        final AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth != null ? maxHealth.getValue() : 20);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.clearActivePotionEffects();

        final PlayerInventory inventory = player.getInventory();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.clear();

        if (gameMode == GameMode.SURVIVAL) {
            // Just cleared, so the kit doesn't have to clear it a second time.
            EventCore.getInstance().getKitManager().give(player, false);
        }
    }

    /**
     * Runs the action on the thread that owns the player: right away on Paper (and on Folia when called from the
     * player's region), otherwise on the player's next tick. On Folia, commands and tasks often run on another
     * thread than the players they change, and players may only be changed from their own region's thread.
     */
    public void runFor(@NotNull Player player, @NotNull Consumer<Player> action) {
        FoliaScheduler.getEntityScheduler().runNowOrSchedule(player, EventCore.getInstance(), () -> action.accept(player));
    }

    /**
     * {@link #runFor(Player, Consumer)} for every player in the event world.
     */
    public void runForEventPlayers(@NotNull Consumer<Player> action) {
        for (Player player : EventCore.getInstance().getEventWorldManager().getPlayers()) {
            runFor(player, action);
        }
    }

}
