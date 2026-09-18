# Lostie Launcher — product specification

The behavioural and visual contract of the launcher, extracted from the Windows
desktop application so the Android port does not have to re-read the WPF project
on every step.

This folder is **documentation only**. It contains no build files and belongs to
neither side of the monorepo: it sits at the root, like `.github/`, because both
sides read it. See [the root README](../README.md) for the repository as a whole.

## Precedence — read this before you trust anything here

1. **The WPF project is the authority.** Where this specification and
   `desktop/LostieLauncher/` disagree, the code wins and the drift is a bug in
   this folder worth fixing. Every section names the files it was extracted
   from so you can check.
2. **This specification describes *behaviour*, not *implementation*.** The
   Android app is a port of what the launcher does, not of how the WPF code does
   it. Where a section describes a mechanism (a `Dispatcher`, a
   `ResourceDictionary`, a `ProgressBar` template) it is explaining the
   behaviour that mechanism produces, not prescribing the Android equivalent.
3. **Section 10 is the exception list.** Anything that is intrinsically Windows
   is collected there rather than left implicit in the screens that use it.

Captured from commit `42511aa`, application version `0.9.1.0`, on 2026-09-18.

## Index

| # | Document | Covers | Read it before |
| --- | --- | --- | --- |
| 01 | [Overview](01-overview.md) | application lifecycle, shell, global state, navigation, shortcuts | steps 03, 09, 11 |
| 02 | [Screens](02-screens.md) | the five screens: state, actions, enablement, loading and empty states | steps 10, 14 |
| 03 | [Services](03-services.md) | service contracts, endpoints, HTTP clients, caching, network degradation | steps 04, 07, 08 |
| 04 | [Data model](04-data-model.md) | domain models, remote JSON shapes, local persistence files, settings | steps 04, 07 |
| 05 | [Localization](05-localization.md) | all 118 string keys in all 8 languages, FAQs, hot language switching | step 05 |
| 06 | [Design tokens](06-design-tokens.md) | the 10 themes with exact palettes, typography, spacing, radii, motion | steps 05, 12 |
| 07 | [Components](07-components.md) | anatomy and every state of each reusable card, plus the skeletons | steps 12, 14 |
| 08 | [Dialogs](08-dialogs.md) | the four dialogs and the message-box variants | step 13 |
| 09 | [Utilities](09-utilities.md) | the pure decision functions and the desktop test inventory | steps 06, 10 |
| 10 | [Windows-only](10-windows-only.md) | what has no direct Android translation, and why | steps 07, 09, 15 |

`samples/` holds the two live CDN payloads captured on 2026-09-18. They are
reference data for reading section 03 and 04; step 04 is expected to fetch its
own copies as test resources rather than depend on these.

## Counts, so a later step can check itself

| Thing | Count | Authority |
| --- | --- | --- |
| Screens | 5 | `desktop/LostieLauncher/Views/Partials/` |
| Dialogs | 4 window types | `desktop/LostieLauncher/Views/Dialogs/` |
| Reusable components | 4 cards + 3 skeletons | `desktop/LostieLauncher/Views/Components/` |
| ViewModels | 7 | `desktop/LostieLauncher/ViewModels/` |
| Interfaces declared in `Services/` | 10 | `desktop/LostieLauncher/Services/` |
| Of those, registered in the container | 9 (all but `IUpdatePackage`, which is returned by the gateway) | `desktop/LostieLauncher/Core/DependencyInjection.cs` |
| Service sections in section 03 | 7 (the three update interfaces share one) | [03-services.md](03-services.md) |
| Remote endpoints | 5 | `desktop/LostieLauncher/Core/DependencyInjection.cs` |
| Named HTTP clients | 3 | `desktop/LostieLauncher/Core/DependencyInjection.cs` |
| Languages | 8 | `desktop/LostieLauncher/Models/AppLanguage.cs` |
| Localized string keys | 118 (× 8 = 944 strings) | `desktop/LostieLauncher/Content/Strings.cs` |
| FAQ entries | 6 (× 8 = 48) | `desktop/LostieLauncher/Content/Faqs.cs` |
| Themes | 10 | `desktop/LostieLauncher/Themes/` |
| Theme resource keys | 28 per theme (14 colours + 14 brushes) | `desktop/LostieLauncher/Themes/` |
| Desktop unit tests | 528 | `desktop/LostieLauncher.Tests/` |

## Known drift between this specification and the port plan

The port plan (`.docs/Android.md`, step 05) says *"los dos idiomas"*. The
launcher has **eight** languages, not two, and has had eight since before the
monorepo restructuring. Step 05 must port all eight. Section 05 carries the
full catalogue so there is no ambiguity about which.
