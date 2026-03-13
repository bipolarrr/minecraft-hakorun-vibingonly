package com.example.hakorun.model;

import java.util.*;

public class RunSession {
    private final String runId;
    private final int attemptIndex;
    private final long overworldSeed;
    private final long netherSeed;
    private final long endSeed;
    private final long createdAt;
    private RunState state;
    private int sharedLivesRemaining;
    private final Map<UUID, Integer> playerLivesRemaining;
    private final List<DeathRecord> deathLog;
    private boolean dragonDead;
    private long runStartTime;

    public RunSession(String runId, int attemptIndex, long overworldSeed, long netherSeed, long endSeed, int initialSharedLives) {
        this.runId = runId;
        this.attemptIndex = attemptIndex;
        this.overworldSeed = overworldSeed;
        this.netherSeed = netherSeed;
        this.endSeed = endSeed;
        this.createdAt = System.currentTimeMillis();
        this.state = RunState.LOBBY;
        this.sharedLivesRemaining = initialSharedLives;
        this.playerLivesRemaining = new HashMap<>();
        this.deathLog = new ArrayList<>();
        this.dragonDead = false;
        this.runStartTime = 0L;
    }

    // For deserialization
    public RunSession(String runId, int attemptIndex, long overworldSeed, long netherSeed, long endSeed,
                      long createdAt, RunState state, int sharedLivesRemaining,
                      Map<UUID, Integer> playerLivesRemaining, List<DeathRecord> deathLog,
                      boolean dragonDead, long runStartTime) {
        this.runId = runId;
        this.attemptIndex = attemptIndex;
        this.overworldSeed = overworldSeed;
        this.netherSeed = netherSeed;
        this.endSeed = endSeed;
        this.createdAt = createdAt;
        this.state = state;
        this.sharedLivesRemaining = sharedLivesRemaining;
        this.playerLivesRemaining = playerLivesRemaining != null ? new HashMap<>(playerLivesRemaining) : new HashMap<>();
        this.deathLog = deathLog != null ? new ArrayList<>(deathLog) : new ArrayList<>();
        this.dragonDead = dragonDead;
        this.runStartTime = runStartTime;
    }

    public String getRunId() { return runId; }
    public int getAttemptIndex() { return attemptIndex; }
    public long getOverworldSeed() { return overworldSeed; }
    public long getNetherSeed() { return netherSeed; }
    public long getEndSeed() { return endSeed; }
    public long getCreatedAt() { return createdAt; }
    public RunState getState() { return state; }
    public void setState(RunState state) { this.state = state; }
    public int getSharedLivesRemaining() { return sharedLivesRemaining; }
    public void setSharedLivesRemaining(int lives) { this.sharedLivesRemaining = lives; }
    public Map<UUID, Integer> getPlayerLivesRemaining() { return playerLivesRemaining; }
    public List<DeathRecord> getDeathLog() { return deathLog; }
    public boolean isDragonDead() { return dragonDead; }
    public void setDragonDead(boolean dragonDead) { this.dragonDead = dragonDead; }
    public long getRunStartTime() { return runStartTime; }
    public void setRunStartTime(long runStartTime) { this.runStartTime = runStartTime; }

    public String getOverworldName() { return "hakorun_" + runId + "_world"; }
    public String getNetherName() { return "hakorun_" + runId + "_nether"; }
    public String getEndName() { return "hakorun_" + runId + "_end"; }

    public void addDeathRecord(DeathRecord record) { deathLog.add(record); }

    public void setPlayerLives(UUID playerUuid, int lives) {
        playerLivesRemaining.put(playerUuid, lives);
    }

    public int getPlayerLives(UUID playerUuid, int defaultLives) {
        return playerLivesRemaining.getOrDefault(playerUuid, defaultLives);
    }
}
