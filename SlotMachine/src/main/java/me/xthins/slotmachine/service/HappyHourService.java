package me.xthins.slotmachine.service;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.util.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class HappyHourService {
    private final SlotMachinePlugin plugin;
    private BukkitTask task;

    public HappyHourService(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) task.cancel();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickMinute, 20L * 60L, 20L * 60L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
    }

    private void tickMinute() {
        if (!plugin.getConfig().getBoolean("happy-hour.enabled", true)) return;
        var store = plugin.getDataStore();
        if (store.isHappyHourActive()) {
            int left = Math.max(0, store.getHappyHourMinutesLeft() - 1);
            store.setHappyHourMinutesLeft(left);
            if (left > 0) {
                plugin.broadcast("happy-hour-remaining", "minutes", String.valueOf(left));
            } else {
                stop(false);
            }
        } else {
            int next = Math.max(0, store.getHappyHourNextStartIn() - 1);
            if (next <= 0) {
                startManual(false);
            } else {
                store.setHappyHourNextStartIn(next);
                store.save();
            }
        }
    }

    public void startManual(boolean manual) {
        var store = plugin.getDataStore();
        int duration = plugin.getConfig().getInt("happy-hour.duration-minutes", 5);
        store.setHappyHourActive(true);
        store.setHappyHourMinutesLeft(duration);
        store.setHappyHourMultiplier(plugin.getConfig().getDouble("happy-hour.multiplier", 2.0));
        store.setHappyHourNextStartIn(plugin.getConfig().getInt("happy-hour.interval-minutes", 60));
        store.save();
        plugin.broadcast(manual ? "happy-hour-manual-start" : "happy-hour-start", "minutes", String.valueOf(duration));
        playAll(plugin.getConfig().getString("sounds.happy-hour", "ENTITY_PLAYER_LEVELUP"));
    }

    public void stop(boolean manual) {
        var store = plugin.getDataStore();
        store.setHappyHourActive(false);
        store.setHappyHourMinutesLeft(0);
        store.setHappyHourMultiplier(1.0);
        store.setHappyHourNextStartIn(plugin.getConfig().getInt("happy-hour.interval-minutes", 60));
        store.save();
        plugin.broadcast(manual ? "happy-hour-stopped" : "happy-hour-end");
    }

    public double apply(double base) {
        if (!plugin.getDataStore().isHappyHourActive()) return base;
        return base * plugin.getDataStore().getHappyHourMultiplier();
    }

    public void sendStatus(Player player) {
        if (plugin.getDataStore().isHappyHourActive()) {
            player.sendMessage(ColorUtil.color(plugin.message("happy-hour-status-active")
                    .replace("{minutes}", String.valueOf(plugin.getDataStore().getHappyHourMinutesLeft()))));
        } else {
            player.sendMessage(ColorUtil.color(plugin.message("happy-hour-status-idle")
                    .replace("{minutes}", String.valueOf(plugin.getDataStore().getHappyHourNextStartIn()))));
        }
    }

    private void playAll(String soundName) {
        Sound sound = plugin.parseSound(soundName, Sound.ENTITY_PLAYER_LEVELUP);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        }
    }
}
