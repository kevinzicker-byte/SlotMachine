package me.xthins.slotmachine.command;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.util.ColorUtil;
import org.bukkit.command.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HappyHourCommand implements CommandExecutor, TabCompleter {
    private final SlotMachinePlugin plugin;

    public HappyHourCommand(SlotMachinePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("slotmachine.admin")) {
            sender.sendMessage(ColorUtil.color(plugin.message("no-permission")));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.color("&cUsage: /happyhour <start|stop|status>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> plugin.getHappyHourService().startManual(true);
            case "stop" -> plugin.getHappyHourService().stop(true);
            case "status" -> {
                if (sender instanceof org.bukkit.entity.Player player) plugin.getHappyHourService().sendStatus(player);
                else sender.sendMessage("Happy hour active=" + plugin.getDataStore().isHappyHourActive() + ", left=" + plugin.getDataStore().getHappyHourMinutesLeft() + ", next=" + plugin.getDataStore().getHappyHourNextStartIn());
            }
            default -> sender.sendMessage(ColorUtil.color("&cUsage: /happyhour <start|stop|status>"));
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("start", "stop", "status").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        return List.of();
    }
}
