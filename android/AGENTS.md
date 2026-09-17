# Agent guidelines — Android

> **Placeholder.** The Android application does not exist yet. This file marks
> where its guidelines go and what they must cover; it is not yet a set of rules
> you can follow, because there is no code to apply them to.

Read [the global guidelines](../AGENTS.md) first. They already apply here:
English-only, branch off `development`, one side per change, no version bumps,
and the shared `.github/` infrastructure.

## What lives here, once the project lands

This folder will hold the native Android app — Kotlin, Jetpack Compose, MVVM —
as the second application of the monorepo. It is a **port of the desktop
launcher's behavior, not of its code**: the desktop side under
[`../desktop/`](../desktop/) is the reference for what the app does, while how it
does it is an Android decision.

Whoever creates the project is expected to leave behind guidelines of the same
depth as the desktop ones in [`../desktop/.agents/`](../desktop/.agents/):

| Topic                                                         | Desktop counterpart                                                         |
| --------------------------------------------------------------| --------------------------------------------------------------------------- |
| layer contract and the dependency rules between layers        | [architecture.md](../desktop/.agents/architecture.md)                       |
| formatting and the conventions a formatter cannot catch       | [code-style.md](../desktop/.agents/code-style.md)                           |
| test constraints and what must stay testable without a device | [testing.md](../desktop/.agents/testing.md)                                 |
| localized text and the theme system, kept complete            | [localization-and-themes.md](../desktop/.agents/localization-and-themes.md) |

Plus what has no desktop equivalent: coroutines and state handling, dependency
registration, and how anything touching the platform is made testable.

## Rules that already bind

1. **Do not touch `../desktop/`.** The desktop launcher is a shipping
   application with its own CI gates. Nothing on this side needs to modify it.
2. **Commands run from this folder**, not from the repository root — the same
   rule the desktop side follows for `global.json`.
3. The CI in [`../.github/workflows/ci.yml`](../.github/workflows/ci.yml) must
   build and test this side too, in its own jobs, without changing what the
   desktop jobs already do.
