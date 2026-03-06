package me.xthins.slotmachine.command;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.PlayerStats;
import me.xthins.slotmachine.util.ColorUtil;
import me.xthins.slotmachine.util.MoneyUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class GambleCommand implements CommandExecutor, TabCompleter {
    private final SlotMachinePlugin plugin;

    public GambleCommand(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(plugin.message("player-only")));
            return true;
        }
        if (!player.hasPermission("slotmachine.use")) {
            player.sendMessage(ColorUtil.color(plugin.message("no-permission")));
            return true;
        }
        if (args.length == 0) {
            plugin.getSlotMachineService().openMenu(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "top" -> {
                player.sendMessage(ColorUtil.color("&6Top Profit"));
                int rank = 1;
                for (PlayerStats stats : plugin.getSlotMachineService().topPlayers()) {
                    if (rank > 10) break;
                    String name = stats.getLastName() == null ? stats.getUuid().toString().substring(0, 8) : stats.getLastName();
                    player.sendMessage(ColorUtil.color("&f" + rank + ". &7" + name + " &8- &a" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), stats.getProfit())));
                    player.sendMessage(ColorUtil.color("&7  &8» &7Wagered: &f" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), stats.getWagered()) + " &8| &aW:" + stats.getWins() + " &cL:" + stats.getLosses()));
                    player.sendMessage(ColorUtil.color("&7  &8» &7Biggest: &b" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), stats.getBiggestWin()) + " &8| &dJackpots: " + stats.getJackpots()));
                    rank++;
                }
            }
            case "factions" -> {
                Map<String, Double> top = plugin.getSlotMachineService().topFactions();
                if (top.isEmpty()) {
                    player.sendMessage(ColorUtil.color(plugin.message("no-faction-data")));
                    return true;
                }
                player.sendMessage(ColorUtil.color("&6Top Gambling Factions"));
                int rank = 1;
                for (Map.Entry<String, Double> entry : top.entrySet()) {
                    player.sendMessage(ColorUtil.color("&f" + rank + ". &7" + entry.getKey() + " &8- &6" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), entry.getValue())));
                    if (++rank > 10) break;
                }
            }
            case "stats" -> player.sendMessage(plugin.getSlotMachineService().statsMessage(player));
            case "pot" -> player.sendMessage(ColorUtil.color(plugin.prefix() + " &7Current pot: &d" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), plugin.getDataStore().getJackpotPot())));
            case "history" -> plugin.getSlotMachineService().historyLines().forEach(player::sendMessage);
            case "reload" -> {
                if (!player.hasPermission("slotmachine.admin")) {
                    player.sendMessage(ColorUtil.color(plugin.message("no-permission")));
                    return true;
                }
                plugin.reloadEverything();
                player.sendMessage(ColorUtil.color(plugin.message("reloaded")));
            }
            case "holo" -> {
                if (!player.hasPermission("slotmachine.admin")) {
                    player.sendMessage(ColorUtil.color(plugin.message("no-permission")));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.color("&cUsage: /gamble holo <winners|factions|refresh>"));
                    return true;
                }
                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "winners", "factions" -> {
                        plugin.getDataStore().setHologramLocation(args[1].toLowerCase(Locale.ROOT), player.getLocation());
                        plugin.getDataStore().save();
                        player.sendMessage(ColorUtil.color(plugin.message("holo-saved").replace("{type}", args[1].toLowerCase(Locale.ROOT))));
                        plugin.getHologramService().updateAll();
                    }
                    case "refresh" -> {
                        plugin.getHologramService().updateAll();
                        player.sendMessage(ColorUtil.color(plugin.message("holo-updated")));
                    }
                    default -> player.sendMessage(ColorUtil.color("&cUsage: /gamble holo <winners|factions|refresh>"));
                }
            }
            default -> player.sendMessage(ColorUtil.color("&cUsage: /gamble [top|factions|stats|pot|history|reload|holo]"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("top", "factions", "stats", "pot", "history", "reload", "holo").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("holo")) return Arrays.asList("winners", "factions", "refresh").stream().filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        return List.of();
    }
}
