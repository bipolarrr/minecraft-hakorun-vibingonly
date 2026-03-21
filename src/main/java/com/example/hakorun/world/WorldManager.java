package com.example.hakorun.world;

import com.example.hakorun.HakoRunPlugin;
import com.example.hakorun.model.RunSession;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;

public class WorldManager {
    private final HakoRunPlugin plugin;

    public WorldManager(HakoRunPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates the three worlds for a run session synchronously on the main thread.
     * Must be called from the main server thread.
     */
    public boolean createRunWorlds(RunSession session) {
        String owName = session.getOverworldName();
        String neName = session.getNetherName();
        String enName = session.getEndName();

        plugin.getLogger().info("[HakoRun] 월드 생성 시작 | runId=" + session.getRunId()
                + " | attempt=" + session.getAttemptIndex()
                + " | overworldSeed=" + session.getOverworldSeed()
                + " | netherSeed=" + session.getNetherSeed()
                + " | endSeed=" + session.getEndSeed());

        // Safety: check if worlds already loaded (should never happen with unique runId)
        if (Bukkit.getWorld(owName) != null || Bukkit.getWorld(neName) != null || Bukkit.getWorld(enName) != null) {
            plugin.getLogger().warning("[HakoRun] 이미 같은 이름의 월드가 로드되어 있습니다. runId=" + session.getRunId());
            return false;
        }

        try {
            World overworld = createWorld(owName, World.Environment.NORMAL, session.getOverworldSeed());
            if (overworld == null) { plugin.getLogger().severe("[HakoRun] Overworld 생성 실패: " + owName); return false; }

            World nether = createWorld(neName, World.Environment.NETHER, session.getNetherSeed());
            if (nether == null) { plugin.getLogger().severe("[HakoRun] Nether 생성 실패: " + neName); return false; }

            World end = createWorld(enName, World.Environment.THE_END, session.getEndSeed());
            if (end == null) { plugin.getLogger().severe("[HakoRun] End 생성 실패: " + enName); return false; }

            plugin.getLogger().info("[HakoRun] 월드 생성 완료 | " + owName + ", " + neName + ", " + enName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[HakoRun] 월드 생성 중 예외 발생", e);
            return false;
        }
    }

    private World createWorld(String name, World.Environment env, long seed) {
        WorldCreator creator = new WorldCreator(name);
        creator.environment(env);
        creator.seed(seed);
        if (env == World.Environment.NETHER) {
            creator.generator((ChunkGenerator) null);
        }
        World world = creator.createWorld();
        if (world != null) {
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            world.setDifficulty(Difficulty.HARD);
        }
        return world;
    }

    /**
     * Re-loads existing run world folders after a server restart.
     * Does not generate new chunks — only loads what is already on disk.
     * Returns true if all three worlds are now loaded.
     */
    public boolean loadRunWorlds(RunSession session) {
        String owName = session.getOverworldName();
        String neName = session.getNetherName();
        String enName = session.getEndName();

        File container = Bukkit.getWorldContainer();
        if (!new File(container, owName).isDirectory()
                || !new File(container, neName).isDirectory()
                || !new File(container, enName).isDirectory()) {
            plugin.getLogger().warning("[HakoRun] 런 월드 폴더 없음, 복구 불가. runId=" + session.getRunId());
            return false;
        }

        try {
            World overworld = loadExistingWorld(owName, World.Environment.NORMAL);
            if (overworld == null) { plugin.getLogger().severe("[HakoRun] Overworld 로드 실패: " + owName); return false; }

            World nether = loadExistingWorld(neName, World.Environment.NETHER);
            if (nether == null) { plugin.getLogger().severe("[HakoRun] Nether 로드 실패: " + neName); return false; }

            World end = loadExistingWorld(enName, World.Environment.THE_END);
            if (end == null) { plugin.getLogger().severe("[HakoRun] End 로드 실패: " + enName); return false; }

            plugin.getLogger().info("[HakoRun] 런 월드 복구 완료 | " + owName + ", " + neName + ", " + enName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[HakoRun] 런 월드 로드 중 예외", e);
            return false;
        }
    }

    private World loadExistingWorld(String name, World.Environment env) {
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;
        WorldCreator creator = new WorldCreator(name);
        creator.environment(env);
        World world = creator.createWorld();
        if (world != null) {
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            world.setDifficulty(Difficulty.HARD);
        }
        return world;
    }

    /**
     * Safely unloads and optionally deletes run worlds.
     * Evacuates any players still in those worlds to lobby first.
     */
    public void cleanupRunWorlds(RunSession session, boolean delete) {
        String owName = session.getOverworldName();
        String neName = session.getNetherName();
        String enName = session.getEndName();

        World lobby = getLobbyWorld();

        unloadWorld(owName, lobby, delete);
        unloadWorld(neName, lobby, delete);
        unloadWorld(enName, lobby, delete);
    }

    private void unloadWorld(String worldName, World fallback, boolean delete) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            if (delete) deleteWorldFolder(worldName);
            return;
        }

        // Evacuate players
        Location safeSpawn = fallback != null ? fallback.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
        for (Player p : world.getPlayers()) {
            try {
                p.teleport(safeSpawn);
            } catch (Exception e) {
                plugin.getLogger().warning("[HakoRun] 플레이어 대피 실패: " + p.getName());
            }
        }

        boolean unloaded = Bukkit.unloadWorld(world, !delete); // save=true if not deleting
        if (!unloaded) {
            plugin.getLogger().warning("[HakoRun] 월드 언로드 실패: " + worldName);
            return;
        }

        if (delete) {
            deleteWorldFolder(worldName);
        }
    }

    private void deleteWorldFolder(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists()) return;
        try {
            deleteDirectory(worldFolder.toPath());
            plugin.getLogger().info("[HakoRun] 월드 폴더 삭제 완료: " + worldName);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[HakoRun] 월드 폴더 삭제 실패 (무시하고 계속): " + worldName, e);
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public World getLobbyWorld() {
        String lobbyName = plugin.getConfigManager().getLobbyWorldName();
        World w = Bukkit.getWorld(lobbyName);
        if (w == null) w = Bukkit.getWorlds().get(0);
        return w;
    }

    public World getRunOverworld(RunSession session) {
        return session != null ? Bukkit.getWorld(session.getOverworldName()) : null;
    }

    public World getRunNether(RunSession session) {
        return session != null ? Bukkit.getWorld(session.getNetherName()) : null;
    }

    public World getRunEnd(RunSession session) {
        return session != null ? Bukkit.getWorld(session.getEndName()) : null;
    }

    public boolean isRunWorld(String worldName, RunSession session) {
        if (session == null) return false;
        return worldName.equals(session.getOverworldName())
                || worldName.equals(session.getNetherName())
                || worldName.equals(session.getEndName());
    }
}
