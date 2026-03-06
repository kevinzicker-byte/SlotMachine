package me.xthins.slotmachine.model;

import java.util.UUID;

public class PlayerStats {
    private final UUID uuid;
    private String lastName;
    private String factionTag;
    private long bets;
    private double wagered;
    private double won;
    private long wins;
    private long losses;
    private double biggestWin;
    private long jackpots;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFactionTag() { return factionTag; }
    public void setFactionTag(String factionTag) { this.factionTag = factionTag; }
    public long getBets() { return bets; }
    public void addBet() { this.bets++; }
    public double getWagered() { return wagered; }
    public void addWagered(double amount) { this.wagered += amount; }
    public double getWon() { return won; }
    public void addWon(double amount) { this.won += amount; }
    public long getWins() { return wins; }
    public void addWin() { this.wins++; }
    public long getLosses() { return losses; }
    public void addLoss() { this.losses++; }
    public double getBiggestWin() { return biggestWin; }
    public void setBiggestWin(double biggestWin) { this.biggestWin = Math.max(this.biggestWin, biggestWin); }
    public long getJackpots() { return jackpots; }
    public void addJackpot() { this.jackpots++; }
    public double getProfit() { return won - wagered; }
}
