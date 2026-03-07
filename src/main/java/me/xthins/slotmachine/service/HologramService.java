package me.xthins.slotmachine.service;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.hook.DecentHologramsHook;
import me.xthins.slotmachine.hook.SaberFactionsHook;
import me.xthins.slotmachine.model.PlayerStats;
import me.xthins.slotmachine.storage.YamlDataStore;
import me.xthins.slotmachine.util.ColorUtil;
import me.xthins.slotmachine.util.MoneyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;

public class HologramService {
    private final SlotMachinePlugin plugin;
    private final DecentHologramsHook hook;
    private final SaberFactionsHook factionsHook;
    private final YamlDataStore dataStore;
    private final HappyHourService happyHourService;
    private int taskId = -1;

    public HologramService(SlotMachinePlugin plugin, DecentHologramsHook hook, SaberFactionsHook factionsHook, YamlDataStore dataStore, HappyHourService happyHourService) {
        this.plugin = plugin;
        this.hook = hook;
        this.factionsHook = factionsHook;
        this.dataStore = dataStore;
        this.happyHourService = happyHourService;
    }

    public void start() {
        if (!hook.isEnabled() || !plugin.getHologramsConfig().getBoolean("enabled", true)) return;
        stop();
        long interval = plugin.getHologramsConfig().getInt("update-interval-seconds", 30) * 20L;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAll, 20L, interval);
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    public void updateAll() {
        updateWinners();
        updateFactions();
        updateJackpot();
    }

    public void create(String type) { switch (type.toLowerCase()) { case "winners" -> updateWinners(); case "factions" -> updateFactions(); case "jackpot" -> updateJackpot(); } }
    public void delete(String type) { hook.delete(plugin.getHologramsConfig().getString(type + ".name", "slot_" + type)); }

    private void updateWinners() {
        if (!plugin.getHologramsConfig().getBoolean("winners.enabled", true)) return;
        Location loc = location("winners"); if (loc == null) return;
        String name = plugin.getHologramsConfig().getString("winners.name", "slot_winners");
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(ColorUtil.color(plugin.getHologramsConfig().getString("winners.title", "&d&lTop Winners")));
        String format = plugin.getHologramsConfig().getString("winners.entry", "&f#{rank} &d{player} &7- &a{amount}");
        java.util.List<Map.Entry<UUID, PlayerStats>> list = new java.util.ArrayList<>(dataStore.getAllStats().entrySet());
        list.sort((a,b)->Double.compare(b.getValue().getProfit(), a.getValue().getProfit()));
        int max = plugin.getHologramsConfig().getInt("winners.max-lines", 10);
        int rank = 0;
        for (Map.Entry<UUID, PlayerStats> e : list) {
            rank++; if (rank > max) break;
            String line = format.replace("{rank}", String.valueOf(rank))
                    .replace("{player}", e.getValue().getPlayerName() == null ? e.getKey().toString() : e.getValue().getPlayerName())
                    .replace("{amount}", MoneyUtil.moneyShort(e.getValue().getProfit()));
            lines.add(ColorUtil.color(line));
        }
        if (lines.size() == 1) lines.add(ColorUtil.color("&7No data yet."));
        hook.createOrMove(name, loc, lines);
    }

    private void updateFactions() {
        if (!plugin.getHologramsConfig().getBoolean("factions.enabled", true)) return;
        Location loc = location("factions"); if (loc == null) return;
        String name = plugin.getHologramsConfig().getString("factions.name", "slot_factions");
        String format = plugin.getHologramsConfig().getString("factions.entry", "&f#{rank} &e{faction} &7- &a{amount}");
        Map<String, Double> totals = new HashMap<>();
        for (Map.Entry<UUID, PlayerStats> e : dataStore.getAllStats().entrySet()) {
            PlayerStats s = e.getValue();
            String faction = s.getLastKnownFaction();
            if ((faction == null || faction.isBlank()) && Bukkit.getPlayer(e.getKey()) != null && factionsHook.isEnabled()) {
                faction = factionsHook.getFactionTag(Bukkit.getPlayer(e.getKey()));
            }
            if (faction == null || faction.isBlank()) faction = "No Faction";
            totals.put(faction, totals.getOrDefault(faction, 0.0) + s.getProfit());
        }
        java.util.List<Map.Entry<String, Double>> list = new java.util.ArrayList<>(totals.entrySet());
        list.sort((a,b)->Double.compare(b.getValue(), a.getValue()));
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(ColorUtil.color(plugin.getHologramsConfig().getString("factions.title", "&6&lTop Gambling Factions")));
        int max = plugin.getHologramsConfig().getInt("factions.max-lines", 10);
        int rank = 0;
        for (Map.Entry<String, Double> e : list) {
            rank++; if (rank > max) break;
            lines.add(ColorUtil.color(format.replace("{rank}", String.valueOf(rank)).replace("{faction}", e.getKey()).replace("{amount}", MoneyUtil.moneyShort(e.getValue()))));
        }
        if (lines.size() == 1) lines.add(ColorUtil.color("&7No data yet."));
        hook.createOrMove(name, loc, lines);
    }

    private void updateJackpot() {
        if (!plugin.getHologramsConfig().getBoolean("jackpot.enabled", true)) return;
        Location loc = location("jackpot"); if (loc == null) return;
        String name = plugin.getHologramsConfig().getString("jackpot.name", "slot_jackpot");
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(ColorUtil.color(plugin.getHologramsConfig().getString("jackpot.title", "&5&lCurrent Jackpot")));
        lines.add(ColorUtil.color(plugin.getHologramsConfig().getString("jackpot.value", "&d{jackpot}").replace("{jackpot}", MoneyUtil.moneyShort(dataStore.getJackpot()))));
        lines.add(ColorUtil.color(happyHourService.isActive() ? plugin.getHologramsConfig().getString("jackpot.happy-on", "&7Happy Hour: &aACTIVE") : plugin.getHologramsConfig().getString("jackpot.happy-off", "&7Happy Hour: &cOFF")));
        hook.createOrMove(name, loc, lines);
    }

    private Location location(String path) {
        String world = plugin.getHologramsConfig().getString(path + ".world");
        if (world == null || Bukkit.getWorld(world) == null) return null;
        return new Location(Bukkit.getWorld(world), plugin.getHologramsConfig().getDouble(path + ".x"), plugin.getHologramsConfig().getDouble(path + ".y"), plugin.getHologramsConfig().getDouble(path + ".z"));
    }
}
