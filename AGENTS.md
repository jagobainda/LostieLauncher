# Agent guidelines — monorepo

Instructions for AI coding agents (Claude Code, Codex, Copilot, Cursor, …)
working in this repository. This file is the **global** index: it holds the
rules that apply to every change, on either side of the monorepo, and routes you
to the side you are actually working on.

This repository holds **two applications that share a product, not a stack**: a
Windows desktop launcher and an Android app. They have separate builds, separate
toolchains, separate conventions and separate guidelines. Read the ones for your
side **before** you write code. The links are plain Markdown on purpose — the
detail is loaded on demand, not injected into every session — so following them
is your job, not the tool's.

## Pick your side

| Working on…                                     | Read                                       |
| ----------------------------------------------- | ------------------------------------------ |
| the desktop launcher (WPF, .NET 10, C#)         | [desktop/AGENTS.md](desktop/AGENTS.md)     |
| the Android app (Kotlin, Compose)               | [android/AGENTS.md](android/AGENTS.md)     |
| branching, committing or opening a PR           | [.agents/workflow.md](.agents/workflow.md) |

Each side's `AGENTS.md` is authoritative for that side: its layer rules, its code
style, its test constraints and the exact commands its CI jobs run. Nothing in
this file overrides them; it only states what is true regardless of which side
you are on.

The human-facing docs are [CONTRIBUTING.md](CONTRIBUTING.md) (workflow and
acceptance criteria) and [README.md](README.md) (the monorepo and the product).
Nothing here may contradict them; if it does, they win and the drift is a bug
worth fixing.

## Repository layout

```
├── .agents/            # global agent rules (workflow, boundaries)
│   └── workflow.md     #   git, PR and boundary rules for both sides
├── .github/            # CI, Dependabot and CODEOWNERS for both sides
├── .editorconfig       # monorepo baseline only (charset, CRLF, indentation)
├── .gitignore          # covers both sides (patterns match at any depth)
├── AGENTS.md           # this file
├── CLAUDE.md           # pointer: @AGENTS.md
├── CONTRIBUTING.md     # human contribution guide
├── LICENSE.txt
├── README.md           # monorepo overview and product landing page
├── desktop/            # Windows launcher — WPF, .NET 10, C#
│   ├── .agents/        #   desktop-only agent rules (4 topic files)
│   ├── .editorconfig   #   C#, XAML and MSBuild rules
│   ├── AGENTS.md       #   desktop index
│   ├── CLAUDE.md       #   pointer: @AGENTS.md
│   ├── README.md       #   desktop architecture, build and configuration
│   ├── global.json     #   test runner opt-in — see "Where to run commands"
│   ├── LostieLauncher.slnx
│   ├── LostieLauncher/         # the app
│   ├── LostieLauncher.Tests/   # unit tests
│   └── scripts/        #   release packaging (maintainer only)
└── android/            # Android app — Kotlin, Compose (not implemented yet)
    ├── AGENTS.md       #   what the Android guidelines must cover
    ├── CLAUDE.md       #   pointer: @AGENTS.md
    └── README.md       #   status and scope of the Android side
```

## Where to run commands

**Each side's commands run from that side's folder, never from the repository
root.** This is not a style preference: the configuration that makes them work
lives inside the folder. On the desktop side, `desktop/global.json` is what opts
the repo into the Microsoft.Testing.Platform runner, and it is discovered by
walking up from the **current working directory**. Run `dotnet test` from the
repository root and it will not be found:

```
error : Testing with VSTest target is no longer supported by
Microsoft.Testing.Platform on .NET 10 SDK and later.
```

That error means you are in the wrong directory, not that anything is broken.
`cd desktop` first. The CI jobs do the same thing with
`working-directory: desktop`.

## Global non-negotiables

These apply to every change, on both sides, with no topic file to look up:

1. **Everything you write is in English**: code, comments, docs, commits, branch
   names, PR text.
2. **Branch off `development`**; PRs target `development`, never `main`. See
   [.agents/workflow.md](.agents/workflow.md).
3. **One side per change.** A PR that touches `desktop/` and `android/` together
   needs a reason. Do not "fix something while you're in there" on the other
   side — you will be reviewed by someone who only opened the PR for one of them.
4. **Run that side's full CI gates locally before proposing a change**, from that
   side's folder. A PR that fails any of them will not be merged.
5. **Warnings are failures** on both sides.
6. **Never bump the version** (`<Version>` / `<FileVersion>` / `<AssemblyVersion>`,
   the Gradle `versionName` / `versionCode`, or the README badges) as part of a
   feature or fix. Releases are the maintainer's path.
7. **Stay inside the scope of what was asked.** You assist; the developer opening
   the PR is responsible for the result. See
   [Boundaries](.agents/workflow.md#boundaries).

## Shared infrastructure

`.github/` serves both sides, so treat it as shared ground:

- **`workflows/ci.yml`** — the desktop jobs set `working-directory: desktop`. An
  Android job must scope itself the same way rather than changing the defaults
  for everyone.
- **`dependabot.yml`** — the `nuget` ecosystem points at `/desktop`. A new
  ecosystem gets its own entry; do not repoint an existing one.
- **`CODEOWNERS`** — patterns without a leading slash match at any depth, which
  is why `*.csproj` still works after the move to `desktop/`.
- **`.editorconfig`** — the root file is the baseline for *every* file in the
  repository and carries `root = true`. Language rules belong in the side's own
  `.editorconfig`, which deliberately omits `root` so it inherits. Do not move
  language rules up to the root file.
