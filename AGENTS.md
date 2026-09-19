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
| porting a behavior between the two sides        | [spec/README.md](spec/README.md)           |
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
├── .gitattributes      # line endings Git must enforce (gradlew stays LF)
├── .gitignore          # covers both sides (patterns match at any depth)
├── AGENTS.md           # this file
├── CLAUDE.md           # pointer: @AGENTS.md
├── CONTRIBUTING.md     # human contribution guide
├── LICENSE.txt
├── README.md           # monorepo overview and product landing page
├── spec/               # product specification — the port contract, read by both sides
│   ├── README.md       #   index and precedence rules
│   ├── 01-…10-….md     #   overview, screens, services, data, text, tokens,
│   │                   #   components, dialogs, utilities, Windows-only
│   └── samples/        #   captured CDN payloads
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
└── android/            # Android app — Kotlin, Compose, MVVM
    ├── .agents/        #   Android-only agent rules (4 topic files)
    ├── .editorconfig   #   Kotlin and Gradle rules
    ├── AGENTS.md       #   Android index
    ├── CLAUDE.md       #   pointer: @AGENTS.md
    ├── README.md       #   Android architecture, stack, build
    ├── build.gradle.kts, settings.gradle.kts, gradle.properties
    ├── gradle/         #   libs.versions.toml + the pinned wrapper
    ├── gradlew(.bat)   #   the wrapper — see "Where to run commands"
    └── app/            #   the app and its unit tests
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
`cd desktop` first.

The Android side works the same way: the Gradle build, the version catalogue and
the wrapper all live in `android/`, and `./gradlew` only exists there. `cd
android` first.

The CI jobs do the same thing, with `working-directory: desktop` on the desktop
jobs and `working-directory: android` on the Android ones.

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

`spec/` is the product specification: what the launcher does and what it looks
like, extracted from the desktop application so the Android side does not have
to re-read the WPF project on every change. It is documentation only, it belongs
to neither side, and **the desktop code is the authority wherever the two
disagree** — a mismatch is a bug in `spec/`, not a licence to change behavior.
Read [spec/README.md](spec/README.md) before porting anything.

`.github/` serves both sides, so treat it as shared ground:

- **`workflows/ci.yml`** — five jobs. `format-check`, `build-and-test` and
  `vulnerable-dependencies` are the desktop's, on `windows-latest` with
  `working-directory: desktop`; the two `android-*` ones are on `ubuntu-latest`
  with `working-directory: android`. A new job scopes itself the same way rather
  than changing the defaults for everyone, and carries the name of its side —
  the three desktop ones keep their original unprefixed names so existing branch
  protection rules still match.
- **`dependabot.yml`** — `nuget` points at `/desktop`, `gradle` at `/android`,
  `github-actions` at `/`. A new ecosystem gets its own entry; do not repoint an
  existing one.
- **`CODEOWNERS`** — patterns without a leading slash match at any depth, which
  is why `*.csproj` still works after the move to `desktop/`, and why
  `*.gradle.kts` and `libs.versions.toml` are written the same way.
- **`.editorconfig`** — the root file is the baseline for *every* file in the
  repository and carries `root = true`. Language rules belong in the side's own
  `.editorconfig`, which deliberately omits `root` so it inherits. Do not move
  language rules up to the root file.
- **`.gitattributes`** — at the repository root, and it exists for one reason:
  the baseline is CRLF, but `android/gradlew` is a shell script the Linux CI
  runner executes, so it is forced to LF here regardless of anyone's
  `core.autocrlf`. Leave that rule alone.
