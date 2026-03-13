package com.example.hakorun.life;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.LifePolicy;
import com.example.hakorun.model.RunMode;
import com.example.hakorun.model.RunSession;
import com.example.hakorun.model.RunState;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LifeManager {
    private final HakoRunPlugin plugin;

    public LifeManager(HakoRunPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when a player dies in a run. Returns true if the run should fail.
     */
    public boolean handleDeath(Player player, RunSession session) {
        RunMode mode = plugin.getConfigManager().getRunMode();
        LifePolicy policy = plugin.getConfigManager().getLifePolicy();

        if (mode == RunMode.INSTANT_WIPE) {
            return true; // Immediately fail on any death
        }

        // TEAM_SHARED_LIVES mode
        if (policy == LifePolicy.SHARED_POOL) {
            int remaining = session.getSharedLivesRemaining() - 1;
            session.setSharedLivesRemaining(Math.max(0, remaining));
            plugin.getRunManager().save();
            return remaining <= 0;
        } else {
            // PER_PLAYER
            int defaultLives = plugin.getConfigManager().getSharedLives();
            int playerLives = session.getPlayerLives(player.getUniqueId(), defaultLives) - 1;
            session.setPlayerLives(player.getUniqueId(), Math.max(0, playerLives));
            plugin.getRunManager().save();
            return playerLives <= 0; // Only this player is eliminated (run may continue)
        }
    }

    /**
     * Sets shared lives pool. Re-evaluates fail condition.
     */
    public void setSharedLives(RunSession session, int lives) {
        session.setSharedLivesRemaining(lives);
        plugin.getRunManager().save();
        plugin.getHudManager().updateAll();
        evaluateFailCondition(session);
    }

    /**
     * Adds delta to shared lives. Can be negative.
     */
    public void addSharedLives(RunSession session, int delta) {
        int newLives = Math.max(0, session.getSharedLivesRemaining() + delta);
        session.setSharedLivesRemaining(newLives);
        plugin.getRunManager().save();
        plugin.getHudManager().updateAll();
        evaluateFailCondition(session);
    }

    /**
     * Sets individual player lives.
     */
    public void setPlayerLives(RunSession session, UUID playerUuid, int lives) {
        session.setPlayerLives(playerUuid, Math.max(0, lives));
        plugin.getRunManager().save();
        plugin.getHudManager().updateAll();
        evaluateFailCondition(session);
    }

    /**
     * Adds delta to individual player lives.
     */
    public void addPlayerLives(RunSession session, UUID playerUuid, int delta) {
        int defaultLives = plugin.getConfigManager().getSharedLives();
        int current = session.getPlayerLives(playerUuid, defaultLives);
        int newLives = Math.max(0, current + delta);
        session.setPlayerLives(playerUuid, newLives);
        plugin.getRunManager().save();
        plugin.getHudManager().updateAll();
        evaluateFailCondition(session);
    }

    /**
     * Syncs default lives value in config and initializes current session.
     */
    public void syncDefaultLives(RunSession session, int n) {
        plugin.getConfigManager().getDataConfig(); // just ensure loaded
        // Update the config value
        plugin.getConfig().set("sharedLives", n);
        plugin.saveConfig();
        if (session != null && session.getState() == RunState.RUNNING) {
            setSharedLives(session, n);
        }
    }

    private void evaluateFailCondition(RunSession session) {
        if (session == null || session.getState() != RunState.RUNNING) return;
        LifePolicy policy = plugin.getConfigManager().getLifePolicy();
        if (policy == LifePolicy.SHARED_POOL && session.getSharedLivesRemaining() <= 0) {
            // Trigger fail via RunManager on main thread
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> plugin.getRunManager().failRun("목숨 소진"));
        }
    }

    public int getDisplayLives(RunSession session, UUID playerUuid) {
        if (session == null) return 0;
        LifePolicy policy = plugin.getConfigManager().getLifePolicy();
        if (policy == LifePolicy.SHARED_POOL) {
            return session.getSharedLivesRemaining();
        } else {
            int defaultLives = plugin.getConfigManager().getSharedLives();
            return session.getPlayerLives(playerUuid, defaultLives);
        }
    }
}
