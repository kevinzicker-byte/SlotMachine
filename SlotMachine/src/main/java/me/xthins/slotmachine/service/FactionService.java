package me.xthins.slotmachine.service;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public class FactionService {
    private final JavaPlugin plugin;
    private final boolean available;

    public FactionService(JavaPlugin plugin) {
        this.plugin = plugin;
        PluginManager pm = plugin.getServer().getPluginManager();
        this.available = pm.getPlugin("SaberFactions") != null || pm.getPlugin("Factions") != null;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getFactionTag(Player player) {
        if (!available) return null;
        try {
            Class<?> fPlayersClass = Class.forName("com.massivecraft.factions.FPlayers");
            Method getInstance = fPlayersClass.getMethod("getInstance");
            Object fPlayers = getInstance.invoke(null);
            Method getByPlayer = fPlayersClass.getMethod("getByPlayer", Player.class);
            Object fPlayer = getByPlayer.invoke(fPlayers, player);
            if (fPlayer == null) return null;
            Method getFaction = fPlayer.getClass().getMethod("getFaction");
            Object faction = getFaction.invoke(fPlayer);
            if (faction == null) return null;
            Method getTag = faction.getClass().getMethod("getTag");
            Object tag = getTag.invoke(faction);
            if (tag == null) return null;
            String result = String.valueOf(tag).trim();
            return result.isEmpty() ? null : result;
        } catch (Throwable ex) {
            plugin.getLogger().fine("Faction hook unavailable: " + ex.getMessage());
            return null;
        }
    }
}
