package com.example.hakorun.run;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

public class RunManager {
    private final HakoRunPlugin plugin;

    private RunState currentState = RunState.LOBBY;
    private RunSession currentSession = null;
    private int currentAttemptIndex = 1;
    private int totalWins = 0;
    private int totalResets = 0;
    private int countdownTask = -1;

    public RunManager(HakoRunPlugin plugin) {
        this.plugin = plugin;
    }

    // --- Persistence ---

    public void load() {
        YamlConfiguration data = plugin.getConfigManager().getDataConfig();
        currentAttemptIndex = data.getInt("currentAttemptIndex", 1);
        totalWins = data.getInt("totalWins", 0);
        totalResets = data.getInt("totalResets", 0);
        String stateStr = data.getString("currentState", "LOBBY");
        try { currentState = RunState.valueOf(stateStr); } catch (Exception e) { currentState = RunState.LOBBY; }

        // Recover session if it was RUNNING (mark as LOBBY since server restarted)
        if (data.contains("session")) {
            currentSession = deserializeSession(data.getConfigurationSection("session"));
            if (currentSession != null) {
                // On restart, if state was RUNNING/PREPARING, set back to LOBBY since worlds may be gone
                if (currentSession.getState() == RunState.RUNNING
                        || currentSession.getState() == RunState.PREPARING_NEXT_RUN
                        || currentSession.getState() == RunState.READY_TO_TRANSFER) {
                    plugin.getLogger().info("[HakoRun] 서버 재시작 후 이전 런 세션 복구 (상태: LOBBY). runId=" + currentSession.getRunId());
                    currentState = RunState.LOBBY;
                    currentSession.setState(RunState.LOBBY);
                } else {
                    currentState = currentSession.getState();
                }
            }
        }

        plugin.getLogger().info("[HakoRun] 데이터 로드 완료 | attempt=" + currentAttemptIndex
                + " | wins=" + totalWins + " | resets=" + totalResets + " | state=" + currentState);
    }

    public void save() {
        YamlConfiguration data = plugin.getConfigManager().getDataConfig();
        data.set("currentAttemptIndex", currentAttemptIndex);
        data.set("totalWins", totalWins);
        data.set("totalResets", totalResets);
        data.set("currentState", currentState.name());
        if (currentSession != null) {
            serializeSession(data, "session", currentSession);
        }
        plugin.getConfigManager().saveData();
    }

    private void serializeSession(YamlConfiguration data, String path, RunSession s) {
        data.set(path + ".runId", s.getRunId());
        data.set(path + ".attemptIndex", s.getAttemptIndex());
        data.set(path + ".overworldSeed", s.getOverworldSeed());
        data.set(path + ".netherSeed", s.getNetherSeed());
        data.set(path + ".endSeed", s.getEndSeed());
        data.set(path + ".createdAt", s.getCreatedAt());
        data.set(path + ".state", s.getState().name());
        data.set(path + ".sharedLivesRemaining", s.getSharedLivesRemaining());
        data.set(path + ".dragonDead", s.isDragonDead());
        data.set(path + ".runStartTime", s.getRunStartTime());

        // Player lives
        Map<UUID, Integer> plr = s.getPlayerLivesRemaining();
        List<String> playerLivesList = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : plr.entrySet()) {
            playerLivesList.add(entry.getKey().toString() + ":" + entry.getValue());
        }
        data.set(path + ".playerLives", playerLivesList);

        // Death log
        List<Map<String, Object>> deathLogData = new ArrayList<>();
        for (DeathRecord dr : s.getDeathLog()) {
            Map<String, Object> drMap = new HashMap<>();
            drMap.put("uuid", dr.getPlayerUuid().toString());
            drMap.put("name", dr.getPlayerName());
            drMap.put("message", dr.getDeathMessage());
            drMap.put("timestamp", dr.getTimestamp());
            deathLogData.add(drMap);
        }
        data.set(path + ".deathLog", deathLogData);
    }

    private RunSession deserializeSession(ConfigurationSection sec) {
        if (sec == null) return null;
        try {
            String runId = sec.getString("runId", UUID.randomUUID().toString().substring(0, 8));
            int attemptIndex = sec.getInt("attemptIndex", 1);
            long owSeed = sec.getLong("overworldSeed", 0L);
            long neSeed = sec.getLong("netherSeed", 0L);
            long enSeed = sec.getLong("endSeed", 0L);
            long createdAt = sec.getLong("createdAt", System.currentTimeMillis());
            RunState st = RunState.LOBBY;
            try { st = RunState.valueOf(sec.getString("state", "LOBBY")); } catch (Exception ignored) {}
            int sharedLives = sec.getInt("sharedLivesRemaining", plugin.getConfigManager().getSharedLives());
            boolean dragonDead = sec.getBoolean("dragonDead", false);
            long runStartTime = sec.getLong("runStartTime", 0L);

            Map<UUID, Integer> playerLives = new HashMap<>();
            List<String> plList = sec.getStringList("playerLives");
            for (String entry : plList) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    try { playerLives.put(UUID.fromString(parts[0]), Integer.parseInt(parts[1])); } catch (Exception ignored) {}
                }
            }

            List<DeathRecord> deathLog = new ArrayList<>();
            List<Map<?, ?>> dlList = sec.getMapList("deathLog");
            for (Map<?, ?> m : dlList) {
                try {
                    UUID uuid = UUID.fromString((String) m.get("uuid"));
                    String name = (String) m.get("name");
                    String message = (String) m.get("message");
                    deathLog.add(new DeathRecord(uuid, name, message));
                } catch (Exception ignored) {}
            }

            RunSession session = new RunSession(runId, attemptIndex, owSeed, neSeed, enSeed,
                    createdAt, st, sharedLives, playerLives, deathLog, dragonDead, runStartTime);
            return session;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[HakoRun] 세션 역직렬화 실패", e);
            return null;
        }
    }

    // --- State Machine ---

    public void startRun(org.bukkit.command.CommandSender sender) {
        if (currentState == RunState.RUNNING) {
            sender.sendMessage(Component.text("[HakoRun] 이미 런이 진행 중입니다.").color(NamedTextColor.RED));
            return;
        }
        if (currentState == RunState.PREPARING_NEXT_RUN) {
            sender.sendMessage(Component.text("[HakoRun] 이미 런을 준비 중입니다. 잠시 기다려 주세요.").color(NamedTextColor.RED));
            return;
        }

        currentState = RunState.PREPARING_NEXT_RUN;

        // Generate new run session with unique runId and seeds
        String runId = generateRunId();
        long owSeed = generateSeed();
        long neSeed = generateSeed();
        long enSeed = generateSeed();
        int initialLives = plugin.getConfigManager().getSharedLives();

        RunSession newSession = new RunSession(runId, currentAttemptIndex, owSeed, neSeed, enSeed, initialLives);
        newSession.setState(RunState.PREPARING_NEXT_RUN);

        // Initialize per-player lives
        for (Player p : Bukkit.getOnlinePlayers()) {
            newSession.setPlayerLives(p.getUniqueId(), initialLives);
        }

        currentSession = newSession;
        save();

        plugin.getLogger().info("[HakoRun] 새 런 준비 중 | runId=" + runId + " | attempt=" + currentAttemptIndex
                + " | owSeed=" + owSeed + " | neSeed=" + neSeed + " | enSeed=" + enSeed);

        Bukkit.broadcast(Component.text("[HakoRun] ").color(NamedTextColor.GOLD)
                .append(Component.text(currentAttemptIndex + "지구 ").color(NamedTextColor.YELLOW))
                .append(Component.text("준비 중...").color(NamedTextColor.GRAY)));

        plugin.getWorldTransitionService().prepareAndTransition(newSession,
                this::onWorldsReady,
                () -> {
                    currentState = RunState.LOBBY;
                    currentSession = null;
                    save();
                    Bukkit.broadcast(Component.text("[HakoRun] 월드 준비에 실패했습니다. 로비로 유지합니다.").color(NamedTextColor.RED));
                }
        );
    }

    private void onWorldsReady() {
        currentState = RunState.READY_TO_TRANSFER;
        currentSession.setState(RunState.READY_TO_TRANSFER);
        save();

        int countdown = plugin.getConfigManager().getRunStartCountdownSeconds();
        Bukkit.broadcast(Component.text("[HakoRun] 월드 준비 완료! ").color(NamedTextColor.GREEN)
                .append(Component.text(countdown + "초 후 " + currentAttemptIndex + "지구로 이동합니다!").color(NamedTextColor.YELLOW)));

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int remaining = countdown;

            @Override
            public void run() {
                if (remaining <= 0) {
                    Bukkit.getScheduler().cancelTask(countdownTask);
                    countdownTask = -1;
                    beginRun();
                    return;
                }
                if (remaining <= 3 || remaining == countdown) {
                    Bukkit.broadcast(Component.text("[HakoRun] ").color(NamedTextColor.YELLOW)
                            .append(Component.text(String.valueOf(remaining)).color(NamedTextColor.WHITE))
                            .append(Component.text("초 후 출발!").color(NamedTextColor.YELLOW)));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }
                remaining--;
            }
        }, 0L, 20L).getTaskId();
    }

    private void beginRun() {
        if (currentSession == null || currentState != RunState.READY_TO_TRANSFER) return;

        currentState = RunState.RUNNING;
        currentSession.setState(RunState.RUNNING);
        currentSession.setRunStartTime(System.currentTimeMillis());
        save();

        plugin.getWorldTransitionService().transferPlayersToRun(currentSession);
        plugin.getHookManager().fireOnRunStart(currentSession);

        Component separator = Component.text("[HakoRun] ══════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        Bukkit.broadcast(separator);
        Bukkit.broadcast(Component.text("  " + currentAttemptIndex + "지구 시작!").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("  runId: ").color(NamedTextColor.GRAY)
                .append(Component.text(currentSession.getRunId()).color(NamedTextColor.WHITE)));
        Bukkit.broadcast(Component.text("  목숨: ").color(NamedTextColor.RED)
                .append(Component.text(String.valueOf(currentSession.getSharedLivesRemaining())).color(NamedTextColor.WHITE)));
        Bukkit.broadcast(separator);

        plugin.getHudManager().showTitleAll(
                Component.text(currentAttemptIndex + "지구").color(NamedTextColor.YELLOW),
                Component.text("목숨: " + currentSession.getSharedLivesRemaining()).color(NamedTextColor.RED)
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.5f, 1.0f);
            plugin.getHudManager().addPlayerToBossBar(p);
        }

        plugin.getHudManager().updateAll();
    }

    public void failRun(String reason) {
        if (currentState != RunState.RUNNING && currentState != RunState.READY_TO_TRANSFER
                && currentState != RunState.PREPARING_NEXT_RUN) {
            return;
        }

        plugin.getLogger().info("[HakoRun] 런 실패: " + reason + " | attempt=" + currentAttemptIndex);

        currentState = RunState.FAILED;
        if (currentSession != null) currentSession.setState(RunState.FAILED);
        totalResets++;

        // Reveal deaths if REVEAL_ON_WIPE
        if (currentSession != null && plugin.getConfigManager().getDeathMessageMode() == DeathMessageMode.REVEAL_ON_WIPE) {
            revealDeaths();
        }

        plugin.getHookManager().fireOnRunFail(currentSession, reason);

        Component separator = Component.text("[HakoRun] ══════════════════════").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD);
        Bukkit.broadcast(separator);
        Bukkit.broadcast(Component.text("  " + currentAttemptIndex + "지구 실패!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("  사유: ").color(NamedTextColor.GRAY)
                .append(Component.text(reason).color(NamedTextColor.WHITE)));
        Bukkit.broadcast(Component.text("  총 실패: ").color(NamedTextColor.GRAY)
                .append(Component.text(totalResets + "회").color(NamedTextColor.WHITE)));
        Bukkit.broadcast(separator);

        plugin.getHudManager().showTitleAll(
                Component.text("런 실패!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text(reason).color(NamedTextColor.GRAY)
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 0.5f, 1.0f);
            plugin.getHudManager().removePlayerFromBossBar(p);
        }

        RunSession oldSession = currentSession;
        currentAttemptIndex++;
        currentState = RunState.LOBBY;
        currentSession = null;
        save();

        // Send players to lobby after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getWorldTransitionService().transferPlayersToLobby();
            // Cleanup old worlds
            plugin.getWorldTransitionService().cleanupOldRun(oldSession);
        }, 60L);

        plugin.getHudManager().updateAll();
    }

    public void winRun() {
        if (currentState != RunState.RUNNING) return;

        plugin.getLogger().info("[HakoRun] 런 승리! attempt=" + currentAttemptIndex);

        currentState = RunState.WON;
        if (currentSession != null) currentSession.setState(RunState.WON);
        totalWins++;

        plugin.getHookManager().fireOnRunWin(currentSession);

        Component separator = Component.text("[HakoRun] ══════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        Bukkit.broadcast(separator);
        Bukkit.broadcast(Component.text("  " + currentAttemptIndex + "지구 클리어!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("  엔더드래곤 처치 성공!").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("  총 승리: ").color(NamedTextColor.GRAY)
                .append(Component.text(totalWins + "회").color(NamedTextColor.WHITE)));
        Bukkit.broadcast(separator);

        plugin.getHudManager().showTitleAll(
                Component.text("런 성공!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("엔더드래곤 처치!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
            plugin.getHudManager().removePlayerFromBossBar(p);
        }

        RunSession oldSession = currentSession;
        currentAttemptIndex++;
        currentState = RunState.LOBBY;
        currentSession = null;
        save();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getWorldTransitionService().transferPlayersToLobby();
            plugin.getWorldTransitionService().cleanupOldRun(oldSession);
        }, 100L);

        plugin.getHudManager().updateAll();
    }

    public void revealDeaths() {
        if (currentSession == null || currentSession.getDeathLog().isEmpty()) {
            Bukkit.broadcast(Component.text("[HakoRun] 이번 런에서 기록된 사망 없음.").color(NamedTextColor.GRAY));
            return;
        }
        Bukkit.broadcast(Component.text("[HakoRun] ── 사망 기록 공개 ──").color(NamedTextColor.RED));
        for (DeathRecord dr : currentSession.getDeathLog()) {
            Bukkit.broadcast(Component.text("  " + dr.toString()).color(NamedTextColor.GRAY));
        }
        Bukkit.broadcast(Component.text("[HakoRun] ─────────────────────").color(NamedTextColor.RED));
    }

    // --- Helpers ---

    private String generateRunId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private long generateSeed() {
        if (!plugin.getConfigManager().isRandomSeed()) {
            return plugin.getConfigManager().getFixedSeed();
        }
        return new Random().nextLong();
    }

    // --- Getters ---

    public RunState getCurrentState() { return currentState; }
    public RunSession getCurrentSession() { return currentSession; }
    public int getCurrentAttemptIndex() { return currentAttemptIndex; }
    public int getTotalWins() { return totalWins; }
    public int getTotalResets() { return totalResets; }
}
