package me.xthins.slotmachine.hook;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;

public class SaberFactionsHook implements FactionHook {
    private final boolean available;

    public SaberFactionsHook() {
        this.available = Bukkit.getPluginManager().getPlugin("SaberFactions") != null || Bukkit.getPluginManager().getPlugin("Factions") != null;
    }

    @Override
    public String getFactionName(OfflinePlayer player) {
        if (!available || player == null) return null;
        try {
            Class<?> mPlayerClass = Class.forName("com.massivecraft.factions.entity.MPlayer");
            Method get = mPlayerClass.getMethod("get", OfflinePlayer.class);
            Object mPlayer = get.invoke(null, player);
            Method getFaction = mPlayerClass.getMethod("getFaction");
            Object faction = getFaction.invoke(mPlayer);
            if (faction == null) return null;
            Method getTag = faction.getClass().getMethod("getTag");
            Object tag = getTag.invoke(faction);
            return tag == null ? null : tag.toString();
        } catch (Throwable ignored) {
            try {
                Class<?> fPlayers = Class.forName("com.massivecraft.factions.FPlayers");
                Method getInstance = fPlayers.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                Method getByPlayer = fPlayers.getMethod("getByOfflinePlayer", OfflinePlayer.class);
                Object fPlayer = getByPlayer.invoke(instance, player);
                Method getFaction = fPlayer.getClass().getMethod("getFaction");
                Object faction = getFaction.invoke(fPlayer);
                Method getTag = faction.getClass().getMethod("getTag");
                Object tag = getTag.invoke(faction);
                return tag == null ? null : tag.toString();
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
