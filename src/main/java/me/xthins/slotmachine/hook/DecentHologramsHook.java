package me.xthins.slotmachine.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class DecentHologramsHook {
    private final boolean enabled;

    public DecentHologramsHook(Plugin plugin) {
        this.enabled = plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null;
    }

    public boolean isEnabled() { return enabled; }

    public void createOrMove(String name, Location location, List<String> lines) {
        if (!enabled || location == null) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh remove " + name);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "dh create " + name + " " + location.getWorld().getName() + " " + location.getX() + " " + location.getY() + " " + location.getZ());
        for (int i = 0; i < lines.size(); i++) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh line add " + name + " " + i + " " + lines.get(i).replace(" ", "\\ "));
        }
    }

    public void delete(String name) {
        if (!enabled) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh remove " + name);
    }
}
