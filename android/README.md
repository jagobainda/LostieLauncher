# LostieLauncher — Android

> **Not implemented yet.** This folder is the reserved place for the native
> Android application; it currently holds only the documentation placeholders
> that describe what belongs here.

The Android app will be a native **Kotlin / Jetpack Compose** client following
the same **MVVM** separation as the desktop launcher, reproducing its behavior
on a phone: game catalog, downloads, installed games, news and FAQs, the theme
set and the eight interface languages.

It is a port of the desktop launcher's **behavior**, not of its code. The
authority on what the app must do is the desktop side, under
[`../desktop/`](../desktop/); how Android does it is an Android decision.

## Status

| Piece                             | State                                    |
| --------------------------------- | ---------------------------------------- |
| Gradle project and app skeleton   | not created                              |
| Architecture guidelines           | outlined in [AGENTS.md](AGENTS.md)       |
| CI jobs                           | not added — see `../.github/workflows/ci.yml` |

## Working here

Once the project exists, its commands run **from this folder**, not from the
repository root — the same rule the desktop side follows. See
[the monorepo README](../README.md) for how the two sides sit together and
[AGENTS.md](AGENTS.md) for the conventions this side is expected to adopt.
