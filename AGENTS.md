<!-- bmad:context -->
<!-- Verified 2026-11-15 against e855639df76c69eebd1cd08ac1f08dff7c7a3c24. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

## munit-zio

Integration library between MUnit and ZIO. sbt crossProject (JVM/JS/Native), Scala 3/2.13/2.12. Public API docs live in `README.md`.

## Policy

- Never push to `master` — PRs only. Releases are manual: maintainer pushes a git version tag; `release.yml` publishes via sbt-ci-release.
- Never print or alter PGP/Sonatype secret values. Env var names follow sbt-ci-release docs (`PGP_SECRET`, `PGP_PASSPHRASE`, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`) — do not rename.

## Where things are

- Library source: `core/src/main/scala/munit/`
- Tests: `core/src/test/scala/munit/`
- MUnit documentation: https://scalameta.org/munit/docs/getting-started.html — read before changing `ZSuite`, assertions, or fixtures.

## Running and verifying

- Run the full cross-version matrix with `sbt "++2.12.21" ci-test` / `sbt "++2.13.18" ci-test` / `sbt "++3.9.0" ci-test` — bare `sbt test` covers only the default Scala version. If these drift, take the versions from `scala212`/`scala213`/`scala3` in `build.sbt`.
- Run `sbt fmt` before committing — CI's `formatting` job runs `sbt fmtCheck`, which `ci-test` does not cover.
- `Test / parallelExecution := false` in `build.sbt` is deliberate (MUnit requirement) — do not remove.

## Conventions that differ from defaults

- All sources live in package `munit` (not a library-specific package) so `munit.ZSuite` sits next to MUnit's own imports — keep new files there.
<!-- /bmad:context -->
