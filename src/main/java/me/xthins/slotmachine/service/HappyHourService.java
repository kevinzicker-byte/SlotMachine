package me.xthins.slotmachine.service;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.storage.YamlDataStore;
import me.xthins.slotmachine.util.ColorUtil;
import org.bukkit.Bukkit;

public class HappyHourService {
    private final SlotMachinePlugin plugin;
    private final YamlDataStore dataStore;
    private int taskId = -1;

    public HappyHourService(SlotMachinePlugin plugin, YamlDataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    public void start() {
        stop();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickMinute, 20L, 1200L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void tickMinute() {
        if (!plugin.getConfig().getBoolean("happy-hour.enabled", true)) return;
        if (dataStore.isHappyHourActive()) {
            int left = Math.max(0, dataStore.getHappyHourMinutesLeft() - 1);
            dataStore.setHappyHourMinutesLeft(left);
            if (left <= 0) {
                stopManual();
            }
        } else {
            int next = Math.max(0, dataStore.getHappyHourNextStartIn() - 1);
            dataStore.setHappyHourNextStartIn(next);
            if (next <= 0) startManual();
        }
    }

    public void startManual() {
        dataStore.setHappyHourActive(true);
        dataStore.setHappyHourMinutesLeft(plugin.getConfig().getInt("happy-hour.duration-minutes", 5));
        dataStore.setHappyHourNextStartIn(plugin.getConfig().getInt("happy-hour.interval-minutes", 60));
        Bukkit.broadcastMessage(format("happy-hour-start", String.valueOf(dataStore.getHappyHourMinutesLeft())));
    }

    public void stopManual() {
        dataStore.setHappyHourActive(false);
        dataStore.setHappyHourMinutesLeft(0);
        dataStore.setHappyHourNextStartIn(plugin.getConfig().getInt("happy-hour.interval-minutes", 60));
        Bukkit.broadcastMessage(format("happy-hour-end", "0"));
    }

    public boolean isActive() { return dataStore.isHappyHourActive(); }
    public int getMinutesLeft() { return dataStore.getHappyHourMinutesLeft(); }
    public double getPayoutMultiplier() { return isActive() ? plugin.getConfig().getDouble("happy-hour.multiplier", 2.0) : 1.0; }
    public double getJackpotChanceMultiplier() { return plugin.getConfig().getBoolean("happy-hour.double-jackpot-chance", true) && isActive() ? 2.0 : 1.0; }

    public String statusMessage() {
        if (isActive()) return format("happy-hour-status-on", String.valueOf(getMinutesLeft()));
        return format("happy-hour-status-off", String.valueOf(dataStore.getHappyHourNextStartIn()));
    }

    private String format(String path, String mins) {
        String msg = plugin.getMessagesConfig().getString(path, path)
                .replace("{prefix}", plugin.getMessagesConfig().getString("prefix", "&8[&dSlotMachine&8]&r"))
                .replace("{minutes}", mins);
        return ColorUtil.color(msg);
    }
}
