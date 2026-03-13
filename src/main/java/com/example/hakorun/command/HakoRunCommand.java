package com.example.hakorun.command;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class HakoRunCommand implements CommandExecutor, TabCompleter {
    private final HakoRunPlugin plugin;

    public HakoRunCommand(HakoRunPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        try {
            switch (sub) {
                case "start" -> {
                    requireAdmin(sender);
                    plugin.getRunManager().startRun(sender);
                }
                case "reset" -> {
                    requireAdmin(sender);
                    plugin.getRunManager().failRun("관리자 강제 리셋");
                }
                case "status" -> handleStatus(sender);
                case "lobby" -> handleLobby(sender);
                case "ui" -> handleUi(sender);
                case "sidebar" -> handleSidebar(sender, args);
                case "mode" -> handleMode(sender, args);
                case "lives" -> handleLives(sender, args);
                case "setlobby" -> handleSetLobby(sender);
                case "revealdeaths" -> {
                    requireAdmin(sender);
                    plugin.getRunManager().revealDeaths();
                }
                case "cleanup" -> {
                    requireAdmin(sender);
                    handleCleanup(sender);
                }
                default -> sendHelp(sender);
            }
        } catch (RuntimeException e) {
            // requireAdmin throws RuntimeException when no permission; already messaged the sender
        }
        return true;
    }

    private void requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("hakorun.admin")) {
            sender.sendMessage(Component.text("[HakoRun] 권한이 없습니다.").color(NamedTextColor.RED));
            throw new RuntimeException("No permission");
        }
    }

    private void handleStatus(CommandSender sender) {
        RunState state = plugin.getRunManager().getCurrentState();
        RunSession session = plugin.getRunManager().getCurrentSession();
        int attempt = plugin.getRunManager().getCurrentAttemptIndex();
        int wins = plugin.getRunManager().getTotalWins();
        int resets = plugin.getRunManager().getTotalResets();

        sender.sendMessage(Component.text("[HakoRun] ── 현재 상태 ──────────────").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("상태: ").color(NamedTextColor.YELLOW).append(Component.text(state.name()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("지구: ").color(NamedTextColor.YELLOW).append(Component.text(attempt + "지구").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("총 승리: ").color(NamedTextColor.GREEN).append(Component.text(String.valueOf(wins)).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("총 실패: ").color(NamedTextColor.RED).append(Component.text(String.valueOf(resets)).color(NamedTextColor.WHITE)));

        if (session != null) {
            sender.sendMessage(Component.text("runId: ").color(NamedTextColor.YELLOW).append(Component.text(session.getRunId()).color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("공유 목숨: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(session.getSharedLivesRemaining())).color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("모드: ").color(NamedTextColor.YELLOW).append(Component.text(plugin.getConfigManager().getRunMode().name()).color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("목숨 정책: ").color(NamedTextColor.YELLOW).append(Component.text(plugin.getConfigManager().getLifePolicy().getDisplayName()).color(NamedTextColor.WHITE)));
            if (state == RunState.RUNNING) {
                long elapsed = (System.currentTimeMillis() - session.getRunStartTime()) / 1000;
                sender.sendMessage(Component.text("생존 시간: ").color(NamedTextColor.YELLOW).append(Component.text(formatTime(elapsed)).color(NamedTextColor.WHITE)));
            }
        }

        if (plugin.getWorldTransitionService().isPreparing()) {
            sender.sendMessage(Component.text("월드 준비 중...").color(NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("────────────────────────────").color(NamedTextColor.GOLD));
    }

    private void handleLobby(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용 가능합니다.").color(NamedTextColor.RED));
            return;
        }
        player.teleport(plugin.getConfigManager().getLobbySpawn());
        player.sendMessage(Component.text("[HakoRun] 로비로 이동했습니다.").color(NamedTextColor.GREEN));
    }

    private void handleUi(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용 가능합니다.").color(NamedTextColor.RED));
            return;
        }
        plugin.getHudManager().toggleHud(player);
    }

    private void handleSidebar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용 가능합니다.").color(NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("사용법: /hakorun sidebar <policy|alive>").color(NamedTextColor.RED));
            return;
        }
        String section = args[1].toLowerCase();
        if (!section.equals("policy") && !section.equals("alive")) {
            sender.sendMessage(Component.text("올바른 섹션을 입력하세요: policy, alive").color(NamedTextColor.RED));
            return;
        }
        plugin.getHudManager().toggleSection(player, section);
    }

    private void handleMode(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hakorun.admin")) {
            sender.sendMessage(Component.text("권한이 없습니다.").color(NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("사용법: /hakorun mode <TEAM_SHARED_LIVES|INSTANT_WIPE>").color(NamedTextColor.RED));
            return;
        }
        try {
            RunMode mode = RunMode.valueOf(args[1].toUpperCase());
            plugin.getConfigManager().setRunMode(mode);
            sender.sendMessage(Component.text("[HakoRun] 모드가 ").color(NamedTextColor.GREEN)
                    .append(Component.text(mode.name()).color(NamedTextColor.WHITE))
                    .append(Component.text("으로 변경되었습니다.").color(NamedTextColor.GREEN)));
            plugin.getHudManager().updateAll();
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("올바른 모드를 입력하세요: TEAM_SHARED_LIVES, INSTANT_WIPE").color(NamedTextColor.RED));
        }
    }

    private void handleLives(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hakorun.admin")) {
            sender.sendMessage(Component.text("권한이 없습니다.").color(NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sendLivesHelp(sender);
            return;
        }

        RunSession session = plugin.getRunManager().getCurrentSession();
        String sub2 = args[1].toLowerCase();

        switch (sub2) {
            case "mode" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("사용법: /hakorun lives mode <인당 데스|공유목숨>").color(NamedTextColor.RED)); return; }
                try {
                    LifePolicy policy = LifePolicy.fromInput(args[2]);
                    plugin.getConfigManager().setLifePolicy(policy);
                    sender.sendMessage(Component.text("[HakoRun] 목숨 정책이 ").color(NamedTextColor.GREEN)
                            .append(Component.text(policy.getDisplayName()).color(NamedTextColor.WHITE))
                            .append(Component.text("으로 변경되었습니다.").color(NamedTextColor.GREEN)));
                    plugin.getHudManager().updateAll();
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("올바른 정책을 입력하세요: 인당 데스, 공유목숨").color(NamedTextColor.RED));
                }
            }
            case "shared" -> {
                if (args.length < 4) { sender.sendMessage(Component.text("사용법: /hakorun lives shared <set|add> <n>").color(NamedTextColor.RED)); return; }
                if (session == null) { sender.sendMessage(Component.text("현재 진행 중인 런이 없습니다.").color(NamedTextColor.RED)); return; }
                try {
                    int n = Integer.parseInt(args[3]);
                    if (args[2].equalsIgnoreCase("set")) {
                        plugin.getLifeManager().setSharedLives(session, n);
                        sender.sendMessage(Component.text("[HakoRun] 공유 목숨을 ").color(NamedTextColor.GREEN)
                                .append(Component.text(String.valueOf(n)).color(NamedTextColor.WHITE))
                                .append(Component.text("으로 설정했습니다.").color(NamedTextColor.GREEN)));
                    } else if (args[2].equalsIgnoreCase("add")) {
                        plugin.getLifeManager().addSharedLives(session, n);
                        sender.sendMessage(Component.text("[HakoRun] 공유 목숨에 ").color(NamedTextColor.GREEN)
                                .append(Component.text(String.valueOf(n)).color(NamedTextColor.WHITE))
                                .append(Component.text("을 추가했습니다. (현재: " + session.getSharedLivesRemaining() + ")").color(NamedTextColor.GREEN)));
                    } else {
                        sender.sendMessage(Component.text("set 또는 add를 입력하세요.").color(NamedTextColor.RED));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("숫자를 입력하세요.").color(NamedTextColor.RED));
                }
            }
            case "player" -> {
                if (args.length < 5) { sender.sendMessage(Component.text("사용법: /hakorun lives player <set|add> <player> <n>").color(NamedTextColor.RED)); return; }
                if (session == null) { sender.sendMessage(Component.text("현재 진행 중인 런이 없습니다.").color(NamedTextColor.RED)); return; }
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) { sender.sendMessage(Component.text("플레이어를 찾을 수 없습니다: " + args[3]).color(NamedTextColor.RED)); return; }
                try {
                    int n = Integer.parseInt(args[4]);
                    if (args[2].equalsIgnoreCase("set")) {
                        plugin.getLifeManager().setPlayerLives(session, target.getUniqueId(), n);
                        sender.sendMessage(Component.text("[HakoRun] " + target.getName() + "의 목숨을 ").color(NamedTextColor.GREEN)
                                .append(Component.text(String.valueOf(n)).color(NamedTextColor.WHITE))
                                .append(Component.text("으로 설정했습니다.").color(NamedTextColor.GREEN)));
                    } else if (args[2].equalsIgnoreCase("add")) {
                        plugin.getLifeManager().addPlayerLives(session, target.getUniqueId(), n);
                        sender.sendMessage(Component.text("[HakoRun] " + target.getName() + "의 목숨에 ").color(NamedTextColor.GREEN)
                                .append(Component.text(String.valueOf(n)).color(NamedTextColor.WHITE))
                                .append(Component.text("을 추가했습니다.").color(NamedTextColor.GREEN)));
                    } else {
                        sender.sendMessage(Component.text("set 또는 add를 입력하세요.").color(NamedTextColor.RED));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("숫자를 입력하세요.").color(NamedTextColor.RED));
                }
            }
            case "sync-default" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("사용법: /hakorun lives sync-default <n>").color(NamedTextColor.RED)); return; }
                try {
                    int n = Integer.parseInt(args[2]);
                    plugin.getLifeManager().syncDefaultLives(session, n);
                    sender.sendMessage(Component.text("[HakoRun] 기본 목숨이 ").color(NamedTextColor.GREEN)
                            .append(Component.text(String.valueOf(n)).color(NamedTextColor.WHITE))
                            .append(Component.text("으로 설정되었습니다.").color(NamedTextColor.GREEN)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("숫자를 입력하세요.").color(NamedTextColor.RED));
                }
            }
            default -> {
                // Legacy: /hakorun lives <n>
                try {
                    int n = Integer.parseInt(args[1]);
                    if (session != null) {
                        plugin.getLifeManager().setSharedLives(session, n);
                    }
                    plugin.getConfig().set("sharedLives", n);
                    plugin.saveConfig();
                    sender.sendMessage(Component.text("[HakoRun] 기본 목숨이 ").color(NamedTextColor.GREEN)
                            .append(Component.text(String.valueOf(n)).color(NamedTextColor.WHITE))
                            .append(Component.text("으로 설정되었습니다.").color(NamedTextColor.GREEN)));
                } catch (NumberFormatException e) {
                    sendLivesHelp(sender);
                }
            }
        }
    }

    private void handleSetLobby(CommandSender sender) {
        if (!sender.hasPermission("hakorun.admin")) { sender.sendMessage(Component.text("권한이 없습니다.").color(NamedTextColor.RED)); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(Component.text("플레이어만 사용 가능합니다.").color(NamedTextColor.RED)); return; }
        plugin.getConfigManager().setLobbySpawn(player.getLocation());
        sender.sendMessage(Component.text("[HakoRun] 현재 위치를 로비 스폰으로 설정했습니다.").color(NamedTextColor.GREEN));
    }

    private void handleCleanup(CommandSender sender) {
        RunSession session = plugin.getRunManager().getCurrentSession();
        if (session == null) {
            sender.sendMessage(Component.text("현재 세션이 없습니다.").color(NamedTextColor.RED));
            return;
        }
        plugin.getWorldTransitionService().cleanupOldRun(session);
        sender.sendMessage(Component.text("[HakoRun] 월드 정리를 시작했습니다.").color(NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("[HakoRun] 명령어 목록:").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/hakorun start ").color(NamedTextColor.YELLOW).append(Component.text("- 새 런 시작").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun reset ").color(NamedTextColor.YELLOW).append(Component.text("- 현재 런 강제 실패").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun status ").color(NamedTextColor.YELLOW).append(Component.text("- 현재 상태 확인").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun lobby ").color(NamedTextColor.YELLOW).append(Component.text("- 로비로 이동").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun ui ").color(NamedTextColor.YELLOW).append(Component.text("- HUD on/off").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun sidebar <policy|alive> ").color(NamedTextColor.YELLOW).append(Component.text("- 사이드바 섹션 개인 토글").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun mode <MODE> ").color(NamedTextColor.YELLOW).append(Component.text("- 런 모드 변경").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun lives ... ").color(NamedTextColor.YELLOW).append(Component.text("- 목숨 관리").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun setlobby ").color(NamedTextColor.YELLOW).append(Component.text("- 로비 스폰 설정").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun revealdeaths ").color(NamedTextColor.YELLOW).append(Component.text("- 사망 기록 공개").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hakorun cleanup ").color(NamedTextColor.YELLOW).append(Component.text("- 월드 정리").color(NamedTextColor.GRAY)));
    }

    private void sendLivesHelp(CommandSender sender) {
        sender.sendMessage(Component.text("[HakoRun] 목숨 명령어:").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/hakorun lives mode <인당 데스|공유목숨>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hakorun lives shared set <n>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hakorun lives shared add <delta>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hakorun lives player set <player> <n>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hakorun lives player add <player> <delta>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hakorun lives sync-default <n>").color(NamedTextColor.YELLOW));
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "reset", "status", "lobby", "ui", "sidebar", "mode", "lives", "setlobby", "revealdeaths", "cleanup");
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "mode" -> { return Arrays.asList("TEAM_SHARED_LIVES", "INSTANT_WIPE"); }
                case "sidebar" -> { return Arrays.asList("policy", "alive"); }
                case "lives" -> { return Arrays.asList("mode", "shared", "player", "sync-default"); }
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("lives")) {
            switch (args[1].toLowerCase()) {
                case "mode" -> { return Arrays.asList("인당 데스", "공유목숨"); }
                case "shared" -> { return Arrays.asList("set", "add"); }
                case "player" -> { return Arrays.asList("set", "add"); }
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("lives") && args[1].equalsIgnoreCase("player")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return Collections.emptyList();
    }
}
