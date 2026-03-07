package me.xthins.slotmachine.service;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.hook.SaberFactionsHook;
import me.xthins.slotmachine.hook.VaultHook;
import me.xthins.slotmachine.model.BetTier;
import me.xthins.slotmachine.model.PlayerStats;
import me.xthins.slotmachine.model.ReelSymbol;
import me.xthins.slotmachine.model.SpinOutcome;
import me.xthins.slotmachine.storage.YamlDataStore;
import me.xthins.slotmachine.util.ColorUtil;
import me.xthins.slotmachine.util.MoneyUtil;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GambleService {
    private final SlotMachinePlugin plugin;
    private final VaultHook vaultHook;
    private final SaberFactionsHook factionsHook;
    private final YamlDataStore dataStore;
    private final HappyHourService happyHourService;
    private final Set<UUID> spinning = new HashSet<>();
    private final Map<UUID, Long> spinStartedAt = new HashMap<>();

    public GambleService(SlotMachinePlugin plugin, VaultHook vaultHook, SaberFactionsHook factionsHook, YamlDataStore dataStore, HappyHourService happyHourService) {
        this.plugin = plugin;
        this.vaultHook = vaultHook;
        this.factionsHook = factionsHook;
        this.dataStore = dataStore;
        this.happyHourService = happyHourService;
    }

    public boolean isSpinning(UUID uuid) { return spinning.contains(uuid); }

    public void setSpinning(Player player, boolean value) {
        UUID uuid = player.getUniqueId();
        if (value) {
            spinning.add(uuid);
            spinStartedAt.put(uuid, System.currentTimeMillis());
        } else {
            spinning.remove(uuid);
            spinStartedAt.remove(uuid);
        }
    }

    public void forceClear(UUID uuid) {
        spinning.remove(uuid);
        spinStartedAt.remove(uuid);
    }

    public int getBetSlot(BetTier tier) { return plugin.getConfig().getInt("bets." + tier.getPath() + ".slot"); }
    public double getBetAmount(BetTier tier) { return plugin.getConfig().getDouble("bets." + tier.getPath() + ".amount"); }
    public String getBetDisplayName(BetTier tier) { return plugin.getConfig().getString("bets." + tier.getPath() + ".display-name", tier.name()); }
    public String getBetMaterial(BetTier tier) { return plugin.getConfig().getString("bets." + tier.getPath() + ".material", "PAPER"); }

    public String withdrawBet(Player player, BetTier tier) {
        double bet = getBetAmount(tier);
        EconomyResponse response = vaultHook.getEconomy().withdrawPlayer(player, bet);
        if (!response.transactionSuccess()) {
            return ColorUtil.color(plugin.getMessagesConfig().getString("not-enough-money", "&cNot enough money."));
        }

        addToJackpot(bet * (plugin.getConfig().getDouble("settings.pot-contribution-percent", 25.0) / 100.0));
        PlayerStats stats = dataStore.getStats(player.getUniqueId());
        stats.setPlayerName(player.getName());
        stats.setWagered(stats.getWagered() + bet);
        if (factionsHook.isEnabled()) {
            stats.setLastKnownFaction(factionsHook.getFactionTag(player));
        }
        return null;
    }

    public SpinOutcome rollOutcome(BetTier tier) {
        double jackpotChance = plugin.getConfig().getDouble("settings.jackpot-chance", 0.0025) * happyHourService.getJackpotChanceMultiplier();
        if (ThreadLocalRandom.current().nextDouble() < jackpotChance) {
            return new SpinOutcome(true, true, false, dataStore.getJackpot(), ReelSymbol.EMERALD);
        }

        double winChance = plugin.getConfig().getDouble("bets." + tier.getPath() + ".win-chance", 0.25);
        boolean won = ThreadLocalRandom.current().nextDouble() < winChance;
        boolean nearMiss = !won && tier == BetTier.BIG && ThreadLocalRandom.current().nextDouble() < 0.18;
        if (!won) return new SpinOutcome(false, false, nearMiss, 0.0, nearMiss ? ReelSymbol.EMERALD : ReelSymbol.IRON);

        double min = plugin.getConfig().getDouble("bets." + tier.getPath() + ".min-multiplier", 1.2);
        double max = plugin.getConfig().getDouble("bets." + tier.getPath() + ".max-multiplier", 2.0);
        double payout = getBetAmount(tier) * ThreadLocalRandom.current().nextDouble(min, max) * happyHourService.getPayoutMultiplier();
        ReelSymbol symbol = switch (tier) {
            case SMALL -> ReelSymbol.IRON;
            case MEDIUM -> ReelSymbol.GOLD;
            case BIG -> ReelSymbol.DIAMOND;
        };
        return new SpinOutcome(true, false, false, payout, symbol);
    }

    public void applyOutcome(Player player, BetTier tier, SpinOutcome outcome) {
        PlayerStats stats = dataStore.getStats(player.getUniqueId());
        stats.setPlayerName(player.getName());
        if (factionsHook.isEnabled()) stats.setLastKnownFaction(factionsHook.getFactionTag(player));

        if (outcome.isWon()) {
            vaultHook.getEconomy().depositPlayer(player, outcome.getPayout());
            stats.setWon(stats.getWon() + outcome.getPayout());
            stats.setWins(stats.getWins() + 1);
            stats.setBiggestWin(Math.max(stats.getBiggestWin(), outcome.getPayout()));
            player.sendMessage(ColorUtil.color(plugin.getMessagesConfig().getString("prefix") + " &aYou won " + MoneyUtil.moneyCommas(outcome.getPayout()) + "&a!"));
        } else {
            stats.setLosses(stats.getLosses() + 1);
            player.sendMessage(ColorUtil.color(plugin.getMessagesConfig().getString("prefix") + " &cUnlucky. Better luck next spin."));
        }

        if (outcome.isJackpot()) {
            stats.setJackpots(stats.getJackpots() + 1);
            for (String line : plugin.getMessagesConfig().getStringList("jackpot-broadcast")) {
                Bukkit.broadcastMessage(ColorUtil.color(line
                        .replace("{prefix}", plugin.getMessagesConfig().getString("prefix"))
                        .replace("{player}", player.getName())
                        .replace("{amount}", MoneyUtil.moneyCommas(outcome.getPayout()))));
            }
            player.getWorld().strikeLightningEffect(player.getLocation());
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
            dataStore.addJackpotHistory(player.getName() + " - " + MoneyUtil.moneyCommas(outcome.getPayout()));
            dataStore.setJackpot(plugin.getConfig().getDouble("settings.jackpot-reset-base", 10000000));
        } else if (outcome.isWon()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    public void addToJackpot(double amount) {
        dataStore.setJackpot(dataStore.getJackpot() + amount);
        if (plugin.getConfig().getBoolean("settings.announce-milestones", true)) {
            for (double milestone : plugin.getConfig().getDoubleList("settings.milestone-values")) {
                if (dataStore.getJackpot() >= milestone && !dataStore.getAnnouncedMilestones().contains(milestone)) {
                    dataStore.getAnnouncedMilestones().add(milestone);
                    String msg = plugin.getMessagesConfig().getString("milestone")
                            .replace("{prefix}", plugin.getMessagesConfig().getString("prefix"))
                            .replace("{amount}", MoneyUtil.moneyCommas(milestone));
                    Bukkit.broadcastMessage(ColorUtil.color(msg));
                }
            }
        }
    }

    public java.util.List<String> buildTopMessages() {
        java.util.List<Map.Entry<UUID, PlayerStats>> list = new java.util.ArrayList<>(dataStore.getAllStats().entrySet());
        list.sort((a,b) -> Double.compare(b.getValue().getProfit(), a.getValue().getProfit()));
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add(ColorUtil.color("&6Top Profit"));
        int rank = 0;
        for (Map.Entry<UUID, PlayerStats> entry : list) {
            rank++;
            if (rank > 10) break;
            PlayerStats s = entry.getValue();
            String name = s.getPlayerName() == null ? entry.getKey().toString() : s.getPlayerName();
            out.add(ColorUtil.color("&f" + rank + ". &7" + name + " &7- &a" + MoneyUtil.moneyCommas(s.getProfit())));
            out.add(ColorUtil.color("&7  &8» &7Wagered: &f" + MoneyUtil.moneyCommas(s.getWagered()) + " &8| &aW:" + s.getWins() + " &cL:" + s.getLosses()));
            out.add(ColorUtil.color("&7  &8» &7Biggest: &b" + MoneyUtil.moneyCommas(s.getBiggestWin()) + " &8| &dJackpots: &5" + s.getJackpots()));
        }
        if (out.size() == 1) out.add(ColorUtil.color("&7No data yet."));
        return out;
    }

    public String buildStatsMessage(Player player) {
        PlayerStats s = dataStore.getStats(player.getUniqueId());
        return ColorUtil.color("&d&lStats &8» &7Wagered: &f" + MoneyUtil.moneyCommas(s.getWagered())
                + " &8| &7Profit: &a" + MoneyUtil.moneyCommas(s.getProfit())
                + " &8| &7Biggest: &b" + MoneyUtil.moneyCommas(s.getBiggestWin())
                + " &8| &dJackpots: &5" + s.getJackpots());
    }

    public String buildPotMessage() {
        return ColorUtil.color(plugin.getMessagesConfig().getString("prefix") + " &7Current pot: &d" + MoneyUtil.moneyCommas(dataStore.getJackpot()));
    }

    public java.util.List<String> buildHistoryMessages() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(ColorUtil.color("&d&lJackpot History"));
        if (dataStore.getJackpotHistory().isEmpty()) lines.add(ColorUtil.color("&7No jackpots yet."));
        for (String h : dataStore.getJackpotHistory()) lines.add(ColorUtil.color("&7- &f" + h));
        return lines;
    }

    public void clearTimedOutSpins() {
        long maxMs = plugin.getConfig().getInt("settings.bedrock-safe-timeout-seconds", 15) * 1000L;
        long now = System.currentTimeMillis();
        for (UUID uuid : new java.util.ArrayList<>(spinStartedAt.keySet())) {
            if (now - spinStartedAt.getOrDefault(uuid, now) > maxMs) {
                forceClear(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(ColorUtil.color(plugin.getMessagesConfig().getString("spin-reset")
                        .replace("{prefix}", plugin.getMessagesConfig().getString("prefix"))));
            }
        }
    }
}
