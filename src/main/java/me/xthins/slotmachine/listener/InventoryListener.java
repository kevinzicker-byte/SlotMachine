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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class InventoryListener implements Listener {
    private final SlotMachinePlugin plugin;
    private final GambleService gambleService;
    private final GambleMenu gambleMenu;

    private final Map<UUID, BukkitTask> spinTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> finishTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> reopenTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> flashTasks = new HashMap<>();

    public InventoryListener(SlotMachinePlugin plugin, GambleService gambleService, GambleMenu gambleMenu) {
        this.plugin = plugin;
        this.gambleService = gambleService;
        this.gambleMenu = gambleMenu;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        String mainTitle = ColorUtil.color(plugin.getConfig().getString("settings.gui-title", "&d&lCasino"));
        String slotsTitle = ColorUtil.color("&d&lSlots");
        String spinningTitle = ColorUtil.color(plugin.getConfig().getString("settings.spinning-title", "&d&lSpinning..."));

        if (!title.equals(mainTitle) && !title.equals(slotsTitle) && !title.equals(spinningTitle)) {
            return;
        }

        event.setCancelled(true);

        if (title.equals(spinningTitle)) {
            return;
        }

        if (title.equals(mainTitle)) {
            handleMainMenuClick(player, event.getRawSlot());
            return;
        }

        handleSlotsMenuClick(player, event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();
        String spinningTitle = ColorUtil.color(plugin.getConfig().getString("settings.spinning-title", "&d&lSpinning..."));

        gambleService.stopCasinoSounds(player);

        if (title.equals(spinningTitle)) {
            hardCancelSpin(player);
        }
    }

    private void handleMainMenuClick(Player player, int rawSlot) {
        switch (rawSlot) {
            case 10, 11, 15, 16 -> {
                player.sendMessage(ColorUtil.color("&cThat game is coming in phase 2."));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.9f);
            }
            case 13 -> gambleMenu.openSlotsMenu(player);
            default -> {
            }
        }
    }

    private void handleSlotsMenuClick(Player player, InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();

        if (rawSlot == 18) {
            gambleService.stopCasinoSounds(player);
            hardCancelSpin(player);
            gambleMenu.openMain(player);
            return;
        }

        BetTier tier = null;
        for (BetTier candidate : BetTier.values()) {
            if (rawSlot == gambleService.getBetSlot(candidate)) {
                tier = candidate;
                break;
            }
        }

        if (tier == null) return;

        if (gambleService.isSpinning(player.getUniqueId())) {
            player.sendMessage(ColorUtil.color(plugin.getMessagesConfig().getString("already-spinning", "&cAlready spinning.")));
            return;
        }

        boolean useFreeSpin = event.getClick() == ClickType.RIGHT && gambleService.canUseFreeSpin(player);
        String error = gambleService.withdrawBet(player, tier, useFreeSpin);
        if (error != null) {
            player.sendMessage(error);
            return;
        }

        hardCancelSpin(player);
        gambleService.setSpinning(player, true);
        gambleService.stopCasinoSounds(player);
        gambleMenu.openSpinBase(player);

        SpinOutcome outcome = gambleService.rollOutcome(tier);
        animate(player, outcome, tier);
    }

    private void animate(Player player, SpinOutcome outcome, BetTier tier) {
        UUID uuid = player.getUniqueId();
        Inventory inv = player.getOpenInventory().getTopInventory();
        String spinningTitle = ColorUtil.color(plugin.getConfig().getString("settings.spinning-title", "&d&lSpinning..."));

        int baseTicks = outcome.isJackpot()
                ? plugin.getConfig().getInt("settings.spin-ticks-jackpot", 42)
                : plugin.getConfig().getInt("settings.spin-ticks-normal", 30);

        int[] top = {3, 4, 5};
        int[] mid = {12, 13, 14};
        int[] bot = {21, 22, 23};

        final int[] step = {0};

        final int leftStop = Math.max(8, baseTicks - 10);
        final int middleStop = Math.max(leftStop + 2, baseTicks - 5);
        final int rightStop = baseTicks;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                hardCancelSpin(player);
                return;
            }

            if (!gambleService.isSpinning(uuid)) {
                hardCancelSpin(player);
                return;
            }

            if (!player.getOpenInventory().getView().getTitle().equals(spinningTitle)) {
                hardCancelSpin(player);
                return;
            }

            step[0]++;

            if (step[0] > rightStop) {
                cancelTask(spinTasks.remove(uuid));
                revealFinalOutcome(player, inv, mid, tier, outcome);
                return;
            }

            spinColumn(inv, top[0], mid[0], bot[0], outcome, step[0] <= leftStop, 0);
            spinColumn(inv, top[1], mid[1], bot[1], outcome, step[0] <= middleStop, 1);
            spinColumn(inv, top[2], mid[2], bot[2], outcome, step[0] <= rightStop, 2);

            float pitch = 1.20f;
            if (step[0] > leftStop) pitch = 1.0f;
            if (step[0] > middleStop) pitch = 0.85f;
            if (step[0] > rightStop - 2) pitch = 0.70f;
            if (outcome.isJackpot() && step[0] > rightStop - 3) pitch = 0.58f;

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, pitch);
        }, 0L, 2L);

        spinTasks.put(uuid, task);
    }

    private void spinColumn(Inventory inv, int topSlot, int midSlot, int botSlot, SpinOutcome outcome, boolean keepSpinning, int columnIndex) {
        if (keepSpinning) {
            inv.setItem(topSlot, buildSymbolItem(randomAnyMat(), "&7"));
            inv.setItem(botSlot, buildSymbolItem(randomAnyMat(), "&7"));

            if (outcome.isNearMiss() && columnIndex < 2) {
                inv.setItem(midSlot, buildSymbolItem(Material.EMERALD, "&a&lJackpot"));
            } else {
                inv.setItem(midSlot, buildSymbolItem(randomAnyMat(), "&7"));
            }
            return;
        }

        if (outcome.isWon()) {
            inv.setItem(topSlot, buildSymbolItem(randomAnyMat(), "&7"));
            inv.setItem(midSlot, buildWinningItem(outcome));
            inv.setItem(botSlot, buildSymbolItem(randomAnyMat(), "&7"));
            return;
        }

        if (outcome.isNearMiss()) {
            inv.setItem(topSlot, buildSymbolItem(randomAnyMat(), "&7"));
            if (columnIndex < 2) {
                inv.setItem(midSlot, buildSymbolItem(Material.EMERALD, "&a&lJackpot"));
            } else {
                inv.setItem(midSlot, buildSymbolItem(randomNonMatchingMat(Material.EMERALD), "&7Miss"));
            }
            inv.setItem(botSlot, buildSymbolItem(randomAnyMat(), "&7"));
            return;
        }

        Material left = randomAnyMat();
        Material middle = randomNonMatchingMat(left);
        Material right = randomNonMatchingMat(left, middle);

        Material selected = switch (columnIndex) {
            case 0 -> left;
            case 1 -> middle;
            default -> right;
        };

        inv.setItem(topSlot, buildSymbolItem(randomAnyMat(), "&7"));
        inv.setItem(midSlot, buildSymbolItem(selected, "&7"));
        inv.setItem(botSlot, buildSymbolItem(randomAnyMat(), "&7"));
    }

    private void revealFinalOutcome(Player player, Inventory inv, int[] mid, BetTier tier, SpinOutcome outcome) {
        UUID uuid = player.getUniqueId();

        if (!gambleService.isSpinning(uuid)) {
            hardCancelSpin(player);
            return;
        }

        if (outcome.isWon()) {
            ItemStack result = buildWinningItem(outcome);
            for (int slot : mid) {
                inv.setItem(slot, result);
            }

            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int n = 0;

                @Override
                public void run() {
                    if (!gambleService.isSpinning(uuid)) {
                        cancelTask(flashTasks.remove(uuid));
                        gambleService.stopCasinoSounds(player);
                        return;
                    }

                    n++;

                    for (int slot : mid) {
                        inv.setItem(slot, n % 2 == 0 ? result : buildSymbolItem(Material.GLOWSTONE_DUST, "&e&lWIN"));
                    }

                    if (n >= 6) {
                        for (int slot : mid) {
                            inv.setItem(slot, result);
                        }

                        cancelTask(flashTasks.remove(uuid));
                        gambleService.applyOutcome(player, tier, outcome);
                        gambleService.setSpinning(player, false);
                        gambleService.stopCasinoSounds(player);

                        BukkitTask reopen = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                gambleMenu.openSlotsMenu(player);
                            }
                        }, 20L);
                        reopenTasks.put(uuid, reopen);
                    }
                }
            }, 0L, 4L);

            flashTasks.put(uuid, task);
            return;
        }

        if (outcome.isNearMiss()) {
            inv.setItem(mid[0], buildSymbolItem(Material.EMERALD, "&a&lJackpot"));
            inv.setItem(mid[1], buildSymbolItem(Material.EMERALD, "&a&lJackpot"));
            inv.setItem(mid[2], buildSymbolItem(randomNonMatchingMat(Material.EMERALD), "&7Miss"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.65f);
        }

        BukkitTask finish = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!gambleService.isSpinning(uuid)) {
                hardCancelSpin(player);
                return;
            }

            gambleService.applyOutcome(player, tier, outcome);
            gambleService.setSpinning(player, false);
            gambleService.stopCasinoSounds(player);

            BukkitTask reopen = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    gambleMenu.openSlotsMenu(player);
                }
            }, 20L);
            reopenTasks.put(uuid, reopen);
        }, 12L);

        finishTasks.put(uuid, finish);
    }

    private void hardCancelSpin(Player player) {
        UUID uuid = player.getUniqueId();

        cancelTask(spinTasks.remove(uuid));
        cancelTask(flashTasks.remove(uuid));
        cancelTask(finishTasks.remove(uuid));
        cancelTask(reopenTasks.remove(uuid));

        gambleService.forceClear(uuid);
        gambleService.stopCasinoSounds(player);
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
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

    private ItemStack buildWinningItem(SpinOutcome outcome) {
        return switch (outcome.getSymbol()) {
            case IRON -> buildSymbolItem(Material.IRON_INGOT, "&f&lSmall Win");
            case GOLD -> buildSymbolItem(Material.GOLD_INGOT, "&6&lMedium Win");
            case DIAMOND -> buildSymbolItem(Material.DIAMOND, "&b&lBig Win");
            case EMERALD -> buildSymbolItem(Material.EMERALD, "&a&lJACKPOT");
        };
    }

    private ItemStack buildSymbolItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
