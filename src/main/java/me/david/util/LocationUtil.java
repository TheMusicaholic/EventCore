package me.david.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;

@UtilityClass
public class LocationUtil {

    public Location fromString(String in) {
        final String[] parts = in.split("/");
        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    public String toString(Location location) {
        return location.getWorld().getName() + "/" + (location.getBlockX() + 0.5) + "/" + location.getBlockY() + "/" + (location.getBlockZ() + 0.5) + "/" + (Math.round(location.getYaw() / 45) * 45);
    }

}
