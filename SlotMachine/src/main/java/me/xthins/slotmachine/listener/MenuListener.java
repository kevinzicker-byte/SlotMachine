package me.xthins.slotmachine.listener;

import me.xthins.slotmachine.SlotMachinePlugin;
import me.xthins.slotmachine.model.BetTier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MenuListener implements Listener {
    private final SlotMachinePlugin plugin;

    public MenuListener(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        String title = event.getView().getTitle();
        String spinTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.spin-title", "&d&lSPINNING..."));
        String mainTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("spin.gui-title", "&d&lSLOT MACHINE"));
        if (!title.equals(mainTitle) && !title.equals(spinTitle)) return;
        event.setCancelled(true);
        if (!title.equals(mainTitle)) return;
        int slot = event.getRawSlot();
        for (BetTier tier : plugin.getBetTiers()) {
            if (tier.guiSlot() == slot) {
                plugin.getSlotMachineService().startSpin(player, tier);
                return;
            }
        }
        if (slot == 4) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.prefix() + " &7Current pot: &d" + me.xthins.slotmachine.util.MoneyUtil.formatMoneyCommas(plugin.currencySymbol(), plugin.getDataStore().getJackpotPot())));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player player)) return;
        String title = event.getView().getTitle();
        String spinTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.spin-title", "&d&lSPINNING..."));
        if (title.equals(spinTitle) && plugin.getSlotMachineService().isSpinning(player.getUniqueId())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && plugin.getSlotMachineService().isSpinning(player.getUniqueId())) {
                    plugin.getSlotMachineService().clearSpin(player.getUniqueId());
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.message("spin-reset")));
                }
            }, plugin.getConfig().getInt("spin.bedrock-timeout-seconds", 20) * 20L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getSlotMachineService().clearSpin(event.getPlayer().getUniqueId());
    }
}
