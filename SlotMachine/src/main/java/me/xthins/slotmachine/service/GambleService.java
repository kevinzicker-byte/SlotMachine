package me.xthins.slotmachine.service;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.BetTier;
import me.xthins.slotmachine.model.GambleProfile;
import me.xthins.slotmachine.model.SpinResult;
import me.xthins.slotmachine.util.MoneyUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GambleService {
    private final SlotMachinePlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> spinning = new HashMap<>();

    public GambleService(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSpinning(UUID uuid) {
        Long until = spinning.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public void setSpinning(UUID uuid) {
        spinning.put(uuid, System.currentTimeMillis() + (plugin.getSettings().bedrockTimeoutSeconds() * 1000L));
    }

    public void clearSpinning(UUID uuid) {
        spinning.remove(uuid);
    }

    public boolean canAfford(Player player, BetTier tier) {
        Economy economy = plugin.getEconomy();
        return economy != null && economy.has(player, tier.cost());
    }

    public boolean withdraw(Player player, BetTier tier) {
        Economy economy = plugin.getEconomy();
        if (economy == null) return false;
        if (!economy.has(player, tier.cost())) return false;
        economy.withdrawPlayer(player, tier.cost());
        GambleProfile profile = plugin.getDataStore().profile(player.getUniqueId(), player.getName());
        profile.setLastKnownName(player.getName());
        profile.addWagered(tier.cost());
        contributeToJackpot(tier.cost());
        plugin.getDataStore().save();
        return true;
    }

    private void contributeToJackpot(double bet) {
        double pot = plugin.getDataStore().jackpotPot();
        pot += bet * (plugin.getSettings().jackpotContributionPercent() / 100.0);
        plugin.getDataStore().setJackpotPot(pot);
        maybeBroadcastMilestone(pot);
    }

    private void maybeBroadcastMilestone(double pot) {
        double last = plugin.getDataStore().lastMilestoneBroadcast();
        for (Double milestone : plugin.getSettings().jackpotMilestones()) {
            if (pot >= milestone && milestone > last) {
                Bukkit.broadcastMessage(plugin.getMessagesService().format("jackpot-milestone", Map.of(
                        "amount", MoneyUtil.money(milestone, false, plugin.getSettings().currencySymbol())
                )));
                plugin.getDataStore().setLastMilestoneBroadcast(milestone);
            }
        }
    }

    public SpinResult roll(Player player, BetTier tier) {
        double effectiveJackpotChance = plugin.getSettings().jackpotChancePercent();
        if (plugin.getHappyHourService().isActive() && plugin.getSettings().doubleJackpotChance()) {
            effectiveJackpotChance *= 2.0;
        }

        double roll = random.nextDouble() * 100.0;
        if (roll <= effectiveJackpotChance) {
            return new SpinResult(SpinResult.ResultType.JACKPOT, plugin.getDataStore().jackpotPot(), Material.EMERALD_BLOCK, MoneyUtil.money(plugin.getDataStore().jackpotPot(), false, plugin.getSettings().currencySymbol()));
        }

        double tierRoll = random.nextDouble();
        if (tierRoll < 0.08) {
            return payoutResult(SpinResult.ResultType.BIG, tier, Material.DIAMOND_BLOCK);
        }
        if (tierRoll < 0.20) {
            return payoutResult(SpinResult.ResultType.MEDIUM, tier, Material.GOLD_BLOCK);
        }
        if (tierRoll < 0.38) {
            return payoutResult(SpinResult.ResultType.SMALL, tier, Material.IRON_BLOCK);
        }
        if (tierRoll < 0.50) {
            return new SpinResult(SpinResult.ResultType.NEAR_MISS, 0.0, Material.EMERALD, plugin.getSettings().currencySymbol() + "0");
        }
        return new SpinResult(SpinResult.ResultType.LOSS, 0.0, Material.COAL_BLOCK, plugin.getSettings().currencySymbol() + "0");
    }

    private SpinResult payoutResult(SpinResult.ResultType type, BetTier tier, Material material) {
        double mult;
        if (type == SpinResult.ResultType.BIG) {
            mult = randomBetween(Math.max(tier.minMultiplier(), 5.0), Math.max(tier.maxMultiplier(), 6.0));
        } else if (type == SpinResult.ResultType.MEDIUM) {
            mult = randomBetween(Math.max(2.0, Math.min(tier.minMultiplier(), tier.maxMultiplier())), Math.max(2.1, Math.min(tier.maxMultiplier(), 7.0)));
        } else {
            mult = randomBetween(tier.minMultiplier(), Math.min(tier.maxMultiplier(), 2.5));
        }
        double payout = tier.cost() * mult;
        if (plugin.getHappyHourService().isActive()) {
            payout *= plugin.getHappyHourService().multiplier();
        }
        return new SpinResult(type, payout, material, MoneyUtil.money(payout, false, plugin.getSettings().currencySymbol()));
    }

    private double randomBetween(double min, double max) {
        if (max <= min) return min;
        return min + (random.nextDouble() * (max - min));
    }

    public void applyResult(Player player, BetTier tier, SpinResult result) {
        Economy economy = plugin.getEconomy();
        GambleProfile profile = plugin.getDataStore().profile(player.getUniqueId(), player.getName());
        profile.setLastKnownName(player.getName());

        if (result.type() == SpinResult.ResultType.JACKPOT) {
            if (economy != null) economy.depositPlayer(player, result.payout());
            profile.addWon(result.payout());
            profile.addWin();
            profile.addJackpot();
            profile.trackBiggestWin(result.payout());

            double won = plugin.getDataStore().jackpotPot();
            plugin.getDataStore().setJackpotPot(plugin.getSettings().jackpotBasePot());
            plugin.getDataStore().setLastMilestoneBroadcast(0.0);
            String entry = player.getName() + " - " + MoneyUtil.money(won, false, plugin.getSettings().currencySymbol()) + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            plugin.getDataStore().addJackpotHistory(entry);
            Bukkit.broadcastMessage(plugin.getMessagesService().format("jackpot-win-broadcast", Map.of(
                    "player", player.getName(),
                    "amount", MoneyUtil.money(won, false, plugin.getSettings().currencySymbol())
            )));
            player.getWorld().strikeLightningEffect(player.getLocation());
            player.playSound(player.getLocation(), plugin.sound("sounds.jackpot", "ENTITY_LIGHTNING_BOLT_THUNDER"), 1f, 1f);
        } else if (result.payout() > 0) {
            if (economy != null) economy.depositPlayer(player, result.payout());
            profile.addWon(result.payout());
            profile.addWin();
            profile.trackBiggestWin(result.payout());
            player.sendMessage(plugin.getMessagesService().format("regular-win", Map.of("amount", MoneyUtil.money(result.payout(), false, plugin.getSettings().currencySymbol()))));
            player.playSound(player.getLocation(), plugin.sound("sounds.spin-win", "ENTITY_PLAYER_LEVELUP"), 1f, 1f);
        } else {
            profile.addLoss();
            if (result.type() == SpinResult.ResultType.NEAR_MISS) {
                player.sendMessage(plugin.getMessagesService().format("near-miss", Map.of()));
            } else {
                player.sendMessage(plugin.getMessagesService().format("regular-loss", Map.of()));
            }
            player.playSound(player.getLocation(), plugin.sound("sounds.spin-lose", "ENTITY_ITEM_BREAK"), 1f, 1f);
        }

        plugin.getDataStore().save();
    }

    public void clearPlayer(OfflinePlayer player) {
        if (player == null) return;
        clearSpinning(player.getUniqueId());
    }
}
