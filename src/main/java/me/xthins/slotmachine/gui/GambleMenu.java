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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GambleMenu {
    private final SlotMachinePlugin plugin;
    private final GambleService gambleService;

    public GambleMenu(SlotMachinePlugin plugin, GambleService gambleService) {
        this.plugin = plugin;
        this.gambleService = gambleService;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, ColorUtil.color(plugin.getConfig().getString("settings.gui-title", "&d&lCasino")));
        Material filler = material(plugin.getGuiConfig().getString("layout.filler", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, item(filler, "&7"));
        }

        inv.setItem(10, glowingItem(Material.IRON_INGOT, "&d&lSlots",
                "&7Classic reels with jackpots,",
                "&7free spins, near misses, and",
                "&7happy hour support.",
                "",
                "&aClick to open"));

        inv.setItem(12, item(Material.PAPER, "&6&lBlackjack",
                "&7Coming in phase 2.",
                "",
                "&8Real blackjack rules"));

        inv.setItem(13, item(Material.SUNFLOWER, "&e&lCoinflip",
                "&7Coming in phase 2.",
                "",
                "&850/50 wager game"));

        inv.setItem(14, item(Material.REDSTONE, "&c&lRoulette",
                "&7Coming in phase 2.",
                "",
                "&8Real roulette payouts"));

        inv.setItem(16, item(Material.TNT, "&4&lMines",
                "&7Coming in phase 2.",
                "",
                "&8Risk/reward cash-out game"));

        inv.setItem(plugin.getGuiConfig().getInt("layout.pot-slot", 22), buildPotItem());

        PlayerStatsView statsView = gambleService.buildPlayerStatsView(player);
        inv.setItem(24, item(Material.BOOK, "&b&lYour Casino Stats",
                "&7Wagered: &f" + MoneyUtil.moneyCommas(statsView.wagered()),
                "&7Won: &a" + MoneyUtil.moneyCommas(statsView.won()),
                "&7Profit: " + (statsView.profit() >= 0 ? "&a" : "&c") + MoneyUtil.moneyCommas(statsView.profit()),
                "&7Wins: &a" + statsView.wins(),
                "&7Losses: &c" + statsView.losses(),
                "&7Biggest Win: &b" + MoneyUtil.moneyCommas(statsView.biggestWin()),
                "&7Jackpots: &d" + statsView.jackpots(),
                "&7Free Spins: &6" + gambleService.getFreeSpins(player),
                "",
                "&8More detailed stats later"));

        player.openInventory(inv);
    }

    public void openSlotsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, ColorUtil.color("&d&lSlots"));
        Material filler = material(plugin.getGuiConfig().getString("layout.filler", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, item(filler, "&7"));
        }

        for (BetTier tier : BetTier.values()) {
            ArrayList<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Click to spin."));
            lore.add(ColorUtil.color("&8Adds to the progressive jackpot."));
            lore.add(ColorUtil.color("&7Free spins: &6" + gambleService.getFreeSpins(player)));
            if (plugin.getHappyHourService().isActive()) {
                lore.add(ColorUtil.color("&6&lHAPPY HOUR &7x" + trimDouble(plugin.getHappyHourService().getPayoutMultiplier())
                        + " &8(" + plugin.getHappyHourService().getMinutesLeft() + "m left)"));
            }
            String name = gambleService.getBetDisplayName(tier) + " &7- &f" + MoneyUtil.moneyShort(gambleService.getBetAmount(tier));
            inv.setItem(gambleService.getBetSlot(tier), item(material(gambleService.getBetMaterial(tier), Material.PAPER), name, lore));
        }

        inv.setItem(plugin.getGuiConfig().getInt("layout.pot-slot", 22), buildPotItem());
        inv.setItem(18, item(Material.ARROW, "&c&lBack", "&7Return to casino menu"));
        inv.setItem(26, item(Material.NETHER_STAR, "&6&lFree Spins",
                "&7Available: &f" + gambleService.getFreeSpins(player),
                "&7Each free spin uses your",
                "&7selected slot tier without",
                "&7charging your balance."));
        player.openInventory(inv);
    }

    public void openSpinBase(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, ColorUtil.color(plugin.getConfig().getString("settings.spinning-title", "&d&lSpinning...")));
        Material filler = material(plugin.getGuiConfig().getString("layout.filler", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, item(filler, "&7"));
        }

        // Decor frame
        inv.setItem(2, item(Material.PURPLE_STAINED_GLASS_PANE, "&5"));
        inv.setItem(6, item(Material.PURPLE_STAINED_GLASS_PANE, "&5"));
        inv.setItem(11, item(Material.AMETHYST_SHARD, "&d&lPAYLINE"));
        inv.setItem(15, item(Material.AMETHYST_SHARD, "&d&lPAYLINE"));
        inv.setItem(20, item(Material.PURPLE_STAINED_GLASS_PANE, "&5"));
        inv.setItem(24, item(Material.PURPLE_STAINED_GLASS_PANE, "&5"));

        inv.setItem(plugin.getGuiConfig().getInt("layout.pot-slot", 22), buildPotItem());
        player.openInventory(inv);
    }

    public ItemStack buildPotItem() {
        ItemStack item = item(Material.SUNFLOWER, "&d&lJackpot Pot");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ArrayList<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Current pot: &d" + MoneyUtil.moneyCommas(plugin.getDataStore().getJackpot())));
            int filled = (int) Math.max(0, Math.min(10, Math.round(plugin.getDataStore().getJackpot() / 10000000.0)));
            StringBuilder bar = new StringBuilder();
            for (int i = 1; i <= 10; i++) {
                bar.append(i <= filled ? "§d█" : "§8█");
            }
            lore.add(ColorUtil.color("&7Progress: " + bar));
            lore.add(ColorUtil.color(plugin.getHappyHourService().isActive()
                    ? "&7Happy Hour: &aACTIVE"
                    : "&7Happy Hour: &cOFF"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack glowingItem(Material material, String name, String... lore) {
        ItemStack item = item(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ArrayList<String> builtLore = new ArrayList<>();
        for (String line : lore) {
            builtLore.add(ColorUtil.color(line));
        }
        return item(material, ColorUtil.color(name), builtLore);
    }

    private ItemStack item(Material material, String name) {
        return item(material, name, new ArrayList<>());
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material material(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String trimDouble(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    public record PlayerStatsView(
            double wagered,
            double won,
            double profit,
            int wins,
            int losses,
            double biggestWin,
            int jackpots
    ) {}
}
