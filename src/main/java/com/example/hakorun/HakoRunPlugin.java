package com.example.hakorun;

import com.example.hakorun.command.HakoRunCommand;
import com.example.hakorun.config.ConfigManager;
import com.example.hakorun.hook.HookManager;
import com.example.hakorun.hud.HudManager;
import com.example.hakorun.life.LifeManager;
import com.example.hakorun.listener.DeathListener;
import com.example.hakorun.listener.DragonListener;
import com.example.hakorun.listener.PortalListener;
import com.example.hakorun.model.RunState;
import com.example.hakorun.run.RunManager;
import com.example.hakorun.world.WorldManager;
import com.example.hakorun.world.WorldOperationQueue;
import com.example.hakorun.world.WorldTransitionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class HakoRunPlugin extends JavaPlugin {
    private static HakoRunPlugin instance;

    private ConfigManager configManager;
    private WorldOperationQueue worldOperationQueue;
    private WorldManager worldManager;
    private WorldTransitionService worldTransitionService;
    private LifeManager lifeManager;
    private HookManager hookManager;
    private HudManager hudManager;
    private RunManager runManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.load();

        worldOperationQueue = new WorldOperationQueue(this);
        worldManager = new WorldManager(this);
        worldTransitionService = new WorldTransitionService(this, worldManager, worldOperationQueue);
        lifeManager = new LifeManager(this);
        hookManager = new HookManager(this);
        hudManager = new HudManager(this);
        runManager = new RunManager(this);
        runManager.load();

        HakoRunCommand commandHandler = new HakoRunCommand(this);
        getCommand("hakorun").setExecutor(commandHandler);
        getCommand("hakorun").setTabCompleter(commandHandler);

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalListener(this), this);
        getServer().getPluginManager().registerEvents(new DragonListener(this), this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                if (runManager.getCurrentState() == RunState.RUNNING) {
                    hudManager.addPlayerToBossBar(event.getPlayer());
                    hudManager.updateAll();
                }
            }
        }, this);

        hudManager.startUpdateTask();

        getLogger().info("[HakoRun] 플러그인이 활성화되었습니다. 버전: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (hudManager != null) hudManager.shutdown();
        if (worldOperationQueue != null) worldOperationQueue.shutdown();
        if (runManager != null) runManager.save();
        getLogger().info("[HakoRun] 플러그인이 비활성화되었습니다.");
    }

    public static HakoRunPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public WorldTransitionService getWorldTransitionService() { return worldTransitionService; }
    public WorldOperationQueue getWorldOperationQueue() { return worldOperationQueue; }
    public LifeManager getLifeManager() { return lifeManager; }
    public HookManager getHookManager() { return hookManager; }
    public HudManager getHudManager() { return hudManager; }
    public RunManager getRunManager() { return runManager; }
}
