package me.david.util;

import lombok.experimental.UtilityClass;
import me.david.EventCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Keeps a player's own state (items, ender chest, game mode, health, hunger, experience and effects) while they're
 * in the event world, together with where they came from. It's stored in the player's persistent data, so the server
 * saves it in the same file and at the same time as their inventory: a crash or restart can never lose one but not
 * the other.
 * <p>
 * All methods have to run on the player's thread.
 */
@UtilityClass
public class PlayerSnapshot {

    private final NamespacedKey SNAPSHOT = key("snapshot");
    private final NamespacedKey FAILED_SNAPSHOT = key("failed_snapshot");
    private final NamespacedKey INVENTORY = key("inventory");
    private final NamespacedKey ENDER_CHEST = key("ender_chest");
    private final NamespacedKey GAME_MODE = key("game_mode");
    private final NamespacedKey HEALTH = key("health");
    private final NamespacedKey FOOD = key("food");
    private final NamespacedKey SATURATION = key("saturation");
    private final NamespacedKey LEVEL = key("level");
    private final NamespacedKey EXP = key("exp");
    private final NamespacedKey ALLOW_FLIGHT = key("allow_flight");
    private final NamespacedKey FLYING = key("flying");
    private final NamespacedKey EFFECTS = key("effects");
    private final NamespacedKey EFFECT_TYPE = key("type");
    private final NamespacedKey EFFECT_DURATION = key("duration");
    private final NamespacedKey EFFECT_AMPLIFIER = key("amplifier");
    private final NamespacedKey EFFECT_AMBIENT = key("ambient");
    private final NamespacedKey EFFECT_PARTICLES = key("particles");
    private final NamespacedKey EFFECT_ICON = key("icon");
    private final NamespacedKey RETURN_LOCATION = key("return_location");

    public boolean has(@NotNull Player player) {
        return player.getPersistentDataContainer().has(SNAPSHOT, PersistentDataType.TAG_CONTAINER);
    }

    /**
     * Puts the player's current state away. Doesn't change the player: clearing them is up to the caller.
     *
     * @param returnLocation where the player is sent back to when they leave the event, if anywhere
     */
    public void save(@NotNull Player player, @Nullable Location returnLocation) {
        final PersistentDataContainer data = player.getPersistentDataContainer();
        final PersistentDataContainer snapshot = data.getAdapterContext().newPersistentDataContainer();

        snapshot.set(INVENTORY, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(player.getInventory().getContents()));
        snapshot.set(ENDER_CHEST, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(player.getEnderChest().getContents()));
        snapshot.set(GAME_MODE, PersistentDataType.STRING, player.getGameMode().name());
        snapshot.set(HEALTH, PersistentDataType.DOUBLE, player.getHealth());
        snapshot.set(FOOD, PersistentDataType.INTEGER, player.getFoodLevel());
        snapshot.set(SATURATION, PersistentDataType.FLOAT, player.getSaturation());
        snapshot.set(LEVEL, PersistentDataType.INTEGER, player.getLevel());
        snapshot.set(EXP, PersistentDataType.FLOAT, player.getExp());
        snapshot.set(ALLOW_FLIGHT, PersistentDataType.BOOLEAN, player.getAllowFlight());
        snapshot.set(FLYING, PersistentDataType.BOOLEAN, player.isFlying());

        final List<PersistentDataContainer> effects = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            final PersistentDataContainer entry = data.getAdapterContext().newPersistentDataContainer();
            entry.set(EFFECT_TYPE, PersistentDataType.STRING, effect.getType().getKey().asString());
            entry.set(EFFECT_DURATION, PersistentDataType.INTEGER, effect.getDuration());
            entry.set(EFFECT_AMPLIFIER, PersistentDataType.INTEGER, effect.getAmplifier());
            entry.set(EFFECT_AMBIENT, PersistentDataType.BOOLEAN, effect.isAmbient());
            entry.set(EFFECT_PARTICLES, PersistentDataType.BOOLEAN, effect.hasParticles());
            entry.set(EFFECT_ICON, PersistentDataType.BOOLEAN, effect.hasIcon());
            effects.add(entry);
        }
        if (!effects.isEmpty()) {
            snapshot.set(EFFECTS, PersistentDataType.LIST.dataContainers(), effects);
        }
        if (returnLocation != null) {
            snapshot.set(RETURN_LOCATION, PersistentDataType.STRING, toString(returnLocation));
        }

        data.set(SNAPSHOT, PersistentDataType.TAG_CONTAINER, snapshot);
    }

    /**
     * Where the player came from before entering the event world, or null if there's no snapshot or that world is
     * gone.
     */
    public @Nullable Location getReturnLocation(@NotNull Player player) {
        final PersistentDataContainer snapshot = player.getPersistentDataContainer().get(SNAPSHOT, PersistentDataType.TAG_CONTAINER);
        if (snapshot == null) return null;

        final String location = snapshot.get(RETURN_LOCATION, PersistentDataType.STRING);
        return location != null ? fromString(location) : null;
    }

    /**
     * Gives the player their saved state back, in place of whatever they have now, and removes the snapshot. Does
     * nothing if there's no snapshot.
     */
    public void restore(@NotNull Player player) {
        final PersistentDataContainer data = player.getPersistentDataContainer();
        final PersistentDataContainer snapshot = data.get(SNAPSHOT, PersistentDataType.TAG_CONTAINER);
        if (snapshot == null) return;

        ItemStack[] inventory;
        ItemStack[] enderChest;
        try {
            inventory = readItems(snapshot, INVENTORY, player.getInventory());
            enderChest = readItems(snapshot, ENDER_CHEST, player.getEnderChest());
        } catch (Exception e) {
            // The player still mustn't keep the event items. The snapshot is kept under another key, so the items
            // aren't lost but also aren't tried (and the player cleared) again on every world change.
            EventCore.LOGGER.error("Failed to restore the items of {}, they're kept in their persistent data under {}", player.getName(), FAILED_SNAPSHOT, e);
            data.set(FAILED_SNAPSHOT, PersistentDataType.TAG_CONTAINER, snapshot);
            inventory = new ItemStack[player.getInventory().getSize()];
            enderChest = new ItemStack[player.getEnderChest().getSize()];
        }

        player.getInventory().setContents(inventory);
        player.getEnderChest().setContents(enderChest);

        // The game mode first: changing it resets flight.
        player.setGameMode(readGameMode(snapshot));
        player.setAllowFlight(snapshot.getOrDefault(ALLOW_FLIGHT, PersistentDataType.BOOLEAN, false));
        player.setFlying(player.getAllowFlight() && snapshot.getOrDefault(FLYING, PersistentDataType.BOOLEAN, false));

        final AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        final double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20;
        final double health = snapshot.getOrDefault(HEALTH, PersistentDataType.DOUBLE, maxHealth);
        player.setHealth(health > 0 ? Math.min(health, maxHealth) : maxHealth);
        player.setFoodLevel(snapshot.getOrDefault(FOOD, PersistentDataType.INTEGER, 20));
        player.setSaturation(snapshot.getOrDefault(SATURATION, PersistentDataType.FLOAT, 5F));
        player.setLevel(snapshot.getOrDefault(LEVEL, PersistentDataType.INTEGER, 0));
        player.setExp(snapshot.getOrDefault(EXP, PersistentDataType.FLOAT, 0F));
        player.setFireTicks(0);
        player.setFallDistance(0);

        player.clearActivePotionEffects();
        player.addPotionEffects(readEffects(snapshot));

        data.remove(SNAPSHOT);
    }

    private ItemStack @NotNull [] readItems(@NotNull PersistentDataContainer snapshot, @NotNull NamespacedKey key, @NotNull Inventory inventory) {
        final byte[] bytes = snapshot.get(key, PersistentDataType.BYTE_ARRAY);
        if (bytes == null) return new ItemStack[inventory.getSize()];

        // setContents rejects more items than the inventory has slots.
        final ItemStack[] items = ItemStack.deserializeItemsFromBytes(bytes);
        return items.length > inventory.getSize() ? Arrays.copyOf(items, inventory.getSize()) : items;
    }

    private @NotNull GameMode readGameMode(@NotNull PersistentDataContainer snapshot) {
        final String gameMode = snapshot.get(GAME_MODE, PersistentDataType.STRING);
        try {
            return gameMode != null ? GameMode.valueOf(gameMode) : Bukkit.getDefaultGameMode();
        } catch (IllegalArgumentException e) {
            return Bukkit.getDefaultGameMode();
        }
    }

    private @NotNull List<PotionEffect> readEffects(@NotNull PersistentDataContainer snapshot) {
        final List<PotionEffect> effects = new ArrayList<>();
        for (PersistentDataContainer entry : snapshot.getOrDefault(EFFECTS, PersistentDataType.LIST.dataContainers(), List.of())) {
            final String typeKey = entry.get(EFFECT_TYPE, PersistentDataType.STRING);
            final NamespacedKey key = typeKey != null ? NamespacedKey.fromString(typeKey) : null;
            final PotionEffectType type = key != null ? Registry.MOB_EFFECT.get(key) : null;
            if (type == null) continue;

            effects.add(new PotionEffect(
                    type,
                    entry.getOrDefault(EFFECT_DURATION, PersistentDataType.INTEGER, 0),
                    entry.getOrDefault(EFFECT_AMPLIFIER, PersistentDataType.INTEGER, 0),
                    entry.getOrDefault(EFFECT_AMBIENT, PersistentDataType.BOOLEAN, false),
                    entry.getOrDefault(EFFECT_PARTICLES, PersistentDataType.BOOLEAN, true),
                    entry.getOrDefault(EFFECT_ICON, PersistentDataType.BOOLEAN, true)
            ));
        }
        return effects;
    }

    // Unlike LocationUtil, which rounds spawn points to the block, this keeps the exact position and view direction.
    private @NotNull String toString(@NotNull Location location) {
        return location.getWorld().getName() + "/" + location.getX() + "/" + location.getY() + "/" + location.getZ() + "/" + location.getYaw() + "/" + location.getPitch();
    }

    private @Nullable Location fromString(@NotNull String location) {
        final String[] parts = location.split("/");
        if (parts.length != 6) return null;

        final World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        try {
            return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private @NotNull NamespacedKey key(@NotNull String key) {
        return new NamespacedKey(EventCore.getInstance(), key);
    }

}
