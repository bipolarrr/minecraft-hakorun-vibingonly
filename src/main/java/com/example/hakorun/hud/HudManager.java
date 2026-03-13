package com.example.hakorun.hud;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.RunSession;
import com.example.hakorun.model.RunState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HudManager {
    private final HakoRunPlugin plugin;
    private final Set<UUID> hudDisabled = new HashSet<>();
    private BossBar bossBar;
    private int taskId = -1;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    public HudManager(HakoRunPlugin plugin) {
        this.plugin = plugin;
        if (plugin.getConfigManager().isUseBossBarTimer()) {
            // org.bukkit.boss.BossBar 인터페이스가 Adventure title(Component) API를 노출하지 않으므로
            // createBossBar(String) / setTitle(String) 유지 — BossBar 한정 불가피한 예외
            bossBar = Bukkit.createBossBar("§6런 타이머", BarColor.YELLOW, BarStyle.SOLID);
        }
    }

    public void startUpdateTask() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L).getTaskId();
    }

    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (bossBar != null) {
            bossBar.removeAll();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public void toggleHud(Player player) {
        UUID uuid = player.getUniqueId();
        if (hudDisabled.contains(uuid)) {
            hudDisabled.remove(uuid);
            // [2] sendMessage(String) deprecated → sendMessage(Component)
            player.sendMessage(Component.text("[HakoRun] HUD가 활성화되었습니다.").color(NamedTextColor.GREEN));
        } else {
            hudDisabled.add(uuid);
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            player.sendMessage(Component.text("[HakoRun] HUD가 비활성화되었습니다.").color(NamedTextColor.YELLOW));
        }
    }

    public void updateAll() {
        RunSession session = plugin.getRunManager().getCurrentSession();
        RunState state = plugin.getRunManager().getCurrentState();
        int totalWins = plugin.getRunManager().getTotalWins();
        int totalResets = plugin.getRunManager().getTotalResets();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (hudDisabled.contains(p.getUniqueId())) continue;
            updateScoreboard(p, session, state, totalWins, totalResets);
            if (state == RunState.RUNNING && session != null) {
                updateTimerDisplay(p, session);
            }
        }

        if (bossBar != null && state == RunState.RUNNING && session != null) {
            long elapsed = (System.currentTimeMillis() - session.getRunStartTime()) / 1000;
            // BossBar Adventure API 미지원으로 setTitle(String) 유지
            bossBar.setTitle("§6런 타이머: §f" + formatTime(elapsed));
        }
    }

    private void updateScoreboard(Player player, RunSession session, RunState state, int wins, int resets) {
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard sb = sbm.getNewScoreboard();

        // [4] Component.text("§6§l◆...") → §코드 제거 후 Adventure API로 색상/장식 적용
        Objective obj = sb.registerNewObjective("hakorun", Criteria.DUMMY,
                Component.text("◆ HakoRun ◆").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 사이드바 항목 문자열의 §코드는 클라이언트가 렌더링하는 가짜 플레이어명이므로 그대로 유지
        int score = 15;
        setScore(obj, "§7", score--);
        setScore(obj, "§e상태: §f" + formatState(state), score--);

        if (session != null) {
            setScore(obj, "§e지구: §f" + session.getAttemptIndex() + "지구", score--);
            int lives = plugin.getLifeManager().getDisplayLives(session, player.getUniqueId());
            setScore(obj, "§c목숨: §f" + lives, score--);

            if (state == RunState.RUNNING) {
                long elapsed = (System.currentTimeMillis() - session.getRunStartTime()) / 1000;
                setScore(obj, "§b시간: §f" + formatTime(elapsed), score--);
            }
        } else {
            setScore(obj, "§e지구: §f-", score--);
            setScore(obj, "§c목숨: §f-", score--);
        }

        setScore(obj, "§6§m          ", score--);
        setScore(obj, "§a승리: §f" + wins, score--);
        setScore(obj, "§c실패: §f" + resets, score--);

        int onlineInRun = countPlayersInRun(session);
        setScore(obj, "§d생존: §f" + onlineInRun + "명", score--);
        setScore(obj, "§7 ", score--);

        player.setScoreboard(sb);
        playerScoreboards.put(player.getUniqueId(), sb);
    }

    private void setScore(Objective obj, String entry, int score) {
        Score s = obj.getScore(entry);
        s.setScore(score);
    }

    private void updateTimerDisplay(Player player, RunSession session) {
        if (!plugin.getConfigManager().isUseActionBarTimer()) return;
        long elapsed = (System.currentTimeMillis() - session.getRunStartTime()) / 1000;
        // [5] Component.text("§6⏱ ...") 에서 §6 제거 — Adventure는 §코드를 파싱하지 않음
        player.sendActionBar(Component.text("⏱ " + formatTime(elapsed)).color(NamedTextColor.GOLD));
    }

    public void showTitle(Player player, Component title, Component subtitle) {
        player.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(1000))
        ));
    }

    public void showTitleAll(Component title, Component subtitle) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            showTitle(p, title, subtitle);
        }
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private String formatState(RunState state) {
        return switch (state) {
            case LOBBY -> "§a로비";
            case PREPARING_NEXT_RUN -> "§e준비 중";
            case READY_TO_TRANSFER -> "§b이동 대기";
            case RUNNING -> "§c진행 중";
            case FAILED -> "§4실패";
            case WON -> "§6승리";
        };
    }

    private int countPlayersInRun(RunSession session) {
        if (session == null) return 0;
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            String worldName = p.getWorld().getName();
            if (worldName.equals(session.getOverworldName())
                    || worldName.equals(session.getNetherName())
                    || worldName.equals(session.getEndName())) {
                count++;
            }
        }
        return count;
    }

    public void addPlayerToBossBar(Player player) {
        if (bossBar != null) bossBar.addPlayer(player);
    }

    public void removePlayerFromBossBar(Player player) {
        if (bossBar != null) bossBar.removePlayer(player);
    }
}
