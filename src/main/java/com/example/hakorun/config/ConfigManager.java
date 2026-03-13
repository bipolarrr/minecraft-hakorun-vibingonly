package com.example.hakorun.config;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.DeathMessageMode;
import com.example.hakorun.model.LifePolicy;
import com.example.hakorun.model.RunMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {
    private final HakoRunPlugin plugin;
    private FileConfiguration config;

    // Data file for persistent state
    private File dataFile;
    private YamlConfiguration dataConfig;

    public ConfigManager(HakoRunPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "[HakoRun] data.yml 생성 실패", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[HakoRun] data.yml 저장 실패", e);
        }
    }

    public YamlConfiguration getDataConfig() { return dataConfig; }

    // --- Config Getters ---

    public String getLobbyWorldName() {
        return config.getString("lobbyWorld", "world");
    }

    public Location getLobbySpawn() {
        World w = plugin.getServer().getWorld(getLobbyWorldName());
        if (w == null) w = plugin.getServer().getWorlds().get(0);
        double x = config.getDouble("lobbySpawnX", 0.5);
        double y = config.getDouble("lobbySpawnY", 64.0);
        double z = config.getDouble("lobbySpawnZ", 0.5);
        float yaw = (float) config.getDouble("lobbySpawnYaw", 0.0);
        float pitch = (float) config.getDouble("lobbySpawnPitch", 0.0);
        return new Location(w, x, y, z, yaw, pitch);
    }

    public void setLobbySpawn(Location loc) {
        config.set("lobbyWorld", loc.getWorld().getName());
        config.set("lobbySpawnX", loc.getX());
        config.set("lobbySpawnY", loc.getY());
        config.set("lobbySpawnZ", loc.getZ());
        config.set("lobbySpawnYaw", (double) loc.getYaw());
        config.set("lobbySpawnPitch", (double) loc.getPitch());
        plugin.saveConfig();
    }

    public int getSharedLives() {
        return config.getInt("sharedLives", 3);
    }

    public RunMode getRunMode() {
        String s = config.getString("mode", "TEAM_SHARED_LIVES");
        try { return RunMode.valueOf(s); } catch (Exception e) { return RunMode.TEAM_SHARED_LIVES; }
    }

    public void setRunMode(RunMode mode) {
        config.set("mode", mode.name());
        plugin.saveConfig();
    }

    public LifePolicy getLifePolicy() {
        String s = config.getString("lifePolicy", "SHARED_POOL");
        try { return LifePolicy.valueOf(s); } catch (Exception e) { return LifePolicy.SHARED_POOL; }
    }

    public void setLifePolicy(LifePolicy policy) {
        config.set("lifePolicy", policy.name());
        plugin.saveConfig();
    }

    public DeathMessageMode getDeathMessageMode() {
        String s = config.getString("deathMessageMode", "REVEAL_ON_WIPE");
        try { return DeathMessageMode.valueOf(s); } catch (Exception e) { return DeathMessageMode.REVEAL_ON_WIPE; }
    }

    public boolean isDeleteOldRunWorlds() {
        return config.getBoolean("deleteOldRunWorlds", true);
    }

    public int getRunStartCountdownSeconds() {
        return config.getInt("runStartCountdownSeconds", 5);
    }

    public boolean isUseBossBarTimer() {
        return config.getBoolean("useBossBarTimer", false);
    }

    public boolean isUseActionBarTimer() {
        return config.getBoolean("useActionBarTimer", true);
    }

    public boolean isRandomSeed() {
        return config.getBoolean("seed.random", true);
    }

    public long getFixedSeed() {
        return config.getLong("seed.fixedSeed", 0L);
    }

    public List<String> getHookCommands(String eventName) {
        List<String> list = config.getStringList("hooks." + eventName);
        return list != null ? list : new ArrayList<>();
    }
}
