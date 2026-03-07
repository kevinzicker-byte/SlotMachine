package me.xthins.slotmachine.command;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.gui.GambleMenu;
import me.xthins.slotmachine.service.GambleService;
import me.xthins.slotmachine.storage.YamlDataStore;
import me.xthins.slotmachine.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GambleCommand implements CommandExecutor {
    private final SlotMachinePlugin plugin;
    private final GambleService gambleService;
    private final GambleMenu gambleMenu;
    private final YamlDataStore dataStore;

    public GambleCommand(SlotMachinePlugin plugin, GambleService gambleService, GambleMenu gambleMenu, YamlDataStore dataStore) {
        this.plugin = plugin;
        this.gambleService = gambleService;
        this.gambleMenu = gambleMenu;
        this.dataStore = dataStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("slotmachine.use")) {
            player.sendMessage(ColorUtil.color(plugin.getMessagesConfig().getString("no-permission")));
            return true;
        }

        if (args.length == 0) {
            gambleMenu.openMain(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "top" -> gambleService.buildTopMessages().forEach(player::sendMessage);
            case "stats" -> player.sendMessage(gambleService.buildStatsMessage(player));
            case "pot" -> player.sendMessage(gambleService.buildPotMessage());
            case "history" -> gambleService.buildHistoryMessages().forEach(player::sendMessage);
            case "help" -> plugin.getMessagesConfig().getStringList("help").stream().map(ColorUtil::color).forEach(player::sendMessage);
            case "reload" -> {
                if (!player.hasPermission("slotmachine.admin")) {
                    player.sendMessage(ColorUtil.color(plugin.getMessagesConfig().getString("no-permission")));
                    return true;
                }
                plugin.reloadConfig();
                player.sendMessage(ColorUtil.color("&aReloaded config.yml. For full reload, restart server."));
            }
            default -> player.sendMessage(ColorUtil.color("&cUsage: /gamble [top|stats|pot|history|help|reload]"));
        }

        return true;
    }
}
