package me.xthins.slotmachine.model;

import org.bukkit.Material;

public record SpinResult(ResultType type, double payout, Material paylineMaterial, String broadcastAmount) {
    public enum ResultType {
        LOSS, SMALL, MEDIUM, BIG, JACKPOT, NEAR_MISS
    }
}
