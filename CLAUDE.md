# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

```bash
mvn clean package
```

Output JAR goes to `target/`. Deploy by copying to a Paper 1.21.1 server's `plugins/` directory. There are no tests in this project.

## Architecture

**HakoRun** is a Paper Minecraft plugin that automates hardcore Ender Run (하코런) challenge sessions — world generation, player lifecycle, death tracking, lives management, and HUD rendering.

### Run State Machine

```
LOBBY → PREPARING_NEXT_RUN → READY_TO_TRANSFER → RUNNING → FAILED | WON → LOBBY
```

`RunManager` is the central state machine. All state transitions go through it. `RunSession` holds the active run's data (runId, seeds, deaths, lives) and is persisted to `data.yml` across restarts.

### Package Responsibilities

| Package | Role |
|---|---|
| `run/` | Run lifecycle (start, fail, win, serialization) |
| `world/` | World creation/deletion (`WorldManager`) and player transitions (`WorldTransitionService`) |
| `life/` | Life deduction logic and policy enforcement |
| `hud/` | Sidebar, action bar, boss bar rendering (1 Hz refresh) |
| `command/` | `/hakorun` (`/hr`) command dispatch and tab completion |
| `listener/` | Death → `LifeManager`, Dragon kill → `RunManager.winRun()`, Portals → dimension transitions |
| `hook/` | Shell command execution on run events |
| `config/` | `config.yml` (settings) + `data.yml` (session persistence) |
| `model/` | `RunState`, `RunMode`, `LifePolicy`, `DeathMessageMode`, `RunSession`, `DeathRecord` |

### Threading Model

- **Main thread**: World creation, player teleport, HUD updates, event handling, Bukkit scheduler tasks
- **`WorldOperationQueue`** (single background thread): Heavy disk I/O like world deletion
- Do not call Bukkit API off the main thread. Schedule back to main thread via `Bukkit.getScheduler().runTask(...)`.

### Key Design Points

- **Worlds**: Each run generates 3 independent worlds (Overworld/Nether/End) with separate seeds. Portal transitions use 1:8 coordinate scaling for OW↔Nether.
- **Life Policies**: `SHARED_POOL` (team pool) vs `PER_PLAYER` (independent per player). `RunMode.INSTANT_WIPE` bypasses life tracking and fails on first death.
- **Death messages**: `VANILLA`, `HIDDEN`, or `REVEAL_ON_WIPE` — controlled by cancelling `PlayerDeathEvent`.
- **HUD**: Per-player toggleable sidebar sections (policy info, alive count). Action bar shows lives/time/attempt.
- **Persistence**: Session state fully serialized to `data.yml`; plugin recovers in-progress runs on server restart.
- **Hooks**: Shell commands fired on `on_run_start`, `on_run_fail`, `on_run_win` events via `HookManager`.

### Initialization Order (`HakoRunPlugin.onEnable`)

1. `ConfigManager.load()` — reads `config.yml` and `data.yml`
2. `WorldOperationQueue` — background executor
3. Managers: `WorldManager` → `WorldTransitionService` → `LifeManager` → `HookManager` → `HudManager` → `RunManager`
4. Register command executor + tab completer
5. Register `DeathListener`, `DragonListener`, `PortalListener`
6. Start HUD update task (every 20 ticks = 1 s)

### Permissions

- `hakorun.admin` — required for all `/hakorun` subcommands (OP by default)
- `hakorun.player` — granted to all players by default
