package me.xthins.slotmachine.model;

public enum BetTier {
    SMALL("small"),
    MEDIUM("medium"),
    BIG("big");

    private final String path;

    BetTier(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
