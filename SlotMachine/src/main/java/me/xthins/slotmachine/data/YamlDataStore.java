package me.xthins.slotmachine.data;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.GambleProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class YamlDataStore implements DataStore {
    private final SlotMachinePlugin plugin;
    private final File file;
    private final Map<UUID, GambleProfile> profiles = new LinkedHashMap<>();
    private YamlConfiguration yaml;

    public YamlDataStore(SlotMachinePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    @Override
    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create data.yml: " + e.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        profiles.clear();

        if (yaml.getConfigurationSection("players") != null) {
            for (String key : yaml.getConfigurationSection("players").getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                String base = "players." + key + ".";
                GambleProfile profile = new GambleProfile(uuid, yaml.getString(base + "name", key));
                profile.setWagered(yaml.getDouble(base + "wagered", 0.0));
                profile.setWon(yaml.getDouble(base + "won", 0.0));
                profile.setWins(yaml.getInt(base + "wins", 0));
                profile.setLosses(yaml.getInt(base + "losses", 0));
                profile.setJackpots(yaml.getInt(base + "jackpots", 0));
                profile.setBiggestWin(yaml.getDouble(base + "biggestWin", 0.0));
                profiles.put(uuid, profile);
            }
        }

        if (!yaml.contains("jackpot.pot")) yaml.set("jackpot.pot", plugin.getSettings().jackpotBasePot());
        if (!yaml.contains("jackpot.history")) yaml.set("jackpot.history", new ArrayList<String>());
        if (!yaml.contains("happyHour.active")) yaml.set("happyHour.active", false);
        if (!yaml.contains("happyHour.multiplier")) yaml.set("happyHour.multiplier", 1.0);
        if (!yaml.contains("happyHour.minutesLeft")) yaml.set("happyHour.minutesLeft", 0);
        if (!yaml.contains("happyHour.nextStartIn")) yaml.set("happyHour.nextStartIn", plugin.getSettings().happyHourIntervalMinutes());
        if (!yaml.contains("meta.lastMilestoneBroadcast")) yaml.set("meta.lastMilestoneBroadcast", 0.0);
        save();
    }

    @Override
    public void save() {
        for (Map.Entry<UUID, GambleProfile> entry : profiles.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            GambleProfile profile = entry.getValue();
            yaml.set(base + "name", profile.getLastKnownName());
            yaml.set(base + "wagered", profile.getWagered());
            yaml.set(base + "won", profile.getWon());
            yaml.set(base + "wins", profile.getWins());
            yaml.set(base + "losses", profile.getLosses());
            yaml.set(base + "jackpots", profile.getJackpots());
            yaml.set(base + "biggestWin", profile.getBiggestWin());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }

    @Override public Map<UUID, GambleProfile> profiles() { return profiles; }
    @Override public GambleProfile profile(UUID uuid, String name) {
        return profiles.computeIfAbsent(uuid, id -> new GambleProfile(id, name));
    }
    @Override public double jackpotPot() { return yaml.getDouble("jackpot.pot", plugin.getSettings().jackpotBasePot()); }
    @Override public void setJackpotPot(double value) { yaml.set("jackpot.pot", value); }
    @Override public List<String> jackpotHistory() { return yaml.getStringList("jackpot.history"); }
    @Override public void addJackpotHistory(String entry) {
        List<String> history = jackpotHistory();
        history.add(0, entry);
        while (history.size() > 10) history.remove(history.size() - 1);
        yaml.set("jackpot.history", history);
    }
    @Override public boolean happyHourActive() { return yaml.getBoolean("happyHour.active", false); }
    @Override public void setHappyHourActive(boolean active) { yaml.set("happyHour.active", active); }
    @Override public double happyHourMultiplier() { return yaml.getDouble("happyHour.multiplier", 1.0); }
    @Override public void setHappyHourMultiplier(double multiplier) { yaml.set("happyHour.multiplier", multiplier); }
    @Override public int happyHourMinutesLeft() { return yaml.getInt("happyHour.minutesLeft", 0); }
    @Override public void setHappyHourMinutesLeft(int minutes) { yaml.set("happyHour.minutesLeft", minutes); }
    @Override public int nextHappyHourStartIn() { return yaml.getInt("happyHour.nextStartIn", plugin.getSettings().happyHourIntervalMinutes()); }
    @Override public void setNextHappyHourStartIn(int minutes) { yaml.set("happyHour.nextStartIn", minutes); }

    @Override
    public Location hologramLocation(String key) {
        String base = "holograms." + key + ".";
        if (!yaml.contains(base + "world")) return null;
        World world = Bukkit.getWorld(yaml.getString(base + "world", ""));
        if (world == null) return null;
        return new Location(world,
                yaml.getDouble(base + "x"),
                yaml.getDouble(base + "y"),
                yaml.getDouble(base + "z"),
                (float) yaml.getDouble(base + "yaw", 0.0),
                (float) yaml.getDouble(base + "pitch", 0.0));
    }

    @Override
    public void setHologramLocation(String key, Location location) {
        String base = "holograms." + key + ".";
        yaml.set(base + "world", location.getWorld() == null ? null : location.getWorld().getName());
        yaml.set(base + "x", location.getX());
        yaml.set(base + "y", location.getY());
        yaml.set(base + "z", location.getZ());
        yaml.set(base + "yaw", location.getYaw());
        yaml.set(base + "pitch", location.getPitch());
    }

    @Override public double lastMilestoneBroadcast() { return yaml.getDouble("meta.lastMilestoneBroadcast", 0.0); }
    @Override public void setLastMilestoneBroadcast(double value) { yaml.set("meta.lastMilestoneBroadcast", value); }
}
