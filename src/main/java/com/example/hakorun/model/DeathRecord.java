package com.example.hakorun.model;

import java.time.Instant;
import java.util.UUID;

public class DeathRecord {
    private final UUID playerUuid;
    private final String playerName;
    private final String deathMessage;
    private final long timestamp;

    public DeathRecord(UUID playerUuid, String playerName, String deathMessage) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.deathMessage = deathMessage;
        this.timestamp = Instant.now().getEpochSecond();
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getDeathMessage() { return deathMessage; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + playerName + "] " + deathMessage;
    }
}
