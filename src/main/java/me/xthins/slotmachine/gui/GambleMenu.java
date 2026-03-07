package me.xthins.slotmachine.gui;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.BetTier;
import me.xthins.slotmachine.service.GambleService;
import me.xthins.slotmachine.util.ColorUtil;
import me.xthins.slotmachine.util.MoneyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class GambleMenu {
    private final SlotMachinePlugin plugin;
    private final GambleService gambleService;

    public GambleMenu(SlotMachinePlugin plugin, GambleService gambleService) {
        this.plugin = plugin;
        this.gambleService = gambleService;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, ColorUtil.color(plugin.getConfig().getString("settings.gui-title")));
        Material filler = material(plugin.getGuiConfig().getString("layout.filler", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, item(filler, " "));

        for (BetTier tier : BetTier.values()) {
            ArrayList<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Click to spin."));
            lore.add(ColorUtil.color("&8Adds to the jackpot pot."));
            if (plugin.getHappyHourService().isActive()) lore.add(ColorUtil.color("&6&lHAPPY HOUR &7x2 &8(" + plugin.getHappyHourService().getMinutesLeft() + "m left)"));
            String name = gambleService.getBetDisplayName(tier) + " &7- &f" + MoneyUtil.moneyShort(gambleService.getBetAmount(tier));
            inv.setItem(gambleService.getBetSlot(tier), item(material(gambleService.getBetMaterial(tier), Material.PAPER), ColorUtil.color(name), lore));
        }

        inv.setItem(plugin.getGuiConfig().getInt("layout.pot-slot", 22), buildPotItem());
        player.openInventory(inv);
    }

    public void openSpinBase(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, ColorUtil.color(plugin.getConfig().getString("settings.spinning-title")));
        Material filler = material(plugin.getGuiConfig().getString("layout.filler", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, item(filler, " "));
        inv.setItem(plugin.getGuiConfig().getInt("layout.pot-slot", 22), buildPotItem());
        player.openInventory(inv);
    }

    public ItemStack buildPotItem() {
        ItemStack item = item(Material.SUNFLOWER, ColorUtil.color("&d&lJackpot Pot"));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ArrayList<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Current pot: &d" + MoneyUtil.moneyCommas(plugin.getDataStore().getJackpot())));
            int filled = (int) Math.max(0, Math.min(10, Math.round(plugin.getDataStore().getJackpot() / 10000000.0)));
            StringBuilder bar = new StringBuilder();
            for (int i = 1; i <= 10; i++) bar.append(i <= filled ? "§d█" : "§8█");
            lore.add(ColorUtil.color("&7Progress: " + bar));
            lore.add(ColorUtil.color(plugin.getHappyHourService().isActive() ? "&7Happy Hour: &aACTIVE" : "&7Happy Hour: &cOFF"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack item(Material material, String name) { return item(material, name, new ArrayList<>()); }

    private ItemStack item(Material material, String name, java.util.List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material material(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); } catch (Exception ex) { return fallback; }
    }
}
