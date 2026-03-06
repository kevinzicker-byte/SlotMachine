package me.xthins.slotmachine.hook;

import me.xthins.slotmachine.SlotMachinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;

public class DecentHologramsHook {
    private final SlotMachinePlugin plugin;

    public DecentHologramsHook(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("DecentHolograms") != null;
    }

    public void createOrMove(String id, Location location) {
        if (!isAvailable() || location.getWorld() == null) return;
        String loc = location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh h delete " + id);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh h create " + id + " " + loc);
    }

    public void setLines(String id, List<String> lines) {
        if (!isAvailable()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh h delete " + id);
        Location location = plugin.getDataStore().hologramLocation(id.equals(plugin.getSettings().winnersHologramId()) ? "winners" : "factions");
        if (location == null || location.getWorld() == null) return;
        createOrMove(id, location);
        for (String line : lines) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh h addline " + id + " "" + line.replace(""", "") + """);
        }
    }
}
