package me.xthins.slotmachine.service;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class MessageService {
    private final SlotMachinePlugin plugin;

    public MessageService(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    public String get(String path) {
        return plugin.getMessages().getString(path, path);
    }

    public String format(String path, Map<String, String> placeholders) {
        String text = get(path).replace("{prefix}", plugin.getSettings().prefix());
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ColorUtil.color(text);
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(ColorUtil.color(get(path).replace("{prefix}", plugin.getSettings().prefix())));
    }
}
