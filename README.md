# fsrs-kt

[![CI](https://img.shields.io/github/actions/workflow/status/yalexaner/fsrs-kt/ci.yml?branch=master&style=flat-square&logo=githubactions&label=CI)](https://github.com/yalexaner/fsrs-kt/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.21-7F52FF.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![KMP targets](https://img.shields.io/badge/targets-JVM-blue.svg?style=flat-square)](#status)
[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg?style=flat-square)](LICENSE)

A pure Kotlin Multiplatform implementation of the **FSRS-6** spaced-repetition
scheduler. Scheduler only — no optimizer, no native dependencies, no
expect/actual. The same math runs anywhere Kotlin runs.

The closest existing ports — `ts-fsrs`, `py-fsrs`, `rs-fsrs`, `swift-fsrs`,
`android-fsrs` — are each tied to one language or platform. `fsrs-kt` fills
the pure-KMP gap so Compose Multiplatform apps (and anything else with a
`commonMain`) can depend on a single artifact.

## Status

**Pre-release. API is unstable.** Not yet published to Maven Central.

| Phase | State |
|---|---|
| 1 — Project skeleton | ✅ complete |
| 2 — Public types (`Card`, `Rating`, `State`, `ReviewLog`, `Parameters`) | in progress (feature branch) |
| 3+ — FSRS math, scheduler, custom parameters, fuzz, KMP target expansion, Dokka, Maven Central | planned |

Current build target: **JVM only**. Native, JS, Wasm, and Android targets
land in a later phase — see [`SPEC.md`](SPEC.md) §4.3.

## What this library will do (when v1.0 ships)

- Full FSRS-6 scheduling with default or user-supplied parameters (21 weights).
- Custom learning steps, relearning steps, desired retention, max interval,
  optional fuzz.
- Card state machine: Learning → Review → Relearning, with retrievability
  calculation.
- Deterministic given a fixed `Clock` and seeded `Random`.

Out of scope for v1.0: the parameter optimizer. See [`SPEC.md`](SPEC.md) §10
for the roadmap.

## Documentation

- [`SPEC.md`](SPEC.md) — the implementation specification.
- [`TODO.md`](TODO.md) — atomic tasks and phase status.

## Reference

The authoritative reference for FSRS-6 math is
[`py-fsrs`](https://github.com/open-spaced-repetition/py-fsrs). `fsrs-kt`
ports its test vectors and defers to upstream for any ambiguity.

## License

[MIT](LICENSE).
