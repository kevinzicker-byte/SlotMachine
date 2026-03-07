package me.xthins.slotmachine.hook;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class SaberFactionsHook {
    private final Plugin plugin;
    private final boolean enabled;

    public SaberFactionsHook(Plugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getServer().getPluginManager().getPlugin("SaberFactions") != null
                || plugin.getServer().getPluginManager().getPlugin("Factions") != null;
    }

    public boolean isEnabled() { return enabled; }

    public String getFactionTag(Player player) {
        if (!enabled || player == null) return "No Faction";
        try {
            Class<?> fPlayersClass = Class.forName("com.massivecraft.factions.FPlayers");
            Object inst = fPlayersClass.getMethod("getInstance").invoke(null);
            Object fPlayer = fPlayersClass.getMethod("getByPlayer", Player.class).invoke(inst, player);
            Object faction = fPlayer.getClass().getMethod("getFaction").invoke(fPlayer);
            Object tag = faction.getClass().getMethod("getTag").invoke(faction);
            return String.valueOf(tag);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not get faction tag for " + player.getName());
            return "No Faction";
        }
    }
}
