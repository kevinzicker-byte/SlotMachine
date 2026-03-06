package me.xthins.slotmachine.hook;

import org.bukkit.OfflinePlayer;

public interface FactionHook {
    String getFactionName(OfflinePlayer player);
    boolean isAvailable();
}
