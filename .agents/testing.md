# Shared testing rules

Use the platform testing document for its framework, fixtures and commands.

- Cover behavior changes and the failure path of a bug fix. Choose assertions
  about observable behavior rather than mirroring implementation details.
- Keep unit tests runnable in CI without a desktop session or Android device.
  Put platform interactions behind seams so the decisions remain testable.
- Use doubles for external services. A local HTTP test server is appropriate
  when testing the actual HTTP or serialization stack.
- Restrict filesystem tests to the platform's temporary-directory fixture.
  Do not use real registry writes, arbitrary external network access or sleeps.
- Inject and pin clocks and dispatchers. Set culture or locale explicitly when
  the expected behavior depends on it, and restore any process-wide change.
- Keep production visibility narrow: desktop exposes internals to its test
  project; Android tests can access internals in the app module.
- Keep test folders aligned with production areas and reuse existing helpers.
  Parameterize genuinely parallel cases rather than duplicating tests.
- An unsupported capability must remain distinguishable from an empty result,
  a stopped game or a successful operation.
