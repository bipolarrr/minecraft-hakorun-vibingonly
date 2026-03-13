Fix only issue #3 in the existing Minecraft Paper plugin project “HakoRun”.

Context:
- Java 21
- Maven
- PaperMC 1.21.11
- Existing codebase, not a rewrite

Issue #3:
- The lives policy names are not human-friendly enough.
- I want the lives policy to be presented as 2 easy-to-read modes:
  - 인당 데스
  - 공유목숨

Requirements:
- Keep the patch minimal
- Do not redesign the lives system
- Do not change unrelated run/world/HUD logic
- Do not rename large parts of the internal architecture unless truly necessary
- Prefer keeping existing internal enum/logic if possible
- Focus on human-friendly display names and command input handling
- Do not add placeholder code
- Do not write a long explanation

Preferred approach:
- If the project already has internal modes like SHARED_POOL / PER_PLAYER, keep them internally
- Add or adjust user-facing labels so they display as:
  - 인당 데스
  - 공유목숨
- If commands currently accept only technical enum names, allow human-friendly aliases too
- Preserve backward compatibility with existing technical inputs if that is easy and low-risk

Scope:
- Search only the files directly related to:
  - lives policy enum or model
  - lives-related command parsing
  - user-facing text/messages for lives mode
- Do not perform a broad architecture review

Paper/plugin constraints:
- Keep behavior stable
- Do not break existing command flow
- Do not change persistence format unless absolutely required
- If persistence already stores enum names, prefer keeping that unchanged

Success condition:
- The plugin exposes exactly two human-friendly lives policy names:
  - 인당 데스
  - 공유목숨
- Existing internal behavior still works
- Existing technical mode values can remain internally if safer
- The patch stays small and plausible for Paper 1.21.11

Output:
- Apply the patch directly
- Keep commentary minimal
- If needed, give only a very short summary of touched files