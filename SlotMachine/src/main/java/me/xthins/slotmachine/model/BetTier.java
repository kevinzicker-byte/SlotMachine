package me.xthins.slotmachine.model;

import org.bukkit.Material;

public record BetTier(
        String key,
        String displayName,
        Material material,
        double cost,
        double minMultiplier,
        double maxMultiplier,
        double winChance,
        int guiSlot
) {}
