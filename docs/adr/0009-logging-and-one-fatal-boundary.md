# 0009. Logging setup, and one fatal start-up boundary

- Status: Accepted
- Date: 2026-09-17 (recorded)

## Context
A failed start-up used to show a dialog and exit from inside a constructor. Logs from separate
runs mixed in one file, and tests wrote log files.

## Decision
- SLF4J with Logback.
  - Runtime: `logback.xml`. `Main` sets the run's timestamp property before anything touches
    SLF4J, so each run writes its own file under `logs/` (old ones are pruned on start-up) as well
    as to the console. The default level is `INFO`; per-tick detail is `DEBUG`.
  - Tests: `logback-test.xml`, at `WARN`, with no files. Logback prefers it when both are on the
    classpath.
  - For a deep dive, raise the root level or point `-Dlogback.configurationFile` at another
    config.
- `GameLoop` catches tick and render failures, logs them at `ERROR` and carries on. A circuit
  breaker stops the loop after a run of consecutive tick failures.
- Unloadable content throws `GameStartupException`, caught only in `Main`, which logs it and exits
  non-zero. The one other catch is the dev panel's wave-script box, whose input is typed by hand.

## Consequences
- A fatal problem has one place where it ends the program, and it names the cause.
- A per-tick failure doesn't kill the game silently, and a persistent one doesn't spam forever.

## Rejected
- *Dialogs from constructors*, or exits scattered over the code.
