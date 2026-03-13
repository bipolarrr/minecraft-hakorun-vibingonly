You are fixing exactly ONE issue in an existing Minecraft Paper plugin project.

Project context:
- Plugin name: HakoRun
- Java 21
- Maven
- PaperMC 1.21.11 API
- plugin.yml based plugin
- This is an already-existing vibe-coded codebase, not a new rewrite

Target issue to solve:
- During an active run, when the lives system or lives values are changed by command, the RIGHT-SIDE scoreboard does NOT immediately and correctly reflect the updated state.

Examples of the broken behavior:
- switching lives mode during a run does not update the sidebar correctly
- changing shared lives count does not immediately update the sidebar
- changing a player’s lives in per-player mode does not immediately update the sidebar
- the underlying memory state may change, but the displayed scoreboard remains stale or partially stale

Important design constraints you must preserve:
1. Lives can be changed live in-game by command
2. Both modes exist and must keep working:
   - SHARED_POOL
   - PER_PLAYER
3. Command-driven lives changes must always trigger this flow:
   1) memory state update
   2) persistent save
   3) HUD/scoreboard immediate refresh
   4) fail-condition re-evaluation
4. Do NOT use config-edit + reload
5. Do NOT remove existing features
6. Do NOT rewrite the plugin architecture unless absolutely necessary
7. Do NOT add placeholder code, stubs, fake TODO fixes, or commented-out bypasses
8. Use the smallest safe patch with the highest probability of real Paper runtime correctness

Your task:
Fix only the issue where the right-side scoreboard does not properly update after live lives-related changes during a run.

Required investigation scope:
Do NOT only inspect the scoreboard render method.
You must inspect all related flow, including:
- lives-related commands
- lives state mutation services/managers
- persistence/save calls
- HUD/scoreboard refresh entry points
- any cached sidebar text/state/score entries
- fail-condition re-evaluation flow
- any player-specific or global scoreboard handling
- relevant imports and type mismatches
- plugin.yml only if command wiring is relevant

Paper/Bukkit-specific requirements:
- Check whether the sidebar is rebuilt from fresh state or reusing stale cached values
- Check whether a scoreboard update is skipped because of state guards, mode guards, or wrong run-state checks
- Check whether the lives command updates memory but forgets to request HUD refresh
- Check whether refresh happens only for some players and not all affected players
- Check org.bukkit scoreboard usage carefully
- Keep thread context correct; do not move scoreboard/player/world API usage async unless explicitly safe
- If there are separate HUD channels (scoreboard/actionbar/bossbar), do not break the others while fixing sidebar refresh

Preferred fix style:
- Centralize or reuse an existing single refresh path if possible
- After any lives-related command succeeds, ensure the plugin refreshes the right-side scoreboard from current authoritative state
- Prefer re-rendering from fresh state over patching individual lines if that is safer
- Preserve existing gameplay behavior and command semantics

What NOT to do:
- Do not refactor unrelated systems
- Do not redesign the whole HUD system
- Do not rename large parts of the codebase without need
- Do not remove scoreboard features just to hide the bug
- Do not fix this by requiring reconnect, restart, reload, or manual scoreboard reset command

Success criteria:
1. When lives mode changes during RUNNING, the sidebar immediately reflects the new mode and corresponding values
2. When shared lives change during RUNNING, the sidebar immediately shows the new number
3. When a specific player’s lives change in PER_PLAYER mode during RUNNING, the sidebar immediately reflects the change
4. The fix works without restart/reload/rejoin
5. Fail-condition re-evaluation still happens correctly
6. Existing run flow and world flow remain intact

Process:
1. Find the true root cause
2. Trace the full mutation-to-render path
3. Apply the minimum safe patch
4. Re-check for any nearby compile/runtime issues introduced by the patch
5. Keep the change narrowly scoped to this issue

Output format:
1. Root cause
2. Why the sidebar became stale
3. Exact files changed
4. Minimal unified diff
5. Why this fix is safe in Paper 1.21.11
6. Any remaining edge cases only if strictly relevant

Here is the relevant code to inspect and patch:
- lives command classes
- lives/state manager classes
- HUD/scoreboard/sidebar renderer classes
- run/session manager classes if they affect refresh timing
- any service that handles post-command refresh or fail-condition evaluation

If some of those files are not obvious, search the project and identify them first before patching.