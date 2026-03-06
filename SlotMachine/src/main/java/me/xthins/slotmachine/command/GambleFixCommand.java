package me.xthins.slotmachine.command;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GambleFixCommand implements CommandExecutor {
    private final SlotMachinePlugin plugin;
    public GambleFixCommand(SlotMachinePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("slotmachine.admin")) {
            sender.sendMessage(ColorUtil.color(plugin.message("no-permission")));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ColorUtil.color("&cUsage: /gamblefix <player>"));
            return true;
        }
        var target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getSlotMachineService().clearSpin(target.getUniqueId());
        sender.sendMessage(ColorUtil.color(plugin.message("fixed-player").replace("{player}", target.getName() == null ? args[0] : target.getName())));
        return true;
    }
}
