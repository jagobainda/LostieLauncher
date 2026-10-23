# Desktop testing

Apply the [shared testing rules](../../.agents/testing.md).
Tests live in [LostieLauncher.Tests/](../LostieLauncher.Tests/), mirroring app
folders, normally one `<Type>Tests.cs` per type. The stack is xUnit v3,
NSubstitute and Shouldly; package versions live in the test project file.

## WPF boundary

- Do not instantiate windows, load view XAML or pump a real dispatcher.
- Tests that require `Application.Current.Resources` use
  `[Collection(WpfCollection.Name)]` and the existing
  [WpfApplicationFixture](../LostieLauncher.Tests/Helpers/WpfApplicationFixture.cs).
  It creates the shared `Application` and configures resource assembly lookup.
  Theme resource dictionaries are an existing supported use; a blanket ban on
  all XAML loading would contradict these tests.
- Do not create another `Application` in an individual test.
- Async ViewModel tests inherit `ViewModelTestBase`, trigger the command,
  `await PumpAsync()`, then assert.

## Test shape

- Names follow `Method_Scenario_ExpectedResult`.
- Use readonly NSubstitute fields and a `CreateSut()` factory.
- Keep `// Arrange`, `// Act`, `// Assert` sections; comments explain intent.
- Assert with Shouldly, verify calls with `Received(1)` / `DidNotReceive()`.
- Use `[Theory]` / `[InlineData]` for parallel cases.

## Existing helpers

| Helper in `Helpers/` | Purpose |
| --- | --- |
| `FakeHttpMessageHandler` | HTTP responses and request inspection |
| `HttpClientFactoryStub` | named clients |
| `TestServiceProviderBuilder` | production-shaped container with overrides |
| `TempDirectoryFixture` | isolated filesystem tests |
| `PropertyChangedRecorder` | state notification assertions |
| `ViewModelTestBase` | deterministic synchronization context and `PumpAsync` |
| `WpfApplicationFixture` | shared Application and resource lookup |

`GlobalUsings.cs` already imports the test frameworks, helpers and common BCL
namespaces. Do not re-add unused imports: `IDE0005` is an error.

The test project treats warnings as errors and currently suppresses only
`xUnit1031` and `xUnit1051`. Do not add suppressions to make tests compile.
Run tests with the commands in [build-and-validation.md](build-and-validation.md).
