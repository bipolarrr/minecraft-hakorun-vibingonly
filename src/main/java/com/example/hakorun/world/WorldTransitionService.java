package com.example.hakorun.world;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.RunSession;
import com.example.hakorun.model.RunState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class WorldTransitionService {
    private final HakoRunPlugin plugin;
    private final WorldManager worldManager;
    private final WorldOperationQueue operationQueue;
    private volatile boolean preparing = false;

    public WorldTransitionService(HakoRunPlugin plugin, WorldManager worldManager, WorldOperationQueue operationQueue) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.operationQueue = operationQueue;
    }

    public boolean isPreparing() { return preparing; }

    /**
     * Asynchronously prepares worlds for a new run session.
     * Players remain in lobby during preparation.
     * On completion, transitions to READY_TO_TRANSFER state and starts countdown.
     */
    public void prepareAndTransition(RunSession session, Runnable onReady, Runnable onFail) {
        if (preparing) {
            plugin.getLogger().warning("[HakoRun] 이미 준비 중입니다. 중복 요청 무시.");
            return;
        }
        preparing = true;

        plugin.getLogger().info("[HakoRun] 월드 준비 시작... runId=" + session.getRunId());

        // World creation MUST happen on main thread (Bukkit requirement)
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                boolean success = worldManager.createRunWorlds(session);
                if (success) {
                    plugin.getLogger().info("[HakoRun] 월드 준비 완료. runId=" + session.getRunId());
                    preparing = false;
                    onReady.run();
                } else {
                    plugin.getLogger().severe("[HakoRun] 월드 준비 실패. 플레이어는 로비에 유지됩니다.");
                    preparing = false;
                    notifyAdmins(Component.text("[HakoRun] 월드 준비에 실패했습니다. 로그를 확인하세요.").color(NamedTextColor.RED));
                    onFail.run();
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[HakoRun] 월드 준비 중 예외", e);
                preparing = false;
                notifyAdmins(Component.text("[HakoRun] 월드 준비 중 예외가 발생했습니다: " + e.getMessage()).color(NamedTextColor.RED));
                onFail.run();
            }
        });
    }

    /**
     * Teleports all online players to the run overworld spawn.
     */
    public void transferPlayersToRun(RunSession session) {
        Location spawn = worldManager.getRunOverworld(session).getSpawnLocation();
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                p.teleport(spawn);
                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                p.getInventory().clear();
                p.setHealth(20.0);
                p.setFoodLevel(20);
                p.setSaturation(5.0f);
                p.setLevel(0);
                p.setExp(0.0f);
            } catch (Exception e) {
                plugin.getLogger().warning("[HakoRun] 플레이어 이동 실패: " + p.getName());
            }
        }
    }

    /**
     * Teleports all players to lobby.
     */
    public void transferPlayersToLobby() {
        Location lobbySpawn = plugin.getConfigManager().getLobbySpawn();
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                p.teleport(lobbySpawn);
                p.setGameMode(org.bukkit.GameMode.ADVENTURE);
            } catch (Exception e) {
                plugin.getLogger().warning("[HakoRun] 로비 이동 실패: " + p.getName());
            }
        }
    }

    /**
     * Cleans up old run worlds asynchronously (file deletion can be slow).
     */
    public void cleanupOldRun(RunSession session) {
        if (session == null) return;
        boolean delete = plugin.getConfigManager().isDeleteOldRunWorlds();
        if (!delete) {
            plugin.getLogger().info("[HakoRun] 이전 런 월드 보관 (삭제 비활성화): " + session.getRunId());
            return;
        }
        // Unload must happen on main thread, file deletion can be offloaded
        Bukkit.getScheduler().runTask(plugin, () -> {
            worldManager.cleanupRunWorlds(session, true);
        });
    }

    private void notifyAdmins(Component message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("hakorun.admin")) {
                p.sendMessage(message);
            }
        }
        plugin.getLogger().warning(PlainTextComponentSerializer.plainText().serialize(message));
    }
}
