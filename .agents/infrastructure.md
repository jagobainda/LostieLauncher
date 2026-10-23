# Shared infrastructure

- [CI](../.github/workflows/ci.yml) has three desktop jobs on `windows-latest`
  and two Android jobs on `ubuntu-latest`. Each job sets its platform's working
  directory. New jobs use the same convention and a platform-prefixed name.
- Keep build and test commands in each platform's `build-and-validation.md`
  synchronized with CI. A formatting apply command is not the verification gate.
- [Dependabot](../.github/dependabot.yml) scans NuGet in `/desktop`, Gradle in
  `/android` and GitHub Actions in `/`. Add an ecosystem without repointing
  another one. Velopack is explicitly excluded and maintained manually.
- [CODEOWNERS](../.github/CODEOWNERS) covers project files, Gradle build files,
  the version catalogue and workflows. Preserve pattern scope when moving files.
- Root [.editorconfig](../.editorconfig) defines the baseline and `root = true`.
  Platform files inherit it and hold language-specific rules without `root`.
- [.gitattributes](../.gitattributes) enforces LF for `gradlew` and shell scripts,
  CRLF for Windows scripts, and binary treatment for packaged assets. Preserve it.
- Run .NET commands from `desktop/` and Gradle commands from `android/`.
  `desktop/global.json` selects the test runner; it does not pin a .NET SDK.
  The Android wrapper and build configuration live under `android/`.
