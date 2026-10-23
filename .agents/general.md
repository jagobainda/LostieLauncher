# General rules

- Write code, comments, documentation, commits, branch names and PR text in
  English. Translate user-facing content into each supported language.
- Keep changes within the requested scope. Normally work on one application;
  changes to both need a reason, such as a shared documentation reorganization.
- Follow [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution policy and
  acceptance. Use the root [README](../README.md) for product context and the
  contribution guide for platform setup and technical references.
  Report conflicting instructions rather than silently choosing a weaker rule.
- Do not change application versions or README version badges in a feature or
  fix. Releases belong to the maintainer. The applications have separate versions.
- Treat warnings as failures. Do not add suppressions, disable tests or widen
  visibility to make a change pass.
- Before handing off changes, run the affected platform's full CI gates from
  its folder using its `build-and-validation.md`. Report commands that could
  not run and their actual errors; do not claim success from an earlier run.
- Do not commit, push, open a PR or merge unless explicitly requested. Git
  actions follow [workflow.md](workflow.md).

The applications share product behavior, not an implementation. Desktop is the
reference for the behavior being ported; deliberate Android differences are
recorded in its [parity report](../android/docs/parity-report.md). Preserve
those decisions rather than copying Windows mechanisms into Android.
