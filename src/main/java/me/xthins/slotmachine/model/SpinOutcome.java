package me.xthins.slotmachine.model;

public class SpinOutcome {
    private final boolean won;
    private final boolean jackpot;
    private final boolean nearMiss;
    private final double payout;
    private final ReelSymbol symbol;

    public SpinOutcome(boolean won, boolean jackpot, boolean nearMiss, double payout, ReelSymbol symbol) {
        this.won = won;
        this.jackpot = jackpot;
        this.nearMiss = nearMiss;
        this.payout = payout;
        this.symbol = symbol;
    }

    public boolean isWon() { return won; }
    public boolean isJackpot() { return jackpot; }
    public boolean isNearMiss() { return nearMiss; }
    public double getPayout() { return payout; }
    public ReelSymbol getSymbol() { return symbol; }
}
