package me.xthins.slotmachine.command;

import me.xthins.slotmachine.service.HologramService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SlotMachineAdminCommand implements CommandExecutor {
    private final HologramService hologramService;

    public SlotMachineAdminCommand(me.xthins.slotmachine.SlotMachinePlugin plugin, HologramService hologramService) {
        this.hologramService = hologramService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("slotmachine.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("holo") && args[1].equalsIgnoreCase("update")) {
            hologramService.updateAll();
            sender.sendMessage("§aUpdated all holograms.");
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("holo")) {
            if (args[1].equalsIgnoreCase("create")) {
                hologramService.create(args[2]);
                sender.sendMessage("§aCreated/updated hologram §f" + args[2]);
                return true;
            }
            if (args[1].equalsIgnoreCase("delete")) {
                hologramService.delete(args[2]);
                sender.sendMessage("§aDeleted hologram §f" + args[2]);
                return true;
            }
        }

        sender.sendMessage("§e/slotmachine holo create <winners|factions|jackpot>");
        sender.sendMessage("§e/slotmachine holo delete <winners|factions|jackpot>");
        sender.sendMessage("§e/slotmachine holo update");
        return true;
    }
}
