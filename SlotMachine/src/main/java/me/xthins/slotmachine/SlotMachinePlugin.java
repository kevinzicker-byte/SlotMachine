package me.xthins.slotmachine;

import me.xthins.slotmachine.command.GambleCommand;
import me.xthins.slotmachine.command.GambleFixCommand;
import me.xthins.slotmachine.command.HappyHourCommand;
import me.xthins.slotmachine.listener.MenuListener;
import me.xthins.slotmachine.model.BetTier;
import me.xthins.slotmachine.service.FactionService;
import me.xthins.slotmachine.service.HappyHourService;
import me.xthins.slotmachine.service.HologramService;
import me.xthins.slotmachine.service.SlotMachineService;
import me.xthins.slotmachine.storage.DataStore;
import me.xthins.slotmachine.util.ColorUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SlotMachinePlugin extends JavaPlugin {

    private Economy economy;
    private DataStore dataStore;
    private SlotMachineService slotMachineService;
    private HappyHourService happyHourService;
    private HologramService hologramService;
    private FactionService factionService;
    private List<BetTier> betTiers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("gui.yml", false);
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found. Disabling SlotMachine.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        reloadEverything();

        PluginCommand gamble = getCommand("gamble");
        PluginCommand happyhour = getCommand("happyhour");
        PluginCommand fix = getCommand("gamblefix");
        if (gamble != null) {
            GambleCommand gc = new GambleCommand(this);
            gamble.setExecutor(gc);
            gamble.setTabCompleter(gc);
        }
        if (happyhour != null) {
            HappyHourCommand hc = new HappyHourCommand(this);
            happyhour.setExecutor(hc);
            happyhour.setTabCompleter(hc);
        }
        if (fix != null) fix.setExecutor(new GambleFixCommand(this));

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getLogger().info("SlotMachine enabled.");
    }

    @Override
    public void onDisable() {
        if (happyHourService != null) happyHourService.shutdown();
        if (dataStore != null) dataStore.save();
    }

    public void reloadEverything() {
        reloadConfig();
        this.dataStore = new DataStore(this);
        this.dataStore.load(getConfig().getDouble("jackpot.base-pot", 10_000_000), getConfig().getInt("happy-hour.interval-minutes", 60));
        this.factionService = new FactionService(this);
        this.hologramService = new HologramService(this);
        this.happyHourService = new HappyHourService(this);
        this.slotMachineService = new SlotMachineService(this);
        this.betTiers = loadBetTiers();
        happyHourService.start();
        hologramService.updateAll();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        this.economy = rsp.getProvider();
        return economy != null;
    }

    private List<BetTier> loadBetTiers() {
        List<BetTier> list = new ArrayList<>();
        ConfigurationSection bets = getConfig().getConfigurationSection("bets");
        if (bets == null) return list;
        for (String key : bets.getKeys(false)) {
            ConfigurationSection sec = bets.getConfigurationSection(key);
            if (sec == null) continue;
            Material material = material(sec.getString("material"), Material.DIAMOND);
            list.add(new BetTier(
                    sec.getString("key", key),
                    sec.getString("display-name", key),
                    material,
                    sec.getDouble("cost"),
                    sec.getDouble("min-multiplier"),
                    sec.getDouble("max-multiplier"),
                    sec.getDouble("win-chance", defaultWinChance(key)),
                    sec.getInt("gui-slot")
            ));
        }
        list.sort(Comparator.comparingInt(BetTier::guiSlot));
        return list;
    }

    private double defaultWinChance(String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "small" -> 0.38;
            case "medium" -> 0.28;
            case "big" -> 0.18;
            default -> 0.25;
        };
    }

    public String prefix() { return getConfig().getString("prefix", "&8[&dSlotMachine&8]&r"); }
    public String currencySymbol() { return getConfig().getString("economy.currency-symbol", "$"); }
    public boolean useShortGuiMoney() { return getConfig().getBoolean("economy.use-short-format-in-gui", true); }
    public Economy getEconomy() { return economy; }
    public DataStore getDataStore() { return dataStore; }
    public SlotMachineService getSlotMachineService() { return slotMachineService; }
    public HappyHourService getHappyHourService() { return happyHourService; }
    public HologramService getHologramService() { return hologramService; }
    public FactionService getFactionService() { return factionService; }
    public List<BetTier> getBetTiers() { return betTiers; }

    public Material material(String name, Material fallback) {
        if (name == null) return fallback;
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    public Sound parseSound(String name, Sound fallback) {
        if (name == null) return fallback;
        try { return Sound.valueOf(name.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ex) { return fallback; }
    }

    public String message(String key) {
        return prefixMessage(org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(getDataFolder(), "messages.yml")).getString(key, key));
    }

    public String prefixMessage(String message) {
        return message.replace("{prefix}", prefix());
    }

    public void broadcast(String key, String... replacements) {
        String msg = message(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) msg = msg.replace("{" + replacements[i] + "}", replacements[i + 1]);
        getServer().broadcastMessage(ColorUtil.color(msg));
    }
}
