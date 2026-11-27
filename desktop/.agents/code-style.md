# Code style

Apply the [shared architecture rules](../../.agents/architecture.md).

[`.editorconfig`](../.editorconfig) is authoritative and `dotnet format`
enforces it. Listed below are the parts an agent is likely to get wrong; for
anything not listed, match the surrounding code.

## Formatting

- File-scoped namespaces (`namespace LostieLauncher.Services;`).
- Allman braces — opening brace on its own line, including before `else`,
  `catch` and `finally`.
- 4 spaces, CRLF, UTF-8, trailing whitespace trimmed, final newline.
- `using` directives are **not** System-first and **not** group-separated. An
  unused one breaks the build (`IDE0005` is an error).

## The unusual rules

These two differ from common C# defaults, and reformatting them "correctly"
will fail CI:

- **Explicit accessibility modifiers everywhere, including interface members:**

  ```csharp
  public interface IContentService
  {
      public Task<List<GameInfo>> GetGamesAsync();   // yes, `public` on an interface member
  }
  ```

- **No braces around single-statement bodies** (`csharp_prefer_braces = false`):

  ```csharp
  if (version is null) return "unknown";
  if (e.PropertyName == nameof(SettingsViewModel.Language)) LoadFaqs();
  ```

  Keep it on one line. Do not add braces back.

## Language usage

- `var` everywhere; no `this.` qualification.
- Expression-bodied members wherever they fit (`=> …`) — methods, constructors,
  properties, accessors, operators.
- Nullable reference types are on. Model the null (`is null`, `?.`, pattern
  matching) instead of reaching for `!` to silence the compiler.
- Modern C# matching the surrounding code: collection expressions (`[]`,
  `[.. items]`), target-typed `new`, `switch` expressions, `record` for data,
  primary constructors on services, `sealed` on new classes not designed for
  inheritance.
- Guard constructor arguments with `ArgumentNullException.ThrowIfNull(x)` when
  the constructor is written out explicitly.

## Comments and docs

- C#, XAML, project files and scripts carry no comments or XML docs, as
  [general.md](../../.agents/general.md) requires. Name interfaces and seams so
  their purpose is clear, and put longer explanations in the documentation.
