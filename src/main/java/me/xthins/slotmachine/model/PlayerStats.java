package me.xthins.slotmachine.model;

public class PlayerStats {
    private double wagered;
    private double won;
    private int wins;
    private int losses;
    private double biggestWin;
    private int jackpots;
    private String playerName;
    private String lastKnownFaction;

    public double getWagered() { return wagered; }
    public void setWagered(double wagered) { this.wagered = wagered; }
    public double getWon() { return won; }
    public void setWon(double won) { this.won = won; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public double getBiggestWin() { return biggestWin; }
    public void setBiggestWin(double biggestWin) { this.biggestWin = biggestWin; }
    public int getJackpots() { return jackpots; }
    public void setJackpots(int jackpots) { this.jackpots = jackpots; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getLastKnownFaction() { return lastKnownFaction; }
    public void setLastKnownFaction(String lastKnownFaction) { this.lastKnownFaction = lastKnownFaction; }
    public double getProfit() { return won - wagered; }
}
