# Android downloads and game operations

Apply the [shared architecture rules](../../.agents/architecture.md).
Read [the runtime reference](../docs/game-runtime-options.md) for the currently
bound implementations and remaining gaps.

- Route lifecycle operations through `service/game/`. ViewModels must not
  register or remove library records themselves.
- Room owns download state; WorkManager schedules durable work; `DownloadTransfer`
  owns ranged network I/O. Keep the worker's decision logic in
  `DownloadWorkerRunner`, outside Android worker classes.
- A completed transfer hands its archive to `InstallDownloadedFileHandoff`.
  Completion alone is not evidence of a successfully installed game.
- Preserve hash validation, extraction containment, temporary-directory swap,
  failure outcomes and registration in both Kirin and the local library.
- Keep installed games in the injected shared root. Downloads and logs use
  their separate `StorageLocations` roots; these have different uninstall lifetimes.
- Preserve the permission gate before user-started installation/update.
  Automatic updates do not initiate a storage permission prompt.
- Never interpret unsupported running-state detection as `NOT_RUNNING` or an
  empty known session set. Do not introduce playtime estimates.
- `DownloadManager.purgeStale` must not purge on an empty catalogue: a failed
  request must not become data loss. Keep managed-file and inactive-row checks.
- Room schema changes need an exported schema and an appropriate migration.
  Durable download metadata and in-memory installation phases are different.
- Keep the foreground notification through verification and extraction.
  Its phase/content mapping is tested separately from the Android worker.
- Use the shared APK installer engine for Kirin and launcher APKs, with distinct
  target cache directories. Preserve package/version validation and the system
  installation flow. Game ZIP installation is a different operation.
