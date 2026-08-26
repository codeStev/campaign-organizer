# 73. Scheduled backup snapshots (FR-42)

Date: 2026-08-26
Status: Accepted

## Context

FR-36 made the instance backup a one-click download, but "recent backup"
still depended on remembering to click. FR-42 asked for scheduled snapshots
of the same bundle. The original sketch proposed a sidecar container; with
the app already owning `BackupService.writeBackup(OutputStream)` and a
writable media volume, that is extra moving parts for nothing.

## Decision

**In-app scheduler instead of a sidecar.** A Spring `@Scheduled` job
(`ScheduledBackups`, in the context-agnostic `backup` package beside
`BackupService`, sharing its ArchitectureTest exemption) runs on a cron and:

1. streams `writeBackup` into `<app.media.dir>/backups/backup-<UTC stamp>.zip`
   — written to a `.part` temp file first, then atomically moved, so a crashed
   run never leaves a truncated ZIP that pruning would mistake for good;
2. prunes older snapshots, keeping the most recent N (`app.backup.keep`,
   default 7). Pruning matches only the job's own `backup-*.zip` names and
   only ever deletes from its own directory.

Configuration (env-overridable like everything else):

- `APP_BACKUP_CRON` — Spring cron `sec min hour dom mon dow`, default
  `0 37 3 * * *` (03:37 UTC: off-round minute to dodge shared-load spikes).
- `APP_BACKUP_KEEP` — default `7`.

Failure semantics: a failed snapshot is logged, not thrown — one broken night
must not stop the schedule. Nothing is pruned when the fresh write failed.

## Consequences

- Snapshots live inside the media volume, next to the data they preserve: one
  mount covers everything, and volume-level backups cover the snapshots too.
  The trade-off is that a lost volume loses the backups with it — acceptable
  for a personal single-user deployment whose threat model is "I fat-fingered
  a delete", not "the datacenter burned".
- The scheduler runs in-process: while the backend is down, no snapshot is
  taken (no catch-up). Fine at nightly cadence.
- Snapshots are plain FR-36 ZIPs, so restore = import an existing backup.
