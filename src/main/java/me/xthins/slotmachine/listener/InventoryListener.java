package me.xthins.slotmachine.listener;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.gui.GambleMenu;
import me.xthins.slotmachine.model.BetTier;
import me.xthins.slotmachine.model.SpinOutcome;
import me.xthins.slotmachine.service.GambleService;
import me.xthins.slotmachine.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class InventoryListener implements Listener {
    private final SlotMachinePlugin plugin;
    private final GambleService gambleService;
    private final GambleMenu gambleMenu;

    public InventoryListener(SlotMachinePlugin plugin, GambleService gambleService, GambleMenu gambleMenu) {
        this.plugin = plugin;
        this.gambleService = gambleService;
        this.gambleMenu = gambleMenu;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(ColorUtil.color(plugin.getConfig().getString("settings.gui-title")))
                && !title.equals(ColorUtil.color(plugin.getConfig().getString("settings.spinning-title")))) {
            return;
        }

        event.setCancelled(true);

        if (!title.equals(ColorUtil.color(plugin.getConfig().getString("settings.gui-title")))) {
            return;
        }

        BetTier tier = null;
        for (BetTier candidate : BetTier.values()) {
            if (event.getRawSlot() == gambleService.getBetSlot(candidate)) {
                tier = candidate;
                break;
            }
        }

        if (tier == null) return;

        if (gambleService.isSpinning(player.getUniqueId())) {
            player.sendMessage(ColorUtil.color(plugin.getMessagesConfig().getString("already-spinning")));
            return;
        }

        String error = gambleService.withdrawBet(player, tier);
        if (error != null) {
            player.sendMessage(error);
            return;
        }

        gambleService.setSpinning(player, true);
        gambleMenu.openSpinBase(player);

        SpinOutcome outcome = gambleService.rollOutcome(tier);
        animate(player, outcome, tier);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (title.equals(ColorUtil.color(plugin.getConfig().getString("settings.spinning-title")))
                && gambleService.isSpinning(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> gambleService.forceClear(player.getUniqueId()), 20L);
        }
    }

    private void animate(Player player, SpinOutcome outcome, BetTier tier) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        int ticks = outcome.isJackpot()
                ? plugin.getConfig().getInt("settings.spin-ticks-jackpot", 34)
                : plugin.getConfig().getInt("settings.spin-ticks-normal", 24);

        int[] top = {3, 4, 5};
        int[] mid = {12, 13, 14};
        int[] bot = {21, 22, 23};

        final int[] step = {0};

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!player.isOnline()) {
                gambleService.forceClear(player.getUniqueId());
                task.cancel();
                return;
            }

            step[0]++;

            if (step[0] > ticks) {
                ItemStack result = new ItemStack(outcome.getSymbol().getMaterial());
                for (int slot : mid) {
                    inv.setItem(slot, result);
                }

                flashPayline(player, inv, mid, result, () -> {
                    gambleService.applyOutcome(player, tier, outcome);
                    gambleService.setSpinning(player, false);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> gambleMenu.openMain(player), 20L);
                });

                task.cancel();
                return;
            }

            // Top and bottom rows can stay random
            for (int i = 0; i < 3; i++) {
                inv.setItem(top[i], new ItemStack(randomAnyMat()));
                inv.setItem(bot[i], new ItemStack(randomAnyMat()));
            }

            // Middle row should NEVER accidentally show a fake winning line during animation
            if (outcome.isNearMiss() && step[0] > ticks - 4) {
                inv.setItem(mid[0], new ItemStack(Material.EMERALD));
                inv.setItem(mid[1], new ItemStack(Material.EMERALD));
                inv.setItem(mid[2], new ItemStack(randomNonMatchingMat(Material.EMERALD)));
            } else {
                Material first = randomAnyMat();
                Material second = randomNonMatchingMat(first);
                Material third = randomNonMatchingMat(first, second);

                inv.setItem(mid[0], new ItemStack(first));
                inv.setItem(mid[1], new ItemStack(second));
                inv.setItem(mid[2], new ItemStack(third));
            }

            float pitch = 1.0f;
            if (outcome.isJackpot()) {
                if (step[0] > ticks - 3) {
                    pitch = 0.6f;
                } else if (step[0] > ticks - 6) {
                    pitch = 0.8f;
                }
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, pitch);
        }, 0L, 2L);
    }

    private Material randomAnyMat() {
        Material[] mats = {
                Material.IRON_INGOT,
                Material.GOLD_INGOT,
                Material.DIAMOND,
                Material.EMERALD
        };
        return mats[ThreadLocalRandom.current().nextInt(mats.length)];
    }

    private Material randomNonMatchingMat(Material not) {
        Material[] mats = {
                Material.IRON_INGOT,
                Material.GOLD_INGOT,
                Material.DIAMOND,
                Material.EMERALD
        };

        Material pick;
        do {
            pick = mats[ThreadLocalRandom.current().nextInt(mats.length)];
        } while (pick == not);

        return pick;
    }

    private Material randomNonMatchingMat(Material not1, Material not2) {
        Material[] mats = {
                Material.IRON_INGOT,
                Material.GOLD_INGOT,
                Material.DIAMOND,
                Material.EMERALD
        };

        Material pick;
        do {
            pick = mats[ThreadLocalRandom.current().nextInt(mats.length)];
        } while (pick == not1 || pick == not2);

        return pick;
    }

    private void flashPayline(Player player, Inventory inv, int[] slots, ItemStack result, Runnable done) {
        final int[] n = {0};

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            n[0]++;

            for (int slot : slots) {
                inv.setItem(slot, n[0] % 2 == 0 ? result : new ItemStack(Material.GLOWSTONE_DUST));
            }

            if (n[0] >= 6) {
                for (int slot : slots) {
                    inv.setItem(slot, result);
                }
                task.cancel();
                done.run();
            }
        }, 0L, 4L);
    }
}
