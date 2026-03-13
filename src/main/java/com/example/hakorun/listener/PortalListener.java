package com.example.hakorun.listener;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.RunSession;
import com.example.hakorun.model.RunState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PortalListener implements Listener {
    private final HakoRunPlugin plugin;

    public PortalListener(HakoRunPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        RunSession session = plugin.getRunManager().getCurrentSession();
        RunState state = plugin.getRunManager().getCurrentState();

        if (state != RunState.RUNNING || session == null) return;

        String fromWorld = player.getWorld().getName();

        // Only intercept portals within run worlds
        if (!plugin.getWorldManager().isRunWorld(fromWorld, session)) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            handleNetherPortal(event, session, fromWorld);
        } else if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            handleEndPortal(event, session, fromWorld, player);
        }
    }

    private void handleNetherPortal(PlayerPortalEvent event, RunSession session, String fromWorld) {
        World overworld = plugin.getWorldManager().getRunOverworld(session);
        World nether = plugin.getWorldManager().getRunNether(session);

        if (overworld == null || nether == null) {
            event.setCancelled(true);
            return;
        }

        if (fromWorld.equals(session.getOverworldName())) {
            // Overworld -> Nether
            Location dest = new Location(nether,
                event.getFrom().getX() / 8.0,
                event.getFrom().getY(),
                event.getFrom().getZ() / 8.0);
            dest.setY(Math.max(1, Math.min(nether.getMaxHeight() - 1, dest.getY())));
            event.setTo(dest);
        } else if (fromWorld.equals(session.getNetherName())) {
            // Nether -> Overworld
            Location dest = new Location(overworld,
                event.getFrom().getX() * 8.0,
                event.getFrom().getY(),
                event.getFrom().getZ() * 8.0);
            event.setTo(dest);
        } else {
            event.setCancelled(true);
        }
    }

    private void handleEndPortal(PlayerPortalEvent event, RunSession session, String fromWorld, Player player) {
        World overworld = plugin.getWorldManager().getRunOverworld(session);
        World end = plugin.getWorldManager().getRunEnd(session);

        if (overworld == null || end == null) {
            event.setCancelled(true);
            return;
        }

        if (fromWorld.equals(session.getOverworldName())) {
            // Overworld -> End (stronghold portal)
            Location endSpawn = end.getSpawnLocation();
            event.setTo(endSpawn);
        } else if (fromWorld.equals(session.getEndName())) {
            // End -> Overworld (return portal after dragon kill = win condition)
            if (session.isDragonDead()) {
                event.setCancelled(true);
                // Win!
                plugin.getRunManager().winRun();
            } else {
                // Return to overworld normally
                Location dest = overworld.getSpawnLocation();
                event.setTo(dest);
            }
        } else {
            event.setCancelled(true);
        }
    }
}
