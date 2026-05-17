# fsrs-kt — Development TODO

Atomic, checkable tasks derived from `SPEC.md`. Work top-to-bottom; each
phase has a clear definition-of-done that gates the next.

Conventions:
- `§X.Y` refers to the spec.
- `[file:path]` is the file the task creates or edits.
- A task is "atomic" — one focused work session, ideally under an hour. If
  something feels bigger, split it.
- Definition-of-done at the end of each phase is the gate before moving on.
- VCS: `jj` colocated with `git`; default branch is `master`.

## Status (2026-05-17)

- **Phase 1 — Project skeleton:** ✅ complete. JVM build green on first run.
- **Next:** Phase 2 (public types).

### Decisions that diverged from the original spec defaults

- **Maven namespace:** `io.github.yalexaner` (was `<your-gh-username>`).
- **Branch name:** `master` (was `main`; project convention).
- **Toolchain bumped to latest stable:**
  - Kotlin 2.2.0 → **2.3.21**
  - kotlinx-datetime 0.6.2 → **0.8.0** (NB: `Instant` moved to
    `kotlin.time.Instant`; 0.7.1+ provides back-compat type aliases)
  - Gradle 8.10 → **9.3.0** (KGP 2.3.21's max fully-supported version;
    9.5.x works but emits deprecation warnings)
  - JDK 21 LTS → **25 LTS**
- **Dokka + vanniktech/maven-publish plugins:** deferred until Phase 9/10
  (not added in v1 of `libs.versions.toml` or root `build.gradle.kts`).
- **GitHub repo:** https://github.com/yalexaner/fsrs-kt — created and
  master pushed.

---

## Prerequisites (one-time, before any code)

- [x] JDK 25 installed locally (bumped from 21; `jdk25-openjdk` on Arch).
- [x] Git installed; `jj` colocated repo set up.
- [x] GitHub repo created: `yalexaner/fsrs-kt` (Phase 1 task; done early).
- [x] Maven namespace picked: `io.github.yalexaner` (see §6.1).
- [ ] Bookmark `https://github.com/open-spaced-repetition/py-fsrs/tree/main/fsrs`
      (canonical algorithm reference) and
      `https://github.com/open-spaced-repetition/py-fsrs/tree/main/tests`
      (test vectors we'll port).

---

## Phase 1 — Project skeleton ✅

Goal: empty Kotlin Multiplatform library project that builds.

- [x] Create the GitHub repo (private to start is fine) —
      https://github.com/yalexaner/fsrs-kt.
- [x] Local repo: `jj` + colocated `git`, default branch `master`.
- [x] Add `.gitignore` covering Kotlin/Gradle build output, IDE files,
      `local.properties`, KMP `kotlin-js-store/`, etc.
- [x] Add Gradle wrapper at **9.3.0** (was 8.10 in the original plan —
      bumped for Kotlin 2.3.21 compatibility; see Status block).
      `gradlew`, `gradlew.bat`, `gradle/wrapper/` committed.
- [x] `[file:gradle.properties]` — pasted from §5.5.
- [x] `[file:settings.gradle.kts]` — pasted from §5.3 (includes `:fsrs-kt`).
- [x] `[file:gradle/libs.versions.toml]` — Kotlin **2.3.21**,
      kotlinx-datetime **0.8.0**. Dokka/maven-publish deferred to
      Phase 9/10 (only the kotlin-multiplatform plugin is declared for now).
- [x] `[file:build.gradle.kts]` (root) — minimal, just:
      ```kotlin
      plugins {
          alias(libs.plugins.kotlin.multiplatform) apply false
      }
      ```
- [x] `[file:fsrs-kt/build.gradle.kts]` — JVM target only,
      `jvmToolchain(25)`, `explicitApi()`, `kotlinx-datetime` on
      `commonMain`, `kotlin("test")` on `commonTest`. No publishing yet.
- [x] Create source set directories:
      `fsrs-kt/src/commonMain/kotlin/io/github/yalexaner/fsrs/`,
      `fsrs-kt/src/commonTest/kotlin/io/github/yalexaner/fsrs/`.
- [x] Smoke file
      `[file:fsrs-kt/src/commonMain/kotlin/io/github/yalexaner/fsrs/Smoke.kt]`
      with `internal fun hello(): String = "fsrs-kt"`.
- [x] Smoke test in `commonTest` asserts `hello() == "fsrs-kt"`.
- [x] `./gradlew build` passes on local (BUILD SUCCESSFUL).
- [x] Initial commit (`chore: scaffold KMP library project`) pushed to
      origin/master.

**Definition of done:** clean clone of the repo runs `./gradlew build`
successfully on JDK 25 with no errors. **Met.**

---

## Phase 2 — Public types

Goal: the v1 public type surface compiles and validates inputs. No
algorithm code yet.

Spec reference: §3.3.

- [ ] `[file:.../fsrs/Rating.kt]` — `enum class Rating` per §3.3.
- [ ] `[file:.../fsrs/State.kt]` — `enum class State` per §3.3.
- [ ] `[file:.../fsrs/Card.kt]` — `data class Card` per §3.3.
- [ ] `[file:.../fsrs/ReviewLog.kt]` — `data class ReviewLog` per §3.3.
- [ ] `[file:.../fsrs/Parameters.kt]` — `data class Parameters` with
      `init {}` block validating: `w.size == 21`, `desiredRetention` in
      range, step arrays non-negative, `maximumIntervalDays >= 1`.
- [ ] `[file:.../fsrs/internal/DefaultWeights.kt]` — the 21-element
      `DoubleArray DEFAULT_W` from §2.3. Mark `internal`.
- [ ] `Parameters.Default` companion uses `DEFAULT_W.copyOf()` so callers
      can't mutate the shared array.
- [ ] Enable `explicitApi()` in `fsrs-kt/build.gradle.kts` under `kotlin`.
- [ ] Fix any `explicitApi()` warnings on the types above.
- [ ] Tests in `commonTest`:
  - [ ] `Rating.fromValue` and `State.fromValue` round-trip.
  - [ ] `Parameters` `init` rejects: 20 weights, 22 weights,
        `desiredRetention = 0`, `desiredRetention = 1`, negative step
        intervals, `maximumIntervalDays = 0`.
  - [ ] `Parameters.Default` constructs and is independent across instances
        (mutating one doesn't affect another).
  - [ ] `Card`, `ReviewLog` equality and `copy()` work as expected.

**Definition of done:** `./gradlew check` passes; public types are
`explicitApi`-clean.

---

## Phase 3 — Pure math (the formulas)

Goal: every FSRS-6 formula implemented as a pure function in
`internal/Formulas.kt`, tested in isolation against hand-computed values
and properties.

Spec reference: §2.

- [ ] `[file:.../internal/Formulas.kt]` — create the file. All functions
      `internal`. Constants: `S_MIN = 0.001`, `S_MAX = 36500.0`,
      `D_MIN = 1.0`, `D_MAX = 10.0`.

### Forgetting curve and interval

- [ ] `decay(w20: Double): Double` returns `-w20`.
- [ ] `factor(decay: Double): Double` returns `pow(0.9, 1.0 / decay) - 1.0`.
- [ ] `retrievability(t: Double, s: Double, w: DoubleArray): Double`
      implementing §2.4. Cache `factor` per call from `w[20]`.
- [ ] `intervalDays(s: Double, desiredRetention: Double, w: DoubleArray):
      Double` implementing §2.5.
- [ ] Tests:
  - [ ] `retrievability(s, s, w)` ≈ `0.9` for several `s` values and
        several `w[20]` values, tolerance `1e-9`.
  - [ ] `retrievability(0.0, s, w) == 1.0` (just-reviewed = 100%).
  - [ ] `retrievability` is monotonically decreasing in `t`.
  - [ ] `intervalDays(s, 0.9, w) ≈ s` (interval at default retention =
        stability).
  - [ ] `intervalDays` is monotonically increasing in `s`.

### Initial state

- [ ] `initialStability(g: Rating, w: DoubleArray): Double` clamped per
      §2.6.
- [ ] `initialDifficulty(g: Rating, w: DoubleArray): Double` clamped to
      `[1, 10]`.
- [ ] Tests for all four ratings against the default weights with values
      computed by hand or against py-fsrs.

### Stability update — success

- [ ] `nextStabilitySuccess(s: Double, d: Double, r: Double, g: Rating,
      w: DoubleArray): Double` per §2.7.
- [ ] Tests:
  - [ ] Returns `>= s` for any successful grade and any valid state
        (`SInc >= 1`).
  - [ ] Hard < Good < Easy when all other inputs equal.
  - [ ] Larger `d` → smaller gain (compare same inputs with `d=2` vs
        `d=8`).
  - [ ] Larger `s` → smaller gain (saturation, compare `s=10` vs
        `s=100`).
  - [ ] Smaller `r` → larger gain.
  - [ ] Clamped to `[S_MIN, S_MAX]`.

### Stability update — lapse

- [ ] `nextStabilityLapse(s: Double, d: Double, r: Double, w: DoubleArray):
      Double` per §2.8 including the `min(_, s)` clamp.
- [ ] Tests:
  - [ ] Returns `<= s` (lapse never increases stability).
  - [ ] Larger `d` → smaller `S_post_lapse` (harder cards drop further).
  - [ ] Clamped to `[S_MIN, S_MAX]`.

### Short-term stability

- [ ] `nextStabilityShortTerm(s: Double, g: Rating, w: DoubleArray):
      Double` per §2.9.
- [ ] Tests:
  - [ ] For `g >= Good`, `result >= s` (guarded floor).
  - [ ] For `g = Again`, result can be `< s`.
  - [ ] Larger `s` → smaller `|SInc - 1|` (`w19` saturation).

### Difficulty update

- [ ] `nextDifficulty(d: Double, g: Rating, w: DoubleArray): Double` per
      §2.10 — raw delta, linear damping, mean reversion, clamp.
- [ ] Tests:
  - [ ] Again increases `D`; Hard increases less; Good leaves it almost
        unchanged (mean reversion only); Easy decreases.
  - [ ] As `D → 10`, each update gets smaller (damping).
  - [ ] Pressing "Good" repeatedly converges to `D₀(Easy)`.
  - [ ] Clamped to `[1, 10]`.

**Definition of done:** all formula tests pass; coverage on
`internal/Formulas.kt` is ≥ 95%.

---

## Phase 4 — Fuzz

Goal: integer-day intervals are slightly randomized when enabled, but
deterministic given a seed.

Spec reference: §2.13.

- [ ] `[file:.../internal/Fuzz.kt]` — `internal fun fuzzInterval(
      intervalDays: Int, maximumIntervalDays: Int, random: Random):
      Int`. Use the same range table as `py-fsrs` (port the table
      verbatim).
- [ ] Helper to compute the symmetric range `[I_min, I_max]` around `I`.
- [ ] Tests:
  - [ ] With `Random(seed = 42)`, the output sequence is deterministic
        across runs.
  - [ ] Output always within `[I_min, I_max]`.
  - [ ] Output never exceeds `maximumIntervalDays`.
  - [ ] `I_min >= 1` for any positive input (no zero-day fuzz down).

**Definition of done:** fuzz tests are deterministic and pass on every
target.

---

## Phase 5 — State machine and step logic

Goal: Learning / Review / Relearning transitions and step advancement
match `py-fsrs`.

Spec reference: §2.11, §2.12.

- [ ] `[file:.../internal/Steps.kt]` — `internal` helpers:
  - [ ] `advanceLearning(card, rating, steps, params): NextStepResult`
        returning the new step index (or `null` = graduate).
  - [ ] `advanceRelearning(card, rating, steps, params): NextStepResult`.
  - [ ] `nextDueFromStep(now: Instant, stepSeconds: Long): Instant`.
- [ ] Tests:
  - [ ] New card in Learning with default `[1m, 10m]` advances:
        `Again` → step 0, `Hard` → step 0 (per py-fsrs behavior — verify
        the exact rule from py-fsrs source), `Good` → step+1, `Easy` →
        graduate immediately.
  - [ ] Graduating from final step transitions to Review with `(S₀, D₀)`
        set from §2.6.
  - [ ] `learning_steps = []` graduates immediately on first review.
  - [ ] `relearning_steps = []` keeps a lapsed card in Review with the
        post-lapse `S` from §2.8.
  - [ ] Edge case from py-fsrs: a Relearning card whose `step` is
        beyond the current `relearning_steps.size` — handle exactly as
        py-fsrs does.

**Definition of done:** step-advancement tests pass; the rules are a
literal port of py-fsrs, not Claude's reinterpretation.

> **Note:** if the py-fsrs source has subtle behavior for Hard mid-step,
> or for re-entering Learning after a reschedule, port those behaviors
> verbatim and add a test for each. Don't try to "improve" them.

---

## Phase 6 — Scheduler (public entry point)

Goal: glue the formulas, fuzz, and step logic into the public
`Scheduler.reviewCard()` API.

Spec reference: §3.3.

- [ ] `[file:.../fsrs/Scheduler.kt]` — class skeleton with the three
      public methods from §3.3 stubbed.
- [ ] Implement `reviewCard(card, rating, reviewDateTime, reviewDuration):
      Pair<Card, ReviewLog>`:
  - [ ] Compute elapsed days (`reviewDateTime - card.lastReview`); first
        review has elapsed = 0.
  - [ ] First-ever review on a card: initialize `S` and `D` per §2.6.
  - [ ] Learning state: delegate to `advanceLearning` (Phase 5). On
        graduation, set `state = Review`, `due = now + intervalDays`.
  - [ ] Review state: compute `R` from elapsed; dispatch to
        `nextStabilitySuccess` or `nextStabilityLapse`; update `D` via
        `nextDifficulty`; if `Again` and `relearningSteps` non-empty,
        transition to Relearning; otherwise compute next interval and
        new `due`.
  - [ ] Relearning state: delegate to `advanceRelearning`.
  - [ ] Same-day reviews use `nextStabilityShortTerm`; gate this on
        elapsed `< 1.0` days (verify exact gate from py-fsrs).
  - [ ] Apply fuzz to the integer interval if
        `parameters.enableFuzzing == true`.
  - [ ] Return new `Card` (immutable copy) and `ReviewLog`.
- [ ] Implement `getCardRetrievability(card, now): Double`:
  - [ ] Fresh card (no `lastReview`) returns `1.0`.
  - [ ] Otherwise call `retrievability(elapsedDays, card.stability, w)`.
- [ ] Implement `preview(card, now): Map<Rating, Card>`:
  - [ ] Run `reviewCard` for each of the four ratings against a copy of
        `card`; collect the resulting `Card`s by `Rating`.
  - [ ] Use a fixed seed RNG inside preview so previews are deterministic
        within a single call.
- [ ] End-to-end scenario tests in `commonTest`:
  - [ ] **Scenario A — happy path.** New card → Good (graduates) →
        review 7 days later with Good → review 14 days later with Good.
        Verify `state`, `stability`, `difficulty`, `due` at each step
        against py-fsrs.
  - [ ] **Scenario B — lapse.** New card → Good → review with Again →
        Relearning step → graduate → Good. Verify the lapse path.
  - [ ] **Scenario C — same-day cluster.** Multiple same-day reviews on
        a Review-state card; check short-term stability formula kicks in.
  - [ ] **Scenario D — fuzz off.** With `enableFuzzing = false`, two
        identical scenarios produce identical `Card.due`.
  - [ ] **Scenario E — custom parameters.** Pass a hand-picked weight
        vector different from defaults; verify outputs differ.

**Definition of done:** all scenarios pass; the scheduler is functionally
complete on JVM.

---

## Phase 7 — Multiplatform targets

Goal: the same `commonMain` code compiles and tests pass on every
declared target.

Spec reference: §4.3, §5.4.

- [ ] Update `fsrs-kt/build.gradle.kts` to declare:
  - [ ] `iosArm64()`, `iosX64()`, `iosSimulatorArm64()`.
  - [ ] `macosArm64()`, `macosX64()`.
  - [ ] `js(IR) { browser(); nodejs() }`.
  - [ ] `wasmJs { browser(); nodejs() }` (with the
        `ExperimentalWasmDsl` opt-in).
  - [ ] `linuxX64()`, `linuxArm64()`.
  - [ ] `mingwX64()`.
- [ ] `./gradlew build` passes on macOS (needed for Apple targets).
- [ ] `./gradlew jvmTest iosSimulatorArm64Test jsTest wasmJsTest
      linuxX64Test` (whichever subset is available on the dev machine).
- [ ] If any target fails — e.g. `kotlinx-datetime` doesn't yet support
      it — drop that target with a note in CHANGELOG / SPEC. Don't fight
      it.

**Definition of done:** all declared targets build and their tests pass.

> If `androidTarget()` is needed for the consumer app, add it here:
> apply the `com.android.library` plugin, set `android { namespace =
> "..."; compileSdk = 34; defaultConfig.minSdk = 24 }`, and add
> `publishLibraryVariants("release")` inside the `androidTarget {}`
> block.

---

## Phase 8 — Reference vector parity

Goal: the library matches `py-fsrs` v6.x output on its own test cases.

Spec reference: §7 (testing strategy).

- [ ] Identify the relevant `py-fsrs` test files at
      `https://github.com/open-spaced-repetition/py-fsrs/tree/main/tests`.
- [ ] For each scenario, either:
  - [ ] Hand-translate the inputs into a Kotlin test, *or*
  - [ ] Write a small Python script that runs the py-fsrs scenario and
        emits a JSON of expected outputs (card state at each step),
        then load that JSON as a test resource in `commonTest` and
        assert against it.
- [ ] Tolerance: `1e-9` on `stability` and `difficulty`; exact match on
      `due` after rounding to whole days; exact match on `state`,
      `step`, `cardId`.
- [ ] Document any intentional divergences (e.g. if we picked a slightly
      different fuzz table) in `CHANGELOG.md` and skip those reference
      tests with a comment pointing to the divergence note.

**Definition of done:** all ported py-fsrs reference vectors pass.

---

## Phase 9 — Documentation

Goal: every public surface has KDoc; the project has a usable README,
CHANGELOG, and LICENSE.

Spec reference: §8.

### KDoc

- [ ] Every `public` declaration in `commonMain` has a KDoc comment:
  - [ ] `Rating` enum + each case.
  - [ ] `State` enum + each case.
  - [ ] `Card` class + every property.
  - [ ] `ReviewLog` class + every property.
  - [ ] `Parameters` class + every property; link to §2 of the spec.
  - [ ] `Scheduler` class + every method, with `@param`, `@return`,
        `@throws` where relevant.
- [ ] No `@suppress` to bypass missing-KDoc warnings.

### Dokka

- [ ] Apply `dokka` plugin in `fsrs-kt/build.gradle.kts`.
- [ ] Configure module name and base URL.
- [ ] `./gradlew dokkaHtml` generates output to
      `fsrs-kt/build/dokka/html`.
- [ ] (Optional now, recommended) GitHub Pages workflow to publish Dokka
      on every push to `master`.

### Repo docs

- [ ] `[file:LICENSE]` — MIT license text with current year + author name.
- [ ] `[file:README.md]`:
  - [ ] One-paragraph intro.
  - [ ] Status badges: Maven Central version (after first release),
        CI status, license, supported Kotlin version.
  - [ ] Installation snippet.
  - [ ] 20-line quickstart (port py-fsrs's quickstart to Kotlin).
  - [ ] Custom-parameters snippet.
  - [ ] Compatibility table (targets, Kotlin version).
  - [ ] Link to Dokka site (once deployed).
  - [ ] Acknowledgments to Open Spaced Repetition.
  - [ ] License section.
- [ ] `[file:CHANGELOG.md]` — "Keep a Changelog" format; one
      `[Unreleased]` section to start with.
- [ ] `[file:CONTRIBUTING.md]` — brief: how to set up dev environment,
      run tests, file issues.

**Definition of done:** Dokka renders without warnings; README is
publish-ready.

---

## Phase 10 — CI and publishing setup

Goal: pushing a GitHub release publishes to Maven Central automatically.

Spec reference: §6.

### Maven Central account (one-time, manual)

- [ ] Sign in at `central.sonatype.com`.
- [ ] Add the namespace from prerequisites; verify it (GitHub repo with
      verification key as name, *or* DNS TXT for a domain).
- [ ] Wait for the green check on the namespace.

### PGP key (one-time, manual)

- [ ] Install GnuPG (`brew install gnupg` or distro equivalent).
- [ ] `gpg --full-generate-key` → ECC / Curve25519, no expiration, your
      identity, a strong passphrase.
- [ ] `gpg --list-keys` → record the long key ID.
- [ ] `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`.
- [ ] `gpg --armor --export-secret-keys <KEY_ID> > key.gpg`.
- [ ] **Move `key.gpg` somewhere safe** (password manager or encrypted
      vault). Never commit it.

### Maven Central user token (one-time, manual)

- [ ] Generate at `https://central.sonatype.com/usertoken`. Record the
      username and password (you can't recover them later).

### GitHub Secrets (one-time, manual)

- [ ] In repo Settings → Secrets and variables → Actions, add:
  - [ ] `MAVEN_CENTRAL_USERNAME` — from token.
  - [ ] `MAVEN_CENTRAL_PASSWORD` — from token.
  - [ ] `SIGNING_KEY_ID` — last 8 chars of the PGP key ID.
  - [ ] `SIGNING_PASSWORD` — PGP passphrase.
  - [ ] `GPG_KEY_CONTENTS` — full contents of `key.gpg`.

### Code changes

- [ ] Add the `vanniktech.maven.publish` plugin to
      `fsrs-kt/build.gradle.kts` and the `mavenPublishing { ... }` block
      from §5.4. Replace `<owner>`, `<github-id>`, `<namespace>` with
      real values.
- [ ] `[file:.github/workflows/ci.yml]` — three-OS matrix from §6.5.
- [ ] `[file:.github/workflows/publish.yml]` — release-triggered, runs
      `publishAndReleaseToMavenCentral` per §6.4.

### Local verification

- [ ] `./gradlew checkSigningConfiguration` reports no errors.
- [ ] `./gradlew checkPomFileForMavenPublication` reports no errors.
- [ ] `./gradlew publishToMavenLocal` succeeds; inspect `~/.m2` to
      verify all target artifacts are present.

**Definition of done:** publish workflow exists and runs green on a
test release (see Phase 11).

---

## Phase 11 — First release (0.1.0)

Goal: a real artifact lives on Maven Central.

- [ ] Set `version = "0.1.0"` in `fsrs-kt/build.gradle.kts`.
- [ ] Update `CHANGELOG.md` — move items out of `[Unreleased]` into a
      `[0.1.0]` section dated today.
- [ ] Commit the version bump and tag: `git tag v0.1.0 && git push --tags`.
- [ ] On GitHub, draft a new Release on tag `v0.1.0`, title `v0.1.0`,
      mark as pre-release (it's 0.x, not 1.0).
- [ ] Publish the release; watch the Actions tab — the `publish.yml`
      workflow runs.
- [ ] On success, check `https://central.sonatype.com/publishing/deployments`
      for the deployment.
- [ ] Wait 15–30 minutes; try `implementation("io.github.<ns>:fsrs-kt:0.1.0")`
      in a throwaway project; it should resolve.
- [ ] Add a Maven Central badge to README pointing at the new artifact.

**Definition of done:** a fresh user can run
`implementation("io.github.<ns>:fsrs-kt:0.1.0")` and call
`Scheduler().reviewCard(Card(...), Rating.Good)` against the published
binary.

---

## Post-1.0 ideas (do not start yet)

These live here so they're not forgotten. They are not on the v1.0
critical path.

- [ ] `:fsrs-kt-serialization` companion module with `@Serializable`
      types.
- [ ] `Duration`-based step constructors in `Parameters`.
- [ ] `Clock` and study-day-boundary helpers for apps that want a custom
      day boundary.
- [ ] `:fsrs-kt-optimizer` — see SPEC §10.
- [ ] Batch helpers (`cardsDueBy(now)`, etc.) — likely better as a
      separate "utilities" module so the core stays small.
- [ ] Promote `fsrs-kt` on `klibs.io` and in the Kotlin Slack `#feed`
      channel.

---

## Notes during development

- **Port, don't reinvent.** When in doubt, do what `py-fsrs` does.
  Differences are bugs in our code, not the reference.
- **Same-day reviews are subtle.** The boundary between "use the main
  formula" and "use the short-term formula" depends on state and
  elapsed time. Pin down the rule from py-fsrs early and add a test.
- **Floating-point comparisons in tests** always use a tolerance.
  Never `assertEquals(a, b)` on a `Double`.
- **Don't generate fuzz with `Random.Default`** anywhere in production
  code or tests — always go through the injected `Random` so behavior
  is reproducible.
- **`explicitApi()` warnings are not optional.** Treat them as errors
  before merging anything.
- **Resist adding dependencies.** The only one we want is
  `kotlinx-datetime`. Coroutines, serialization, logging — none of
  those belong in v1.
