package com.example.hakorun.listener;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.RunSession;
import com.example.hakorun.model.RunState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DragonListener implements Listener {
    private final HakoRunPlugin plugin;

    public DragonListener(HakoRunPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;

        RunSession session = plugin.getRunManager().getCurrentSession();
        RunState state = plugin.getRunManager().getCurrentState();

        if (state != RunState.RUNNING || session == null) return;

        String worldName = dragon.getWorld().getName();
        if (!worldName.equals(session.getEndName())) return;

        session.setDragonDead(true);
        plugin.getRunManager().save();

        plugin.getLogger().info("[HakoRun] 엔더드래곤 처치! attempt=" + session.getAttemptIndex());
        Bukkit.broadcast(Component.text("[HakoRun] 엔더드래곤이 처치되었습니다! 엔드 포털로 진입하면 승리합니다!").color(NamedTextColor.LIGHT_PURPLE));

        plugin.getHudManager().showTitleAll(
                Component.text("엔더드래곤 처치!").color(NamedTextColor.LIGHT_PURPLE),
                Component.text("엔드 포털로 진입하세요!").color(NamedTextColor.GREEN)
        );
    }
}
