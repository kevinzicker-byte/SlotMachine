package me.xthins.slotmachine.model;

import org.bukkit.Material;

public enum ReelSymbol {
    IRON(Material.IRON_INGOT),
    GOLD(Material.GOLD_INGOT),
    DIAMOND(Material.DIAMOND),
    EMERALD(Material.EMERALD);

    private final Material material;

    ReelSymbol(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }
}
