# Shared architecture rules

Read the affected platform's architecture document for its layers and wiring.

- Keep MVVM boundaries: views render state and forward actions; ViewModels own
  presentation decisions; services own application operations; models carry data.
- Resolve application services through constructor injection and the platform's
  composition root. Do not construct a service at a use site or add a service locator.
- Inject environment-dependent endpoints, filesystem roots, timeouts and tuning
  options. Keep them out of the types that perform the work. Protocol constants
  and pure algorithm constants can stay beside their implementation.
- Isolate network, filesystem, process and platform operations behind narrow
  interfaces and adapters. Prefer pure decision functions for branching logic.
- Use the configured HTTP clients; preserve each client's timeout and purpose.
- Keep long operations asynchronous and cancellation-aware. Avoid blocking the
  UI thread and protect state shared across threads.
- Log failures through the platform's logging abstraction, in English, with
  enough context to explain what failed. Degrade to an explicit result or useful
  state instead of crashing on an unavailable content server. Preserve cancellation.
- Reuse existing helpers and abstractions. Add a new layer only when the change
  actually needs it; do not create empty packages or speculative infrastructure.
