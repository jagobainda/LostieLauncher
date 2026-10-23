# Agent documentation index

Read the documents below before the corresponding work. Follow links explicitly;
this index does not load their contents. Read the shared rules and only the
platform documents relevant to the task. Paths are relative to this repository.

## Shared topics

| When | Read |
| --- | --- |
| Every task | [.agents/general.md](.agents/general.md) |
| Branching, committing or opening a PR | [.agents/workflow.md](.agents/workflow.md) |
| Changing application code | [.agents/architecture.md](.agents/architecture.md) |
| Changing behavior or tests | [.agents/testing.md](.agents/testing.md) |
| Changing visible text, colors or themes | [.agents/localization-and-themes.md](.agents/localization-and-themes.md) |
| Changing CI, dependencies or repository configuration | [.agents/infrastructure.md](.agents/infrastructure.md) |
| Changing documentation or agent instructions | [.agents/documentation.md](.agents/documentation.md) |

## Platform topics

| When | Desktop (`desktop/`) | Android (`android/`) |
| --- | --- | --- |
| Changing application code | [Architecture](desktop/.agents/architecture.md) | [Architecture](android/.agents/architecture.md) |
| Writing source or build files | [Code style](desktop/.agents/code-style.md) | [Code style](android/.agents/code-style.md) |
| Changing dependency injection or network configuration | [Architecture](desktop/.agents/architecture.md) | [Dependencies](android/.agents/dependencies.md) |
| Changing ViewModels, concurrency or lifecycle | [MVVM and threading](desktop/.agents/mvvm-and-threading.md) | [State and lifecycle](android/.agents/state-and-lifecycle.md) |
| Changing screens, components or dialogs | [MVVM and threading](desktop/.agents/mvvm-and-threading.md) | [UI](android/.agents/ui.md) |
| Changing downloads, installation or game launch | [Architecture](desktop/.agents/architecture.md) | [Game runtime](android/.agents/game-runtime.md) |
| Writing or editing tests | [Testing](desktop/.agents/testing.md) | [Testing](android/.agents/testing.md) |
| Changing visible text | [Localization](desktop/.agents/localization.md) | [Localization](android/.agents/localization.md) |
| Changing colors, themes or visual tokens | [Themes](desktop/.agents/themes.md) | [Themes](android/.agents/themes.md) |
| Building, changing build configuration or validating changes | [Build and validation](desktop/.agents/build-and-validation.md) | [Build and validation](android/.agents/build-and-validation.md) |

## Product references

| Topic | Read |
| --- | --- |
| Repository and contribution policy | [README.md](README.md), [CONTRIBUTING.md](CONTRIBUTING.md) |
| Desktop product, architecture and configuration | [desktop/README.md](desktop/README.md) |
| Android product, architecture and configuration | [android/README.md](android/README.md) |
| Android capabilities and remaining limitations | [Parity report](android/docs/parity-report.md), [game runtime](android/docs/game-runtime-options.md) |
