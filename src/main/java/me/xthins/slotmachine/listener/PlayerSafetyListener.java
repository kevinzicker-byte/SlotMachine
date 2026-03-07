package me.xthins.slotmachine.listener;

import me.xthins.slotmachine.service.GambleService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSafetyListener implements Listener {
    private final GambleService gambleService;

    public PlayerSafetyListener(GambleService gambleService) {
        this.gambleService = gambleService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gambleService.forceClear(event.getPlayer().getUniqueId());
    }
}
