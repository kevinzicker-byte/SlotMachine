package me.xthins.slotmachine.service;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.GambleProfile;
import me.xthins.slotmachine.util.MoneyUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardService {
    private final SlotMachinePlugin plugin;

    public LeaderboardService(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> topWinnersLines(int max) {
        List<GambleProfile> profiles = new ArrayList<>(plugin.getDataStore().profiles().values());
        profiles.sort(Comparator.comparingDouble(GambleProfile::getProfit).reversed());
        List<String> lines = new ArrayList<>();
        lines.add(plugin.getSettings().winnersHologramTitle());
        for (int i = 0; i < Math.min(max, profiles.size()); i++) {
            GambleProfile profile = profiles.get(i);
            lines.add("&f#" + (i + 1) + " &7" + profile.getLastKnownName() + " &8- &a" + MoneyUtil.money(profile.getProfit(), true, plugin.getSettings().currencySymbol()));
        }
        if (lines.size() == 1) lines.add("&7No data yet.");
        return lines;
    }

    public List<String> topFactionLines(int max) {
        Map<String, Double> totals = new LinkedHashMap<>();
        plugin.getDataStore().profiles().forEach((uuid, profile) -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String faction = plugin.getFactionHook().getFactionName(player);
            if (faction == null || faction.isBlank() || faction.equalsIgnoreCase("Wilderness")) return;
            totals.put(faction, totals.getOrDefault(faction, 0.0) + profile.getProfit());
        });

        List<Map.Entry<String, Double>> entries = new ArrayList<>(totals.entrySet());
        entries.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        List<String> lines = new ArrayList<>();
        lines.add(plugin.getSettings().factionsHologramTitle());
        for (int i = 0; i < Math.min(max, entries.size()); i++) {
            Map.Entry<String, Double> entry = entries.get(i);
            lines.add("&f#" + (i + 1) + " &6" + entry.getKey() + " &8- &e" + MoneyUtil.money(entry.getValue(), true, plugin.getSettings().currencySymbol()));
        }
        if (lines.size() == 1) lines.add("&7No data yet.");
        return lines;
    }
}
