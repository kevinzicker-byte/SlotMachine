package me.xthins.slotmachine.listener;

import me.xthins.slotmachine.SlotMachinePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final SlotMachinePlugin plugin;

    public PlayerListener(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getGambleService().clearSpinning(event.getPlayer().getUniqueId());
    }
}
