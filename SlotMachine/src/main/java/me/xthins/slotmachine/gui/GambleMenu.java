package me.xthins.slotmachine.gui;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.BetTier;
import me.xthins.slotmachine.util.ColorUtil;
import me.xthins.slotmachine.util.ItemUtil;
import me.xthins.slotmachine.util.MoneyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GambleMenu {
    private final SlotMachinePlugin plugin;

    public GambleMenu(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 45, ColorUtil.color(plugin.getSettings().guiTitle()));
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, " ");

        for (BetTier tier : plugin.getSettings().betTiers().values()) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Cost: &f" + MoneyUtil.money(tier.cost(), plugin.getSettings().shortFormatInGui(), plugin.getSettings().currencySymbol()));
            lore.add("&7Range: &a" + tier.minMultiplier() + "x &7- &a" + tier.maxMultiplier() + "x");
            lore.add("&8Adds to the jackpot pot.");
            if (plugin.getHappyHourService().isActive()) {
                lore.add("&6&lHAPPY HOUR &7x" + plugin.getHappyHourService().multiplier() + " &8(" + plugin.getDataStore().happyHourMinutesLeft() + "m left)");
            }
            inventory.setItem(tier.guiSlot(), ItemUtil.item(tier.material(), tier.displayName(), lore));
        }

        // reel frame
        for (int slot : plugin.getSettings().topRowSlots()) inventory.setItem(slot, reelItem(Material.IRON_INGOT));
        for (int slot : plugin.getSettings().middleRowSlots()) inventory.setItem(slot, reelItem(Material.GOLD_INGOT));
        for (int slot : plugin.getSettings().bottomRowSlots()) inventory.setItem(slot, reelItem(Material.DIAMOND));
        inventory.setItem(plugin.getSettings().paylineSlots().get(0), ItemUtil.item(Material.RED_STAINED_GLASS_PANE, "&f&l>", List.of("&7Middle line pays")));
        inventory.setItem(plugin.getSettings().paylineSlots().get(1), ItemUtil.item(Material.RED_STAINED_GLASS_PANE, "&f&l<", List.of("&7Middle line pays")));

        inventory.setItem(4, jackpotItem());
        player.openInventory(inventory);
    }

    public ItemStack jackpotItem() {
        double pot = plugin.getDataStore().jackpotPot();
        int filled = (int) Math.round(Math.min(10.0, Math.max(0.0, pot / (100_000_000D / 10D))));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "&d█" : "&8█");
        }
        List<String> lore = new ArrayList<>();
        lore.add("&7Current pot: &d" + MoneyUtil.money(pot, plugin.getSettings().shortFormatInGui(), plugin.getSettings().currencySymbol()));
        lore.add("&7Progress: " + bar);
        lore.add("&8Win chance: &d" + plugin.getSettings().jackpotChancePercent() + "%");
        if (plugin.getHappyHourService().isActive()) {
            lore.add("&6&lHAPPY HOUR &7x" + plugin.getHappyHourService().multiplier() + " &8(" + plugin.getDataStore().happyHourMinutesLeft() + "m left)");
        } else {
            lore.add("&7Happy Hour: &cOFF");
        }
        return ItemUtil.item(Material.SUNFLOWER, "&d&lJackpot Pot", lore);
    }

    public ItemStack reelItem(Material material) {
        return ItemUtil.item(material, " ", List.of());
    }

    private void fill(Inventory inventory, Material material, String name) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, ItemUtil.item(material, name, List.of()));
        }
    }
}
