package me.xthins.slotmachine;

import me.xthins.slotmachine.command.GambleCommand;
import me.xthins.slotmachine.command.GambleFixCommand;
import me.xthins.slotmachine.command.HappyHourCommand;
import me.xthins.slotmachine.command.SlotMachineAdminCommand;
import me.xthins.slotmachine.gui.GambleMenu;
import me.xthins.slotmachine.hook.DecentHologramsHook;
import me.xthins.slotmachine.hook.SaberFactionsHook;
import me.xthins.slotmachine.hook.VaultHook;
import me.xthins.slotmachine.listener.InventoryListener;
import me.xthins.slotmachine.listener.PlayerSafetyListener;
import me.xthins.slotmachine.service.GambleService;
import me.xthins.slotmachine.service.HappyHourService;
import me.xthins.slotmachine.service.HologramService;
import me.xthins.slotmachine.storage.YamlDataStore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SlotMachinePlugin extends JavaPlugin {
    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration hologramsConfig;

    private VaultHook vaultHook;
    private SaberFactionsHook saberFactionsHook;
    private DecentHologramsHook decentHologramsHook;
    private YamlDataStore dataStore;
    private HappyHourService happyHourService;
    private GambleService gambleService;
    private HologramService hologramService;
    private GambleMenu gambleMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("gui.yml");
        saveResourceIfMissing("holograms.yml");
        loadExtraConfigs();

        vaultHook = new VaultHook(this);
        if (!vaultHook.setup()) {
            getLogger().severe("Vault economy not found. Disabling SlotMachine.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saberFactionsHook = new SaberFactionsHook(this);
        decentHologramsHook = new DecentHologramsHook(this);

        dataStore = new YamlDataStore(this);
        dataStore.load();

        happyHourService = new HappyHourService(this, dataStore);
        gambleService = new GambleService(this, vaultHook, saberFactionsHook, dataStore, happyHourService);
        gambleMenu = new GambleMenu(this, gambleService);
        hologramService = new HologramService(this, decentHologramsHook, saberFactionsHook, dataStore, happyHourService);

        getServer().getPluginManager().registerEvents(new InventoryListener(this, gambleService, gambleMenu), this);
        getServer().getPluginManager().registerEvents(new PlayerSafetyListener(gambleService), this);

        if (getCommand("gamble") != null) getCommand("gamble").setExecutor(new GambleCommand(this, gambleService, gambleMenu, dataStore));
        if (getCommand("happyhour") != null) getCommand("happyhour").setExecutor(new HappyHourCommand(this, happyHourService));
        if (getCommand("gamblefix") != null) getCommand("gamblefix").setExecutor(new GambleFixCommand(gambleService));
        if (getCommand("slotmachine") != null) getCommand("slotmachine").setExecutor(new SlotMachineAdminCommand(this, hologramService));

        happyHourService.start();
        hologramService.start();

        long autosave = getConfig().getInt("settings.auto-save-seconds", 60) * 20L;
        getServer().getScheduler().runTaskTimer(this, () -> {
            gambleService.clearTimedOutSpins();
            dataStore.save();
        }, autosave, autosave);

        getLogger().info("SlotMachine enabled.");
    }

    @Override
    public void onDisable() {
        if (hologramService != null) hologramService.stop();
        if (happyHourService != null) happyHourService.stop();
        if (dataStore != null) dataStore.save();
    }

    private void saveResourceIfMissing(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) saveResource(name, false);
    }

    private void loadExtraConfigs() {
        messagesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        guiConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui.yml"));
        hologramsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "holograms.yml"));
    }

    public FileConfiguration getMessagesConfig() { return messagesConfig; }
    public FileConfiguration getGuiConfig() { return guiConfig; }
    public FileConfiguration getHologramsConfig() { return hologramsConfig; }
    public YamlDataStore getDataStore() { return dataStore; }
    public HappyHourService getHappyHourService() { return happyHourService; }
}
