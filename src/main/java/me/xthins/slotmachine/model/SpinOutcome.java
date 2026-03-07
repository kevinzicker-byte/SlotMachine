package me.xthins.slotmachine.model;

public class SpinOutcome {
    private final boolean won;
    private final boolean jackpot;
    private final boolean nearMiss;
    private final double payout;
    private final ReelSymbol symbol;
    private final boolean freeSpinsTriggered;

    public SpinOutcome(boolean won, boolean jackpot, boolean nearMiss, double payout, ReelSymbol symbol) {
        this(won, jackpot, nearMiss, payout, symbol, false);
    }

    public SpinOutcome(boolean won, boolean jackpot, boolean nearMiss, double payout, ReelSymbol symbol, boolean freeSpinsTriggered) {
        this.won = won;
        this.jackpot = jackpot;
        this.nearMiss = nearMiss;
        this.payout = payout;
        this.symbol = symbol;
        this.freeSpinsTriggered = freeSpinsTriggered;
    }

    public boolean isWon() {
        return won;
    }

    public boolean isJackpot() {
        return jackpot;
    }

    public boolean isNearMiss() {
        return nearMiss;
    }

    public double getPayout() {
        return payout;
    }

    public ReelSymbol getSymbol() {
        return symbol;
    }

    public boolean isFreeSpinsTriggered() {
        return freeSpinsTriggered;
    }
}
