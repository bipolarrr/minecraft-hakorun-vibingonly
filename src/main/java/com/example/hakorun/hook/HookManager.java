package com.example.hakorun.hook;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.RunSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class HookManager {
    private final HakoRunPlugin plugin;

    public HookManager(HakoRunPlugin plugin) {
        this.plugin = plugin;
    }

    public void fireOnRunStart(RunSession session) {
        fire("onRunStart", session, Map.of(
                "%attempt%", String.valueOf(session.getAttemptIndex()),
                "%runId%", session.getRunId(),
                "%lives%", String.valueOf(session.getSharedLivesRemaining())
        ));
    }

    public void fireOnPlayerDeath(RunSession session, Player player, String reason) {
        fire("onPlayerDeathInRun", session, Map.of(
                "%player%", player.getName(),
                "%attempt%", String.valueOf(session.getAttemptIndex()),
                "%lives%", String.valueOf(session.getSharedLivesRemaining()),
                "%reason%", reason != null ? reason : "알 수 없음"
        ));
    }

    public void fireOnRunFail(RunSession session, String reason) {
        fire("onRunFail", session, Map.of(
                "%attempt%", String.valueOf(session != null ? session.getAttemptIndex() : 0),
                "%lives%", String.valueOf(session != null ? session.getSharedLivesRemaining() : 0),
                "%reason%", reason != null ? reason : "알 수 없음"
        ));
    }

    public void fireOnRunWin(RunSession session) {
        fire("onRunWin", session, Map.of(
                "%attempt%", String.valueOf(session.getAttemptIndex()),
                "%runId%", session.getRunId()
        ));
    }

    private void fire(String eventName, RunSession session, Map<String, String> placeholders) {
        List<String> commands = plugin.getConfigManager().getHookCommands(eventName);
        for (String cmd : commands) {
            String resolved = resolvePlaceholders(cmd, placeholders);
            plugin.getLogger().info("[HakoRun] Hook 실행 [" + eventName + "]: " + resolved);
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved));
        }
    }

    private String resolvePlaceholders(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
