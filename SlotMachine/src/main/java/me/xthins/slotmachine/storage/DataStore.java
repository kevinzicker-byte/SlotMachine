package me.xthins.slotmachine.storage;

import me.xthins.slotmachine.model.JackpotRecord;
import me.xthins.slotmachine.model.PlayerStats;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class DataStore {

    private final JavaPlugin plugin;
    private final File dataFile;
    private final File holoFile;
    private YamlConfiguration data;
    private YamlConfiguration holograms;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final List<JackpotRecord> jackpotHistory = new ArrayList<>();
    private double jackpotPot;
    private boolean happyHourActive;
    private int happyHourMinutesLeft;
    private int happyHourNextStartIn;
    private double happyHourMultiplier;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.holoFile = new File(plugin.getDataFolder(), "holograms.yml");
    }

    public void load(double defaultPot, int defaultNextStart) {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Failed to create data.yml", e); }
        }
        if (!holoFile.exists()) {
            try { holoFile.createNewFile(); } catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Failed to create holograms.yml", e); }
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        this.holograms = YamlConfiguration.loadConfiguration(holoFile);

        this.jackpotPot = data.getDouble("jackpot.pot", defaultPot);
        this.happyHourActive = data.getBoolean("happyhour.active", false);
        this.happyHourMinutesLeft = data.getInt("happyhour.minutesLeft", 0);
        this.happyHourNextStartIn = data.getInt("happyhour.nextStartIn", defaultNextStart);
        this.happyHourMultiplier = data.getDouble("happyhour.multiplier", 1.0);

        playerStats.clear();
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                UUID uuid;
                try { uuid = UUID.fromString(key); } catch (IllegalArgumentException ex) { continue; }
                ConfigurationSection sec = players.getConfigurationSection(key);
                if (sec == null) continue;
                PlayerStats stats = new PlayerStats(uuid);
                stats.setLastName(sec.getString("name"));
                stats.setFactionTag(sec.getString("faction"));
                for (int i = 0; i < sec.getLong("bets", 0); i++) stats.addBet();
                stats.addWagered(sec.getDouble("wagered", 0));
                stats.addWon(sec.getDouble("won", 0));
                for (int i = 0; i < sec.getLong("wins", 0); i++) stats.addWin();
                for (int i = 0; i < sec.getLong("losses", 0); i++) stats.addLoss();
                stats.setBiggestWin(sec.getDouble("biggest", 0));
                for (int i = 0; i < sec.getLong("jackpots", 0); i++) stats.addJackpot();
                playerStats.put(uuid, stats);
            }
        }

        jackpotHistory.clear();
        List<Map<?, ?>> list = data.getMapList("jackpot.history");
        for (Map<?, ?> map : list) {
            Object player = map.get("player");
            Object amount = map.get("amount");
            Object ts = map.get("timestamp");
            if (player instanceof String p && amount instanceof Number a && ts instanceof Number t) {
                jackpotHistory.add(new JackpotRecord(p, a.doubleValue(), t.longValue()));
            }
        }
    }

    public void save() {
        data.set("jackpot.pot", jackpotPot);
        data.set("happyhour.active", happyHourActive);
        data.set("happyhour.minutesLeft", happyHourMinutesLeft);
        data.set("happyhour.nextStartIn", happyHourNextStartIn);
        data.set("happyhour.multiplier", happyHourMultiplier);

        data.set("players", null);
        for (PlayerStats stats : playerStats.values()) {
            String path = "players." + stats.getUuid();
            data.set(path + ".name", stats.getLastName());
            data.set(path + ".faction", stats.getFactionTag());
            data.set(path + ".bets", stats.getBets());
            data.set(path + ".wagered", stats.getWagered());
            data.set(path + ".won", stats.getWon());
            data.set(path + ".wins", stats.getWins());
            data.set(path + ".losses", stats.getLosses());
            data.set(path + ".biggest", stats.getBiggestWin());
            data.set(path + ".jackpots", stats.getJackpots());
        }

        List<Map<String, Object>> history = new ArrayList<>();
        for (JackpotRecord record : jackpotHistory) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("player", record.playerName());
            row.put("amount", record.amount());
            row.put("timestamp", record.timestamp());
            history.add(row);
        }
        data.set("jackpot.history", history);

        try {
            data.save(dataFile);
            holograms.save(holoFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save YAML files", e);
        }
    }

    public PlayerStats getOrCreate(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, PlayerStats::new);
    }

    public Collection<PlayerStats> allStats() { return playerStats.values(); }
    public double getJackpotPot() { return jackpotPot; }
    public void setJackpotPot(double jackpotPot) { this.jackpotPot = jackpotPot; }
    public List<JackpotRecord> getJackpotHistory() { return jackpotHistory; }
    public void addJackpotRecord(JackpotRecord record, int max) {
        jackpotHistory.add(0, record);
        while (jackpotHistory.size() > max) jackpotHistory.remove(jackpotHistory.size() - 1);
    }
    public boolean isHappyHourActive() { return happyHourActive; }
    public void setHappyHourActive(boolean happyHourActive) { this.happyHourActive = happyHourActive; }
    public int getHappyHourMinutesLeft() { return happyHourMinutesLeft; }
    public void setHappyHourMinutesLeft(int happyHourMinutesLeft) { this.happyHourMinutesLeft = happyHourMinutesLeft; }
    public int getHappyHourNextStartIn() { return happyHourNextStartIn; }
    public void setHappyHourNextStartIn(int happyHourNextStartIn) { this.happyHourNextStartIn = happyHourNextStartIn; }
    public double getHappyHourMultiplier() { return happyHourMultiplier; }
    public void setHappyHourMultiplier(double happyHourMultiplier) { this.happyHourMultiplier = happyHourMultiplier; }

    public void setHologramLocation(String key, Location location) {
        holograms.set(key + ".world", location.getWorld().getName());
        holograms.set(key + ".x", location.getX());
        holograms.set(key + ".y", location.getY());
        holograms.set(key + ".z", location.getZ());
    }

    public Location getHologramLocation(String key) {
        String world = holograms.getString(key + ".world");
        if (world == null || plugin.getServer().getWorld(world) == null) return null;
        return new Location(plugin.getServer().getWorld(world), holograms.getDouble(key + ".x"), holograms.getDouble(key + ".y"), holograms.getDouble(key + ".z"));
    }
}
