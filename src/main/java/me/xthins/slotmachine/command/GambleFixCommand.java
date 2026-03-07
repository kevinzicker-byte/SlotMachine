package me.xthins.slotmachine.command;

import me.xthins.slotmachine.service.GambleService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GambleFixCommand implements CommandExecutor {
    private final GambleService gambleService;

    public GambleFixCommand(GambleService gambleService) {
        this.gambleService = gambleService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("slotmachine.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /gamblefix <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        gambleService.forceClear(target.getUniqueId());
        sender.sendMessage("§aCleared stuck gamble state for §f" + target.getName());
        return true;
    }
}
