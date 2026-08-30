<!-- bmad:context -->
<!-- Verified 2026-08-30 against ab5857dd0f91107093885b4aa336fe6752e5a1dc. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

## munit-zio

Integration library between MUnit and ZIO. sbt crossProject (JVM/JS/Native), Scala 3/2.13/2.12. Public API docs live in `README.md`; MUnit concepts in https://scalameta.org/munit/docs/getting-started.html.

## Policy

- Never push to `master` — PRs only. Releases are manual: maintainer pushes a git version tag; `release.yml` publishes via sbt-ci-release.
- Never print or alter PGP/Sonatype secret values. Env var names follow sbt-ci-release docs (`PGP_SECRET`, `PGP_PASSPHRASE`, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`) — do not rename.

## Where things are

- Library source: `core/src/main/scala/munit/`
- Tests: `core/src/test/scala/munit/`
- MUnit documentation: https://scalameta.org/munit/docs/getting-started.html — read before changing `ZSuite`, assertions, or fixtures.

## Running and verifying

- Run the full cross-version matrix with `TEST=2.12 sbt ci-test` / `TEST=2.13 sbt ci-test` / `TEST=3 sbt ci-test` — bare `sbt test` covers only the default Scala version.
- `Test / parallelExecution := false` in `build.sbt` is deliberate (MUnit requirement) — do not remove.

## Conventions that differ from defaults

- All sources live in package `munit` (not a library-specific package) so `munit.ZSuite` sits next to MUnit's own imports — keep new files there.
<!-- /bmad:context -->
