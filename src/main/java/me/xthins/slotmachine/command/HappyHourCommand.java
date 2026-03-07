package me.xthins.slotmachine.command;

import me.xthins.slotmachine.service.HappyHourService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HappyHourCommand implements CommandExecutor {
    private final HappyHourService happyHourService;

    public HappyHourCommand(me.xthins.slotmachine.SlotMachinePlugin plugin, HappyHourService happyHourService) {
        this.happyHourService = happyHourService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("slotmachine.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(happyHourService.statusMessage());
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            happyHourService.startManual();
            sender.sendMessage("§aStarted Happy Hour.");
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            happyHourService.stopManual();
            sender.sendMessage("§aStopped Happy Hour.");
            return true;
        }

        sender.sendMessage("§cUsage: /happyhour <start|stop|status>");
        return true;
    }
}
