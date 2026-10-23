# Desktop build and validation

Run commands from `desktop/`. [global.json](../global.json) selects the
Microsoft.Testing.Platform runner; it does not pin the SDK version. Use a
.NET 10 SDK, as [CI](../../.github/workflows/ci.yml) does.

## Full CI gates

```powershell
dotnet restore LostieLauncher.slnx
dotnet format LostieLauncher.slnx --verify-no-changes --verbosity diagnostic
dotnet build LostieLauncher.slnx --no-restore --configuration Release
dotnet test LostieLauncher.slnx --no-build --configuration Release --verbosity normal
dotnet list LostieLauncher.slnx package --vulnerable --include-transitive --format json --output-version 1
```

- Formatting is a separate gate. To fix formatting, run
  `dotnet format LostieLauncher.slnx`, then repeat verification.
- The NuGet command produces a report; success of the command alone does not
  prove the absence of vulnerabilities. CI parses its JSON and fails if a
  top-level or transitive package contains vulnerability entries. Inspect both.
- Repository policy treats warnings as failures. The test project sets
  `TreatWarningsAsErrors`; the app's `.editorconfig` escalates `IDE0005`.
  Do not assume every compiler warning in the app is automatically an error.
- Running tests from the repository root misses the test-runner configuration
  and can select the unsupported VSTest path. Preserve `desktop/global.json`
  in this folder.

## Release boundary

`scripts/build-release.ps1` is the maintainer's packaging path, writing
`desktop/publish/` and `desktop/releases/`. Feature and fix work must not
change the version fields in `LostieLauncher.csproj` or README version badges.
