# Maintaining documentation

- Keep the root [AGENTS.md](../AGENTS.md) as the only agent index. It contains
  reading conditions and links, not copies of the topic rules. Do not add
  platform `AGENTS.md` files or secondary indexes inside `.agents/`.
- Put rules shared by both applications in root `.agents/`. Keep framework,
  tooling and platform details in `desktop/.agents/` or `android/.agents/`.
- Give each file one coherent topic. Split when unrelated concerns accumulate,
  not to satisfy an arbitrary line limit. Link shared rules instead of repeating them.
- Keep actionable instructions in `.agents/`. Put product behavior, rationale,
  parity evidence and historical measurements in README files or `docs/`.
- Verify claims against source, build files, CI and fixtures. Distinguish a
  policy requirement from something the current build enforces automatically.
- Avoid fixed test totals, catalogue-key totals and dependency versions that
  already have a maintained source of truth. Link that source instead.
- Label historical measurements and manual checks with their context. Moving
  old evidence does not make it a fresh verification of the application.
- When moving or deleting a document, update inbound links, relative paths,
  anchors, README trees and CI comments that name it.
- For documentation changes, check local links and anchors, inspect the diff
  for lost rules and contradictions, and follow the applicable validation gates.
