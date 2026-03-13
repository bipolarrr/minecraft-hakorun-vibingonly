package com.example.hakorun.listener;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeathListener implements Listener {
    private final HakoRunPlugin plugin;
    private final Set<UUID> spectating = new HashSet<>();

    public DeathListener(HakoRunPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        RunSession session = plugin.getRunManager().getCurrentSession();
        RunState state = plugin.getRunManager().getCurrentState();

        if (state != RunState.RUNNING || session == null) return;

        // Only handle deaths in run worlds
        String worldName = player.getWorld().getName();
        if (!plugin.getWorldManager().isRunWorld(worldName, session)) return;

        // [HIGH] Component.toString()은 Adventure 내부 디버그 표현식 반환 —
        //        PlainTextComponentSerializer로 직렬화해야 사람이 읽을 수 있는 문자열 획득
        Component deathComp = event.deathMessage();
        String deathMsg = deathComp != null
                ? PlainTextComponentSerializer.plainText().serialize(deathComp)
                : player.getName() + "이(가) 사망했습니다.";
        DeathMessageMode dmMode = plugin.getConfigManager().getDeathMessageMode();

        // Record death
        DeathRecord record = new DeathRecord(player.getUniqueId(), player.getName(), deathMsg);
        session.addDeathRecord(record);

        // Handle death message visibility
        if (dmMode == DeathMessageMode.HIDDEN || dmMode == DeathMessageMode.REVEAL_ON_WIPE) {
            event.deathMessage(null); // Suppress vanilla death message
            if (dmMode == DeathMessageMode.HIDDEN) {
                // Notify only admins
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("hakorun.admin")) {
                        p.sendMessage(Component.text("[HakoRun][숨김] " + player.getName() + " 사망 기록됨.").color(NamedTextColor.DARK_GRAY));
                    }
                }
            }
        }

        // Fire hook
        plugin.getHookManager().fireOnPlayerDeath(session, player, deathMsg);

        // Handle life deduction
        boolean shouldFail = plugin.getLifeManager().handleDeath(player, session);

        if (shouldFail) {
            // Schedule fail on next tick to allow death event to complete
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getRunManager().getCurrentState() == RunState.RUNNING) {
                    plugin.getRunManager().failRun(player.getName() + " 사망으로 인한 목숨 소진");
                }
            }, 1L);
        } else {
            // Announce remaining lives
            LifePolicy policy = plugin.getConfigManager().getLifePolicy();
            if (policy == LifePolicy.SHARED_POOL) {
                int remaining = session.getSharedLivesRemaining();
                Bukkit.broadcast(Component.text("[HakoRun] 목숨이 " + remaining + "개 남았습니다.").color(NamedTextColor.RED));
            }
            // Move to spectator after death
            spectating.add(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR);
            }, 20L);
        }

        plugin.getHudManager().updateAll();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        RunSession session = plugin.getRunManager().getCurrentSession();
        RunState state = plugin.getRunManager().getCurrentState();

        if (state != RunState.RUNNING || session == null) return;

        // If player is in spectating set, keep them in spectator at lobby
        if (spectating.contains(player.getUniqueId())) {
            spectating.remove(player.getUniqueId());
            // Respawn at lobby
            event.setRespawnLocation(plugin.getConfigManager().getLobbySpawn());
        }
    }

    public void clearSpectating() {
        spectating.clear();
    }
}
