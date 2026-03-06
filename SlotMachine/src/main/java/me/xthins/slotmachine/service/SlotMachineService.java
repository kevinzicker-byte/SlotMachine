package me.xthins.slotmachine.service;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.BetTier;
import me.xthins.slotmachine.model.JackpotRecord;
import me.xthins.slotmachine.model.PlayerStats;
import me.xthins.slotmachine.util.ColorUtil;
import me.xthins.slotmachine.util.MoneyUtil;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SlotMachineService {
    private final SlotMachinePlugin plugin;
    private final Map<UUID, SpinSession> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private static final DateTimeFormatter HISTORY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public SlotMachineService(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(player, 45, ColorUtil.color(plugin.getConfig().getString("spin.gui-title")));
        ItemStack filler = named(plugin.material(plugin.getConfig().getString("gui.fill-material"), Material.BLACK_STAINED_GLASS_PANE), plugin.getConfig().getString("gui.fill-name", " "));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        List<Integer> top = plugin.getConfig().getIntegerList("reels.top-row-slots");
        List<Integer> middle = plugin.getConfig().getIntegerList("reels.middle-row-slots");
        List<Integer> bottom = plugin.getConfig().getIntegerList("reels.bottom-row-slots");
        for (int slot : top) inv.setItem(slot, named(Material.IRON_INGOT, "&7?"));
        for (int slot : middle) inv.setItem(slot, named(Material.GOLD_INGOT, "&7?"));
        for (int slot : bottom) inv.setItem(slot, named(Material.DIAMOND, "&7?"));

        List<Integer> payline = plugin.getConfig().getIntegerList("reels.payline-slots");
        if (payline.size() >= 2) {
            inv.setItem(payline.get(0), named(Material.ENDER_EYE, plugin.getConfig().getString("gui.payline-left-name", "&f&l>")));
            inv.setItem(payline.get(1), named(Material.ENDER_EYE, plugin.getConfig().getString("gui.payline-right-name", "&f&l<")));
        }

        for (BetTier tier : plugin.getBetTiers()) {
            String cost = plugin.useShortGuiMoney() ? MoneyUtil.formatMoneyShort(plugin.currencySymbol(), tier.cost()) : MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), tier.cost());
            ItemStack item = named(tier.material(), tier.displayName() + " &7- &f" + cost);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Click to spin."));
            lore.add(ColorUtil.color("&8Payout range: &f" + trim(tier.minMultiplier()) + "x - " + trim(tier.maxMultiplier()) + "x"));
            lore.add(ColorUtil.color("&8Win chance: &f" + trim(tier.winChance() * 100) + "%"));
            if (plugin.getDataStore().isHappyHourActive()) {
                lore.add(ColorUtil.color(plugin.getConfig().getString("gui.happy-hour-lore", "&6&lHAPPY HOUR &7x2 &8({minutes}m left)").replace("{minutes}", String.valueOf(plugin.getDataStore().getHappyHourMinutesLeft()))));
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
            inv.setItem(tier.guiSlot(), item);
        }

        inv.setItem(4, createJackpotItem());
        player.openInventory(inv);
    }

    public ItemStack createJackpotItem() {
        Material material = plugin.material(plugin.getConfig().getString("gui.jackpot-item-material"), Material.SUNFLOWER);
        ItemStack item = named(material, plugin.getConfig().getString("gui.jackpot-item-name", "&d&lJackpot Pot"));
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtil.color("&7Current pot: &d" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), plugin.getDataStore().getJackpotPot())));
        lore.add(ColorUtil.color("&7Progress: " + progressBar(plugin.getDataStore().getJackpotPot())));
        lore.add(ColorUtil.color("&8Chance: &f" + trim(currentJackpotChance() * 100) + "%"));
        if (plugin.getDataStore().isHappyHourActive()) {
            lore.add(ColorUtil.color(plugin.getConfig().getString("gui.happy-hour-lore").replace("{minutes}", String.valueOf(plugin.getDataStore().getHappyHourMinutesLeft()))));
        } else {
            lore.add(ColorUtil.color(plugin.getConfig().getString("gui.happy-hour-off-lore")));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSpinning(UUID uuid) { return sessions.containsKey(uuid); }
    public void clearSpin(UUID uuid) { sessions.remove(uuid); }

    public void startSpin(Player player, BetTier tier) {
        if (isSpinning(player.getUniqueId())) {
            player.sendMessage(ColorUtil.color(plugin.message("already-spinning")));
            return;
        }
        if (plugin.getEconomy().getBalance(player) < tier.cost()) {
            player.sendMessage(ColorUtil.color(plugin.message("insufficient-funds")));
            return;
        }

        EconomyResponse withdraw = plugin.getEconomy().withdrawPlayer(player, tier.cost());
        if (!withdraw.transactionSuccess()) {
            player.sendMessage(ColorUtil.color(plugin.message("insufficient-funds")));
            return;
        }

        PlayerStats stats = plugin.getDataStore().getOrCreate(player.getUniqueId());
        stats.setLastName(player.getName());
        stats.setFactionTag(plugin.getFactionService().getFactionTag(player));
        stats.addBet();
        stats.addWagered(tier.cost());

        double contribution = Math.max(1, Math.round(tier.cost() * (plugin.getConfig().getDouble("jackpot.contribution-percent", 25.0) / 100.0)));
        double newPot = plugin.getDataStore().getJackpotPot() + contribution;
        plugin.getDataStore().setJackpotPot(newPot);
        broadcastMilestones(newPot);

        boolean jackpot = random.nextDouble() < currentJackpotChance();
        boolean nearMiss = !jackpot && random.nextDouble() < 0.18;
        double payout = 0;
        double appliedMultiplier = 0;
        Material resultSymbol;

        if (jackpot) {
            payout = plugin.getDataStore().getJackpotPot();
            resultSymbol = Material.EMERALD;
        } else {
            boolean win = random.nextDouble() < tier.winChance();
            if (win) {
                appliedMultiplier = tier.minMultiplier() + (random.nextDouble() * (tier.maxMultiplier() - tier.minMultiplier()));
                payout = Math.round(plugin.getHappyHourService().apply(tier.cost() * appliedMultiplier));
                resultSymbol = payout >= tier.cost() * 10 ? Material.DIAMOND_BLOCK : Material.DIAMOND;
            } else {
                resultSymbol = nearMiss ? Material.EMERALD : Material.REDSTONE;
            }
        }

        Inventory inv = Bukkit.createInventory(player, 45, ColorUtil.color(plugin.getConfig().getString("gui.spin-title", "&d&lSPINNING...")));
        ItemStack filler = named(plugin.material(plugin.getConfig().getString("gui.fill-material"), Material.BLACK_STAINED_GLASS_PANE), plugin.getConfig().getString("gui.fill-name", " "));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        List<Integer> payline = plugin.getConfig().getIntegerList("reels.payline-slots");
        if (payline.size() >= 2) {
            inv.setItem(payline.get(0), named(Material.ENDER_EYE, plugin.getConfig().getString("gui.payline-left-name", "&f&l>")));
            inv.setItem(payline.get(1), named(Material.ENDER_EYE, plugin.getConfig().getString("gui.payline-right-name", "&f&l<")));
        }
        inv.setItem(4, createJackpotItem());
        player.openInventory(inv);

        SpinSession session = new SpinSession(player.getUniqueId(), tier, payout, jackpot, nearMiss, resultSymbol, System.currentTimeMillis());
        sessions.put(player.getUniqueId(), session);

        int timeout = plugin.getConfig().getInt("spin.bedrock-timeout-seconds", 20);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            SpinSession current = sessions.get(player.getUniqueId());
            if (current != null && current == session) {
                sessions.remove(player.getUniqueId());
                player.sendMessage(ColorUtil.color(plugin.message("spin-reset")));
            }
        }, timeout * 20L);

        animate(player, inv, session, appliedMultiplier);
    }

    private void animate(Player player, Inventory inv, SpinSession session, double appliedMultiplier) {
        List<Integer> top = plugin.getConfig().getIntegerList("reels.top-row-slots");
        List<Integer> middle = plugin.getConfig().getIntegerList("reels.middle-row-slots");
        List<Integer> bottom = plugin.getConfig().getIntegerList("reels.bottom-row-slots");
        Material[] symbols = {Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD, Material.REDSTONE, Material.COAL};
        int normalTicks = plugin.getConfig().getInt("spin.ticks-normal", 24);
        int extra = session.jackpot ? plugin.getConfig().getInt("spin.jackpot-extra-slow-ticks", 10) : 0;
        int totalTicks = normalTicks + extra;
        Sound click = plugin.parseSound(plugin.getConfig().getString("sounds.reel-click", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK);
        Sound winSound = plugin.parseSound(session.jackpot ? plugin.getConfig().getString("sounds.jackpot", "ENTITY_LIGHTNING_BOLT_THUNDER") : plugin.getConfig().getString("sounds.spin-win", "ENTITY_PLAYER_LEVELUP"), Sound.ENTITY_PLAYER_LEVELUP);
        Sound loseSound = plugin.parseSound(plugin.getConfig().getString("sounds.spin-lose", "ENTITY_ITEM_BREAK"), Sound.ENTITY_ITEM_BREAK);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    sessions.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                tick++;
                boolean slow = session.jackpot && tick > normalTicks;
                for (int col = 0; col < 3; col++) {
                    Material topMat = symbols[random.nextInt(symbols.length)];
                    Material midMat = symbols[random.nextInt(symbols.length)];
                    Material botMat = symbols[random.nextInt(symbols.length)];
                    if (session.nearMiss && !session.jackpot && tick > totalTicks - 6 && col < 2) {
                        midMat = Material.EMERALD;
                    }
                    if (session.jackpot && tick > totalTicks - (6 - col * 2)) {
                        midMat = Material.EMERALD;
                    }
                    inv.setItem(top.get(col), named(topMat, "&7 "));
                    inv.setItem(middle.get(col), named(midMat, "&7 "));
                    inv.setItem(bottom.get(col), named(botMat, "&7 "));
                }
                player.playSound(player.getLocation(), click, 1f, slow ? 0.6f : 1f);
                if (tick >= totalTicks) {
                    finish(player, inv, session, appliedMultiplier, winSound, loseSound);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void finish(Player player, Inventory inv, SpinSession session, double appliedMultiplier, Sound winSound, Sound loseSound) {
        List<Integer> top = plugin.getConfig().getIntegerList("reels.top-row-slots");
        List<Integer> middle = plugin.getConfig().getIntegerList("reels.middle-row-slots");
        List<Integer> bottom = plugin.getConfig().getIntegerList("reels.bottom-row-slots");

        Material fillerTop = session.resultSymbol == Material.EMERALD ? Material.DIAMOND : Material.IRON_INGOT;
        Material fillerBottom = session.resultSymbol == Material.EMERALD ? Material.DIAMOND : Material.GOLD_INGOT;
        for (int i = 0; i < 3; i++) {
            inv.setItem(top.get(i), named(fillerTop, "&7 "));
            inv.setItem(middle.get(i), named(session.resultSymbol, "&f "));
            inv.setItem(bottom.get(i), named(fillerBottom, "&7 "));
        }

        List<Integer> payline = plugin.getConfig().getIntegerList("reels.payline-slots");
        new BukkitRunnable() {
            int flashes = 0;
            boolean on = false;
            @Override
            public void run() {
                on = !on;
                Material mat = on ? Material.GLOWSTONE : Material.ENDER_EYE;
                String name = on ? "&e&l>" : plugin.getConfig().getString("gui.payline-left-name", "&f&l>");
                if (payline.size() >= 2) {
                    inv.setItem(payline.get(0), named(mat, name));
                    inv.setItem(payline.get(1), named(mat, name.replace('>', '<')));
                }
                flashes++;
                if (flashes >= plugin.getConfig().getInt("spin.flash-cycles", 4) * 2) {
                    payout(player, session, appliedMultiplier, winSound, loseSound);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void payout(Player player, SpinSession session, double appliedMultiplier, Sound winSound, Sound loseSound) {
        PlayerStats stats = plugin.getDataStore().getOrCreate(player.getUniqueId());
        if (session.jackpot) {
            plugin.getEconomy().depositPlayer(player, session.payout);
            stats.addWin();
            stats.addWon(session.payout);
            stats.setBiggestWin(session.payout);
            stats.addJackpot();
            plugin.getDataStore().addJackpotRecord(new JackpotRecord(player.getName(), session.payout, Instant.now().toEpochMilli()), 10);
            plugin.getDataStore().setJackpotPot(plugin.getConfig().getDouble("jackpot.base-pot", 10_000_000));
            plugin.broadcast("jackpot-win-broadcast", "player", player.getName(), "amount", MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), session.payout));
            player.getWorld().strikeLightningEffect(player.getLocation());
            player.playSound(player.getLocation(), winSound, 1f, 1f);
        } else if (session.payout > 0) {
            plugin.getEconomy().depositPlayer(player, session.payout);
            stats.addWin();
            stats.addWon(session.payout);
            stats.setBiggestWin(session.payout);
            player.sendMessage(ColorUtil.color(plugin.message("regular-win").replace("{amount}", MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), session.payout))));
            player.playSound(player.getLocation(), winSound, 1f, 1f);
        } else {
            stats.addLoss();
            if (session.nearMiss) {
                player.sendMessage(ColorUtil.color(plugin.message("near-miss")));
            } else {
                player.sendMessage(ColorUtil.color(plugin.message("regular-loss")));
            }
            player.playSound(player.getLocation(), loseSound, 1f, 1f);
        }
        sessions.remove(player.getUniqueId());
        plugin.getDataStore().save();
        plugin.getHologramService().updateAll();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openMenu(player), 20L);
    }

    public List<PlayerStats> topPlayers() {
        return plugin.getDataStore().allStats().stream().sorted(Comparator.comparingDouble(PlayerStats::getProfit).reversed()).toList();
    }

    public Map<String, Double> topFactions() {
        Map<String, Double> totals = new HashMap<>();
        for (PlayerStats stats : plugin.getDataStore().allStats()) {
            if (stats.getFactionTag() == null || stats.getFactionTag().isBlank()) continue;
            totals.merge(stats.getFactionTag(), stats.getProfit(), Double::sum);
        }
        return totals.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(10)
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    public String statsMessage(Player player) {
        PlayerStats stats = plugin.getDataStore().getOrCreate(player.getUniqueId());
        return ColorUtil.color(plugin.prefix() + " &7Wagered: &f" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), stats.getWagered())
                + " &8| &7Won: &a" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), stats.getWon())
                + " &8| &7Profit: &6" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), stats.getProfit())
                + "\n" + "&7Wins: &a" + stats.getWins() + " &8| &7Losses: &c" + stats.getLosses()
                + " &8| &7Biggest: &b" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), stats.getBiggestWin())
                + " &8| &7Jackpots: &d" + stats.getJackpots());
    }

    public List<String> historyLines() {
        if (plugin.getDataStore().getJackpotHistory().isEmpty()) return List.of(ColorUtil.color(plugin.message("no-history")));
        List<String> lines = new ArrayList<>();
        lines.add(ColorUtil.color(plugin.prefix() + " &d&lRecent Jackpots"));
        int i = 1;
        for (JackpotRecord record : plugin.getDataStore().getJackpotHistory()) {
            lines.add(ColorUtil.color("&f#" + i + " &7" + record.playerName() + " &8- &6" + MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), record.amount()) + " &8(" + HISTORY_FORMAT.format(Instant.ofEpochMilli(record.timestamp())) + ")"));
            if (++i > 10) break;
        }
        return lines;
    }

    private void broadcastMilestones(double currentPot) {
        for (double milestone : plugin.getConfig().getDoubleList("jackpot.milestone-broadcasts")) {
            String key = "milestone." + (long) milestone;
            if (currentPot >= milestone && !plugin.getConfig().getBoolean("runtime." + key, false)) {
                plugin.getConfig().set("runtime." + key, true);
                plugin.saveConfig();
                plugin.broadcast("jackpot-milestone", "amount", MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), milestone));
            }
        }
    }

    private double currentJackpotChance() {
        double chancePercent = plugin.getConfig().getDouble("jackpot.chance-percent", 0.35);
        if (plugin.getDataStore().isHappyHourActive() && plugin.getConfig().getBoolean("happy-hour.double-jackpot-chance", true)) {
            chancePercent *= 2.0;
        }
        return chancePercent / 100.0;
    }

    private ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String progressBar(double amount) {
        int filled = (int) Math.max(0, Math.min(10, Math.round(amount / (100_000_000.0 / 10.0))));
        StringBuilder sb = new StringBuilder();
        String full = plugin.getConfig().getString("gui.jackpot-progress-filled", "&d█");
        String empty = plugin.getConfig().getString("gui.jackpot-progress-empty", "&8█");
        for (int i = 0; i < 10; i++) sb.append(i < filled ? full : empty);
        return sb.toString();
    }

    private String trim(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public record SpinSession(UUID uuid, BetTier tier, double payout, boolean jackpot, boolean nearMiss, Material resultSymbol, long startedAt) {}
}
