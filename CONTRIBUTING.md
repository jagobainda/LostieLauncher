# Contribution guide

Thanks for your interest in contributing to **LostieLauncher**! This document
summarizes the workflow, conventions, and requirements that any contribution
must meet to be merged into the project.

LostieLauncher is a **monorepo** holding two applications that share a product
but not a stack:

| Side                   | Stack                     | Status                  |
| ---------------------- | ------------------------- | ----------------------- |
| [`desktop/`](desktop/) | WPF · .NET 10 · C# · MVVM | Shipping                |
| [`android/`](android/) | Kotlin · Compose · MVVM   | Not implemented yet     |

Before you start, review the [monorepo overview](README.md) and then the README
of the side you are contributing to — [desktop](desktop/README.md) or
[android](android/README.md).

> [!IMPORTANT]
> **Run each side's commands from that side's folder, never from the repository
> root.** The configuration that makes them work lives inside the folder:
> `desktop/global.json` is what opts the repo into the test runner, and it is
> found by walking up from the current working directory. From the root,
> `dotnet test` fails with _"Testing with VSTest target is no longer supported"_
> — that means you are in the wrong directory, not that anything is broken.

Keep a pull request to **one side**. A change that touches both `desktop/` and
`android/` needs a reason; do not fix something on the other side while you are
in there.

---

## 🔀 Git workflow

1. **Fork** the repository.
2. Always branch off **`development`**, not `main`.
3. Create a descriptive branch for your change using the
   `<type>/<short-english-description>` format. Use the most specific type:
   `feat/` for features, `fix/` for bug fixes, `security/` for vulnerability
   fixes, `chore/` for maintenance, `docs/` for documentation, `refactor/` for
   restructuring, `perf/` for performance improvements, `test/` for tests only,
   `ci/` for CI/CD changes, and `build/` for build or packaging changes.
4. Write the description in concise English, using lowercase kebab-case with
   only letters, numbers, and hyphens. For example:
   `fix/login-crash-empty-password`.
5. Make your commits.
6. Open a **Pull Request against `development`**. Merging into `main` is reserved
   for releases.

`main` is the stable release branch; `development` is the integration branch
where changes come together before a new version.

---

## ✅ Before opening a Pull Request

Your PR must pass the CI jobs in `.github/workflows/ci.yml`. Run them locally to
avoid surprises. The commands below are the **desktop** gates, and CI runs them
with `working-directory: desktop` — so run them from `desktop/` too.

### 1. Formatting (mandatory)

CI rejects the PR if the code is not formatted. Apply formatting with:

```powershell
cd desktop
dotnet format LostieLauncher.slnx
```

CI verifies it with `dotnet format --verify-no-changes`, so make sure no changes
remain pending.

### 2. Build and tests

```powershell
cd desktop
dotnet restore LostieLauncher.slnx
dotnet build LostieLauncher.slnx --no-restore --configuration Release
dotnet test  LostieLauncher.slnx --no-build --configuration Release
```

> The project treats **warnings as errors** (including `IDE0005` for unused
> `using`s). A warning will break the build.

### 3. Vulnerable dependencies

CI fails if there are NuGet packages with known vulnerabilities. You can check
with:

```powershell
cd desktop
dotnet list LostieLauncher.slnx package --vulnerable --include-transitive
```

---

## 🧪 Tests

Desktop tests live in `desktop/LostieLauncher.Tests/`, whose folder structure
mirrors the production project (`Services/`, `ViewModels/`, `Utils/`, `Models/`,
`Helpers/`) so you can easily locate the matching test.

Test stack:

- **[xUnit v3](https://xunit.net/)** — test framework.
- **[NSubstitute](https://nsubstitute.github.io/)** — test doubles (mocks).
- **[Shouldly](https://docs.shouldly.org/)** — readable assertions.

Guidelines:

- Add or update tests for any behavior change.
- Tests **must not** load XAML, instantiate windows, or use a real `Dispatcher`:
  they must be able to run on a headless CI agent.
- The app exposes its internals to the tests via `InternalsVisibleTo`, so you
  can test `internal` types without making them public.

---

## 📦 Dependencies

Dependency updates (NuGet and GitHub Actions) are handled automatically by
**Dependabot** through PRs — the `nuget` ecosystem is pointed at `/desktop`,
where the .NET projects live. Thanks to `.github/CODEOWNERS`, the maintainer is
automatically assigned as reviewer on those PRs. You don't need to update
dependencies manually unless your change requires it.

---

## ✔️ Acceptance requirements

For a Pull Request to be accepted it **must mandatorily meet all the
requirements described in this document** (formatting, build, tests, absence of
vulnerable dependencies, and the Git workflow). It must also be **free of merge
conflicts** with `development`; if conflicts arise, you are responsible for
rebasing or merging the latest `development` and resolving them. A PR that fails
any of them will not be merged.

As the maintainer, I **reserve the final say** on what gets merged into the
`development` branch. Meeting all requirements is a necessary condition, but does
not automatically guarantee a merge: any contribution may be rejected or have
changes requested for reasons of **quality control**, consistency with the
architecture, or the project's direction.

---

## 🤖 Use of AI

Using AI tools as an **assistant** during development is allowed. That said:

- Contributions **generated by autonomous agents will be rejected
  automatically.**
- AI must be used only as support. **The developer is ultimately responsible**
  for the code they submit: for maintaining good practices, code quality, and
  security.

If you work with a coding assistant, point it at [AGENTS.md](AGENTS.md): it is
the machine-readable version of this guide. It holds the rules that apply across
the monorepo and routes to the side you are working on —
[desktop/AGENTS.md](desktop/AGENTS.md) or [android/AGENTS.md](android/AGENTS.md)
— where the architecture rules, code conventions, testing constraints and the
exact commands CI runs live. Most agentic tools read it automatically; Claude
Code picks it up through the `CLAUDE.md` pointer at the repository root.
Keeping the assistant inside those rules saves you review cycles — but it
does not transfer responsibility for the result.

---

## 🐛 Reporting bugs or proposing improvements

Open an **issue** describing the problem or proposal in as much detail as
possible (steps to reproduce, version, operating system, screenshots if
applicable). If you're going to work on a large change, discuss it first in an
issue to agree on the approach.

---

Thanks for contributing! 🎮
