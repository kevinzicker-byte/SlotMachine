package me.xthins.slotmachine.config;

import me.xthins.slotmachine.model.BetTier;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Settings {
    private final String prefix;
    private final String currencySymbol;
    private final boolean shortFormatInGui;
    private final Map<String, BetTier> betTiers = new LinkedHashMap<>();
    private final double jackpotBasePot;
    private final double jackpotChancePercent;
    private final double jackpotContributionPercent;
    private final double bigWinThreshold;
    private final List<Double> jackpotMilestones;
    private final boolean happyHourEnabled;
    private final double happyHourMultiplier;
    private final int happyHourDurationMinutes;
    private final int happyHourIntervalMinutes;
    private final boolean doubleJackpotChance;
    private final String guiTitle;
    private final int spinTicksNormal;
    private final int jackpotExtraSlowTicks;
    private final int normalTickSpeed;
    private final int slowTickSpeed;
    private final int flashCycles;
    private final int bedrockTimeoutSeconds;
    private final List<Integer> topRowSlots;
    private final List<Integer> middleRowSlots;
    private final List<Integer> bottomRowSlots;
    private final List<Integer> paylineSlots;
    private final boolean hologramsEnabled;
    private final int hologramUpdateSeconds;
    private final String winnersHologramId;
    private final String factionsHologramId;
    private final String winnersHologramTitle;
    private final String factionsHologramTitle;

    public Settings(FileConfiguration config) {
        this.prefix = config.getString("prefix", "&8[&dSlotMachine&8]&r");
        this.currencySymbol = config.getString("economy.currency-symbol", "$");
        this.shortFormatInGui = config.getBoolean("economy.use-short-format-in-gui", true);

        ConfigurationSection bets = config.getConfigurationSection("bets");
        if (bets != null) {
            for (String key : bets.getKeys(false)) {
                ConfigurationSection section = bets.getConfigurationSection(key);
                if (section == null) continue;
                Material material = Material.matchMaterial(section.getString("material", "STONE"));
                if (material == null) material = Material.STONE;
                betTiers.put(key.toLowerCase(), new BetTier(
                        key.toLowerCase(),
                        section.getString("display-name", key),
                        material,
                        section.getDouble("cost"),
                        section.getDouble("min-multiplier", 1.0),
                        section.getDouble("max-multiplier", 1.0),
                        section.getInt("gui-slot")
                ));
            }
        }

        this.jackpotBasePot = config.getDouble("jackpot.base-pot", 10_000_000);
        this.jackpotChancePercent = config.getDouble("jackpot.chance-percent", 0.35);
        this.jackpotContributionPercent = config.getDouble("jackpot.contribution-percent", 25.0);
        this.bigWinThreshold = config.getDouble("jackpot.big-win-threshold", 1_000_000);
        this.jackpotMilestones = config.getDoubleList("jackpot.milestone-broadcasts");

        this.happyHourEnabled = config.getBoolean("happy-hour.enabled", true);
        this.happyHourMultiplier = config.getDouble("happy-hour.multiplier", 2.0);
        this.happyHourDurationMinutes = config.getInt("happy-hour.duration-minutes", 5);
        this.happyHourIntervalMinutes = config.getInt("happy-hour.interval-minutes", 60);
        this.doubleJackpotChance = config.getBoolean("happy-hour.double-jackpot-chance", true);

        this.guiTitle = config.getString("spin.gui-title", "&d&lSLOT MACHINE");
        this.spinTicksNormal = config.getInt("spin.ticks-normal", 24);
        this.jackpotExtraSlowTicks = config.getInt("spin.jackpot-extra-slow-ticks", 10);
        this.normalTickSpeed = config.getInt("spin.tick-speed-normal", 2);
        this.slowTickSpeed = config.getInt("spin.tick-speed-slow", 5);
        this.flashCycles = config.getInt("spin.flash-cycles", 4);
        this.bedrockTimeoutSeconds = config.getInt("spin.bedrock-timeout-seconds", 20);

        this.topRowSlots = config.getIntegerList("reels.top-row-slots");
        this.middleRowSlots = config.getIntegerList("reels.middle-row-slots");
        this.bottomRowSlots = config.getIntegerList("reels.bottom-row-slots");
        this.paylineSlots = config.getIntegerList("reels.payline-slots");

        this.hologramsEnabled = config.getBoolean("holograms.enabled", true);
        this.hologramUpdateSeconds = config.getInt("holograms.update-seconds", 60);
        this.winnersHologramId = config.getString("holograms.winners-id", "slotmachine_winners");
        this.factionsHologramId = config.getString("holograms.factions-id", "slotmachine_factions");
        this.winnersHologramTitle = config.getString("holograms.winners-title", "&d&lTop Winners");
        this.factionsHologramTitle = config.getString("holograms.factions-title", "&6&lTop Gambling Factions");
    }

    public String prefix() { return prefix; }
    public String currencySymbol() { return currencySymbol; }
    public boolean shortFormatInGui() { return shortFormatInGui; }
    public Map<String, BetTier> betTiers() { return betTiers; }
    public double jackpotBasePot() { return jackpotBasePot; }
    public double jackpotChancePercent() { return jackpotChancePercent; }
    public double jackpotContributionPercent() { return jackpotContributionPercent; }
    public double bigWinThreshold() { return bigWinThreshold; }
    public List<Double> jackpotMilestones() { return jackpotMilestones; }
    public boolean happyHourEnabled() { return happyHourEnabled; }
    public double happyHourMultiplier() { return happyHourMultiplier; }
    public int happyHourDurationMinutes() { return happyHourDurationMinutes; }
    public int happyHourIntervalMinutes() { return happyHourIntervalMinutes; }
    public boolean doubleJackpotChance() { return doubleJackpotChance; }
    public String guiTitle() { return guiTitle; }
    public int spinTicksNormal() { return spinTicksNormal; }
    public int jackpotExtraSlowTicks() { return jackpotExtraSlowTicks; }
    public int normalTickSpeed() { return normalTickSpeed; }
    public int slowTickSpeed() { return slowTickSpeed; }
    public int flashCycles() { return flashCycles; }
    public int bedrockTimeoutSeconds() { return bedrockTimeoutSeconds; }
    public List<Integer> topRowSlots() { return topRowSlots; }
    public List<Integer> middleRowSlots() { return middleRowSlots; }
    public List<Integer> bottomRowSlots() { return bottomRowSlots; }
    public List<Integer> paylineSlots() { return paylineSlots; }
    public boolean hologramsEnabled() { return hologramsEnabled; }
    public int hologramUpdateSeconds() { return hologramUpdateSeconds; }
    public String winnersHologramId() { return winnersHologramId; }
    public String factionsHologramId() { return factionsHologramId; }
    public String winnersHologramTitle() { return winnersHologramTitle; }
    public String factionsHologramTitle() { return factionsHologramTitle; }
}
