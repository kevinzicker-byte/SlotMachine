package me.xthins.slotmachine.data;

import me.xthins.slotmachine.model.GambleProfile;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DataStore {
    void load();
    void save();
    Map<UUID, GambleProfile> profiles();
    GambleProfile profile(UUID uuid, String name);
    double jackpotPot();
    void setJackpotPot(double value);
    List<String> jackpotHistory();
    void addJackpotHistory(String entry);
    boolean happyHourActive();
    void setHappyHourActive(boolean active);
    double happyHourMultiplier();
    void setHappyHourMultiplier(double multiplier);
    int happyHourMinutesLeft();
    void setHappyHourMinutesLeft(int minutes);
    int nextHappyHourStartIn();
    void setNextHappyHourStartIn(int minutes);
    Location hologramLocation(String key);
    void setHologramLocation(String key, Location location);
    double lastMilestoneBroadcast();
    void setLastMilestoneBroadcast(double value);
}
