package me.xthins.slotmachine.storage;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.PlayerStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlDataStore {
    private final SlotMachinePlugin plugin;
    private final File file;
    private YamlConfiguration yaml;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private final List<String> jackpotHistory = new ArrayList<>();
    private final Set<Double> announcedMilestones = new HashSet<>();
    private double jackpot;
    private boolean happyHourActive;
    private int happyHourMinutesLeft;
    private int happyHourNextStartIn;

    public YamlDataStore(SlotMachinePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        jackpot = yaml.getDouble("jackpot", plugin.getConfig().getDouble("settings.jackpot-starting-pot", 10000000));
        happyHourActive = yaml.getBoolean("happy-hour.active", false);
        happyHourMinutesLeft = yaml.getInt("happy-hour.minutes-left", 0);
        happyHourNextStartIn = yaml.getInt("happy-hour.next-start-in", plugin.getConfig().getInt("happy-hour.interval-minutes", 60));
        jackpotHistory.clear();
        jackpotHistory.addAll(yaml.getStringList("jackpot-history"));
        stats.clear();
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    ConfigurationSection sec = players.getConfigurationSection(key);
                    if (sec == null) continue;
                    PlayerStats ps = new PlayerStats();
                    ps.setPlayerName(sec.getString("name"));
                    ps.setLastKnownFaction(sec.getString("faction"));
                    ps.setWagered(sec.getDouble("wagered"));
                    ps.setWon(sec.getDouble("won"));
                    ps.setWins(sec.getInt("wins"));
                    ps.setLosses(sec.getInt("losses"));
                    ps.setBiggestWin(sec.getDouble("biggest"));
                    ps.setJackpots(sec.getInt("jackpots"));
                    stats.put(uuid, ps);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        if (yaml == null) yaml = new YamlConfiguration();
        yaml.set("jackpot", jackpot);
        yaml.set("happy-hour.active", happyHourActive);
        yaml.set("happy-hour.minutes-left", happyHourMinutesLeft);
        yaml.set("happy-hour.next-start-in", happyHourNextStartIn);
        yaml.set("jackpot-history", jackpotHistory);
        yaml.set("players", null);
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String p = "players." + entry.getKey();
            PlayerStats s = entry.getValue();
            yaml.set(p + ".name", s.getPlayerName());
            yaml.set(p + ".faction", s.getLastKnownFaction());
            yaml.set(p + ".wagered", s.getWagered());
            yaml.set(p + ".won", s.getWon());
            yaml.set(p + ".wins", s.getWins());
            yaml.set(p + ".losses", s.getLosses());
            yaml.set(p + ".biggest", s.getBiggestWin());
            yaml.set(p + ".jackpots", s.getJackpots());
        }
        try { yaml.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public PlayerStats getStats(UUID uuid) { return stats.computeIfAbsent(uuid, k -> new PlayerStats()); }
    public Map<UUID, PlayerStats> getAllStats() { return Collections.unmodifiableMap(stats); }
    public double getJackpot() { return jackpot; }
    public void setJackpot(double jackpot) { this.jackpot = jackpot; }
    public List<String> getJackpotHistory() { return jackpotHistory; }
    public void addJackpotHistory(String line) { jackpotHistory.add(0, line); while (jackpotHistory.size() > 10) jackpotHistory.remove(jackpotHistory.size()-1); }
    public boolean isHappyHourActive() { return happyHourActive; }
    public void setHappyHourActive(boolean b) { this.happyHourActive = b; }
    public int getHappyHourMinutesLeft() { return happyHourMinutesLeft; }
    public void setHappyHourMinutesLeft(int v) { this.happyHourMinutesLeft = v; }
    public int getHappyHourNextStartIn() { return happyHourNextStartIn; }
    public void setHappyHourNextStartIn(int v) { this.happyHourNextStartIn = v; }
    public Set<Double> getAnnouncedMilestones() { return announcedMilestones; }
}
