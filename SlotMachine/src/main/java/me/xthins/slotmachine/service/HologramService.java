package me.xthins.slotmachine.service;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.PlayerStats;
import me.xthins.slotmachine.util.ColorUtil;
import me.xthins.slotmachine.util.MoneyUtil;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

public class HologramService {
    private final SlotMachinePlugin plugin;

    public HologramService(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("holograms.enabled", true) && plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null;
    }

    public void updateAll() {
        if (!isEnabled()) return;
        updateWinners();
        updateFactions();
    }

    public void updateWinners() {
        Location location = plugin.getDataStore().getHologramLocation("winners");
        if (location == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(ColorUtil.color(plugin.getConfig().getString("holograms.winners-title", "&d&lTop Winners")));
        List<PlayerStats> top = plugin.getDataStore().allStats().stream()
                .sorted(Comparator.comparingDouble(PlayerStats::getProfit).reversed())
                .limit(10)
                .collect(Collectors.toList());
        if (top.isEmpty()) {
            lines.add(ColorUtil.color("&7No data yet."));
        } else {
            int rank = 1;
            for (PlayerStats stats : top) {
                String name = stats.getLastName() == null ? stats.getUuid().toString().substring(0, 8) : stats.getLastName();
                lines.add(ColorUtil.color("&f#" + rank + " &7" + name + " &8- &a" + MoneyUtil.formatMoneyShort(plugin.currencySymbol(), stats.getProfit())));
                rank++;
            }
        }
        recreate("winners", plugin.getConfig().getString("holograms.winners-id", "slotmachine_winners"), location, lines);
    }

    public void updateFactions() {
        Location location = plugin.getDataStore().getHologramLocation("factions");
        if (location == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(ColorUtil.color(plugin.getConfig().getString("holograms.factions-title", "&6&lTop Gambling Factions")));

        Map<String, Double> totals = new HashMap<>();
        for (PlayerStats stats : plugin.getDataStore().allStats()) {
            if (stats.getFactionTag() == null || stats.getFactionTag().isBlank()) continue;
            totals.merge(stats.getFactionTag(), stats.getProfit(), Double::sum);
        }
        List<Map.Entry<String, Double>> top = totals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .toList();
        if (top.isEmpty()) {
            lines.add(ColorUtil.color("&7No faction data yet."));
        } else {
            int rank = 1;
            for (Map.Entry<String, Double> entry : top) {
                lines.add(ColorUtil.color("&f#" + rank + " &7" + entry.getKey() + " &8- &6" + MoneyUtil.formatMoneyShort(plugin.currencySymbol(), entry.getValue())));
                rank++;
            }
        }
        recreate("factions", plugin.getConfig().getString("holograms.factions-id", "slotmachine_factions"), location, lines);
    }

    private void recreate(String type, String id, Location location, List<String> lines) {
        var server = plugin.getServer();
        String base = String.format(Locale.US, "-l:%s:%.2f:%.2f:%.2f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
        server.dispatchCommand(server.getConsoleSender(), "dh h remove " + id);
        server.dispatchCommand(server.getConsoleSender(), "dh create " + id + " " + base);
        for (int i = 0; i < lines.size(); i++) {
            String sanitized = lines.get(i).replace('"', '\'');
            if (i == 0) {
                server.dispatchCommand(server.getConsoleSender(), "dh l set " + id + " 1 1 " + sanitized);
            } else {
                server.dispatchCommand(server.getConsoleSender(), "dh l add " + id + " 1 " + sanitized);
            }
        }
        plugin.getLogger().fine("Updated hologram " + type);
    }
}
