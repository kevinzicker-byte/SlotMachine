package me.xthins.slotmachine.model;

import java.util.UUID;

public class GambleProfile {
    private final UUID uuid;
    private String lastKnownName;
    private double wagered;
    private double won;
    private int wins;
    private int losses;
    private int jackpots;
    private double biggestWin;

    public GambleProfile(UUID uuid, String lastKnownName) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
    }

    public UUID getUuid() { return uuid; }
    public String getLastKnownName() { return lastKnownName; }
    public void setLastKnownName(String lastKnownName) { this.lastKnownName = lastKnownName; }
    public double getWagered() { return wagered; }
    public void addWagered(double wagered) { this.wagered += wagered; }
    public double getWon() { return won; }
    public void addWon(double won) { this.won += won; }
    public int getWins() { return wins; }
    public void addWin() { this.wins++; }
    public int getLosses() { return losses; }
    public void addLoss() { this.losses++; }
    public int getJackpots() { return jackpots; }
    public void addJackpot() { this.jackpots++; }
    public double getBiggestWin() { return biggestWin; }
    public void trackBiggestWin(double amount) { this.biggestWin = Math.max(this.biggestWin, amount); }
    public double getProfit() { return won - wagered; }

    public void setWagered(double wagered) { this.wagered = wagered; }
    public void setWon(double won) { this.won = won; }
    public void setWins(int wins) { this.wins = wins; }
    public void setLosses(int losses) { this.losses = losses; }
    public void setJackpots(int jackpots) { this.jackpots = jackpots; }
    public void setBiggestWin(double biggestWin) { this.biggestWin = biggestWin; }
}
