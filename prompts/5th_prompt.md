Fix only issue #2 in the existing Minecraft Paper plugin project “HakoRun”.

Context:
- Java 21
- Maven
- PaperMC 1.21.11
- Existing codebase, not a rewrite

Issue #2:
- The right-side scoreboard shows too much information.
- I want the three core runtime fields to appear above the hotbar instead:
  1. remaining lives
  2. elapsed run time
  3. attempt / try count

Requirements:
- Make the hotbar-area HUD (prefer existing ActionBar path if present) show those 3 fields during a run
- Reduce the right-side scoreboard so it is no longer overloaded with those core fields
- Keep the patch minimal and conservative
- Do not redesign the HUD system
- Do not touch unrelated run/world/lives logic
- Do not add placeholder code
- Do not write a long explanation

Scope:
- Search only the files directly related to:
  - sidebar/scoreboard rendering
  - actionbar/hud rendering
  - the state source for lives / elapsed time / attempt count
- Reuse existing update flow if possible
- Do not create a large new abstraction unless required for a tiny safe patch

Paper constraints:
- Keep UI updates on the correct thread
- Prefer existing Adventure/Component style already used by the project
- Do not break existing scoreboard registration/objective logic

Success condition:
- During a run, lives / elapsed time / try count are visible above the hotbar
- The right-side scoreboard contains less information than before
- Build remains plausible for Paper 1.21.11

Output:
- Apply the patch directly
- Keep commentary minimal
- If needed, give only a very short summary of touched files