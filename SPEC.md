# fsrs-kt — Specification

> Pure Kotlin Multiplatform implementation of the **FSRS-6** spaced repetition
> scheduler.

---

## 0. Document status

This document is the implementation spec for `fsrs-kt`, v1.0. It is normative
for our own implementation but **defers to the upstream reference** (`py-fsrs`,
v6.x) for any ambiguity in math. The intent is that anyone reading this should
be able to implement the library, and that anyone reading the library code
should be able to cross-check it against this document.

Scope of v1.0: **scheduler only**. The optimizer is explicitly out of scope and
covered briefly in §10 (Roadmap).

---

## 1. Project overview

### 1.1 What we're building

A pure-Kotlin port of the FSRS-6 scheduler, distributed as a Kotlin
Multiplatform library and published to Maven Central. Consumers — including a
Compose Multiplatform flashcard app — get a single dependency that runs the
same math on JVM, Android, iOS, macOS, JS, Wasm, Linux, and Windows.

### 1.2 Why a new library

Looking at the official `awesome-fsrs` list of ports, there is no pure-KMP
implementation. The closest options are:

- `android-fsrs` — Android-only, not multiplatform.
- `ts-fsrs`, `py-fsrs`, `rs-fsrs`, `swift-fsrs`, etc. — one language each,
  none usable from KMP `commonMain`.
- `fsrs-rs` — Rust crate with optimizer; can be FFI'd but adds toolchain
  weight on both Android and iOS.

`fsrs-kt` slots into the same niche as `ts-fsrs` / `rs-fsrs`: scheduler-only,
no native dependencies, runs anywhere Kotlin runs.

### 1.3 Scope and non-goals

**In scope for v1.0:**

- The full FSRS-6 scheduling algorithm with default parameters.
- Custom-parameter support (user supplies their 21 weights).
- Custom learning steps, relearning steps, desired retention, maximum
  interval, and optional fuzz.
- Card state machine: Learning → Review → Relearning.
- Retrievability calculation.
- Serialization-friendly value types.

**Out of scope for v1.0:**

- Optimizer / parameter training. (Future module — §10.)
- Anki-format import/export.
- Persistence (the consumer chooses their own DB; values are simple data
  classes).
- Workload simulation, analytics, scheduling-strategy variants.

### 1.4 Reference and licensing

- **Algorithm reference:** `open-spaced-repetition/py-fsrs` v6.3.x. We
  port from that source where formulas are ambiguous, and we reuse its public
  test vectors.
- **License:** MIT (matching the broader OSR ecosystem).
- **Naming:** `fsrs-kt`, following the `[lang]-fsrs` convention used by
  `ts-fsrs`, `go-fsrs`, `rs-fsrs`, etc. — signalling "scheduler-only port,"
  consistent with OSR's own guidance.

---

## 2. Algorithm: FSRS-6

This section is a self-contained reference for the math. Notation matches the
reference implementations: `w` is the 21-element parameter array (zero-indexed),
`G` is the user's grade (1=Again, 2=Hard, 3=Good, 4=Easy), `S` is stability in
days, `D` is difficulty (clamped to `[1, 10]`), `R` is retrievability (in
`[0, 1]`), `t` is the elapsed time in days since the last review.

### 2.1 Memory model (DSR)

FSRS models each card with three variables:

- **Difficulty `D`** — how inherently hard the card is (1 = easy, 10 = hard).
  Heuristic; doesn't depend on `R`. Mean-reverts toward an Easy default.
- **Stability `S`** — the number of days for retrievability to decay from
  100% to 90% under the current memory state. The core scheduling quantity.
- **Retrievability `R`** — probability that the user can recall the card
  *right now*, given `S` and `t`.

`S` and `R` are linked by the forgetting curve (§2.4). `D` modulates how
quickly `S` grows in response to successful reviews.

### 2.2 The 21 parameters

FSRS-6 has **21 trainable parameters** `w[0]..w[20]`. The user can either use
the defaults (trained on ~700M reviews from ~10k Anki users) or supply
custom-trained weights. Each weight has a physical role; the optimizer clamps
each to a valid range during training.

| Index | Role |
|-------|------|
| `w0..w3` | Initial stability `S₀(G)` per first grade — `w0`=Again, `w1`=Hard, `w2`=Good, `w3`=Easy. |
| `w4` | Base for initial difficulty `D₀(G)`. Acts as the mean-reversion target via `D₀(Easy)`. |
| `w5` | Exponent for initial difficulty growth across grades. |
| `w6` | Difficulty change scale per rating. |
| `w7` | Mean-reversion strength toward `D₀(Easy)` after a review. |
| `w8` | Overall scale of stability gain on success (used as `e^w8`). |
| `w9` | Saturation exponent: larger `S` ⇒ smaller stability gain. |
| `w10` | Retrievability boost: lower `R` at review ⇒ larger stability gain. |
| `w11` | Post-lapse stability scale. |
| `w12` | Difficulty exponent in the post-lapse stability formula. |
| `w13` | Stability exponent in the post-lapse stability formula. |
| `w14` | Retrievability term in the post-lapse stability formula. |
| `w15` | Stability-gain multiplier when grade is **Hard** (`<1`, so Hard reduces gain). |
| `w16` | Stability-gain multiplier when grade is **Easy** (`>1`, so Easy boosts gain). |
| `w17`, `w18` | Same-day (short-term) stability update controls. |
| `w19` | Same-day stability saturation (FSRS-6, new vs FSRS-5). |
| `w20` | Personalized forgetting-curve decay (FSRS-6, new). Typically `0.1..0.8`; clamped. |

### 2.3 Default parameters

The FSRS-6 defaults (from `py-fsrs` v6.x):

```text
w = [
    0.212,    // w0  initial S(Again)
    1.2931,   // w1  initial S(Hard)
    2.3065,   // w2  initial S(Good)
    8.2956,   // w3  initial S(Easy)
    6.4133,   // w4  D₀ base
    0.8334,   // w5  D₀ exponent
    3.0194,   // w6  ΔD per rating
    0.001,    // w7  mean-reversion
    1.8722,   // w8  stability-gain scale
    0.1666,   // w9  S saturation
    0.796,    // w10 retrievability boost
    1.4835,   // w11 post-lapse scale
    0.0614,   // w12 post-lapse D exponent
    0.2629,   // w13 post-lapse S exponent
    1.6483,   // w14 post-lapse R term
    0.6014,   // w15 Hard penalty
    1.8729,   // w16 Easy bonus
    0.5425,   // w17 short-term scale
    0.0912,   // w18 short-term bias
    0.0658,   // w19 short-term saturation
    0.1542,   // w20 forgetting-curve decay
]
```

These are 64-bit doubles in the spec; the implementation uses `DoubleArray`.

### 2.4 Forgetting curve

The probability that a card is recalled `t` days after the last review,
given its current stability `S`:

```text
DECAY  = -w20
FACTOR = 0.9^(1/DECAY) - 1            // (constant for a given w20)

R(t, S) = (1 + FACTOR * t / S)^DECAY
```

By construction, `R(S, S) = 0.9`: at `t = S`, retrievability has dropped from
100% to 90% — the definition of stability.

Note that `FACTOR` depends on `w20`; pre-FSRS-6 it was the constant `19/81`
(corresponding to `DECAY = -0.5`). Cache `FACTOR` per `Parameters` instance.

### 2.5 Interval calculation

Given current stability and a desired retention rate `DR ∈ (0, 1)`, the next
review interval (in days, before fuzz) is the `t` that drives `R` to `DR`:

```text
I(S, DR) = S / FACTOR * (DR^(1/DECAY) - 1)
```

When `DR = 0.9`, this simplifies to `I = S`. Clamp `I` to `[1,
maximum_interval]` and round to whole days. Apply fuzz if enabled (§2.13).

### 2.6 Initial state (after the first review)

```text
S₀(G) = clamp(w[G-1], 0.1, S_MAX)             // S_MAX = 36500 days
D₀(G) = clamp( w4 - exp(w5 * (G - 1)) + 1, 1.0, 10.0 )
```

So the first review of a card never moves through the lapse formula or the
short-term formula — it goes straight to `(S₀(G), D₀(G))`.

### 2.7 Stability update — successful review

For `G ∈ {Hard, Good, Easy}` and elapsed time `t ≥ 1` day (i.e. a real
inter-day review, not same-day; see §2.8):

```text
R = R(t, S_prev)

hard_penalty = (G == Hard) ? w15 : 1
easy_bonus   = (G == Easy) ? w16 : 1

SInc = exp(w8)
     * (11 - D)
     * S_prev^(-w9)
     * (exp((1 - R) * w10) - 1)
     * hard_penalty
     * easy_bonus
     + 1

S_new = S_prev * SInc
```

Properties guaranteed by the formula:

- `SInc ≥ 1` always, so a successful review can never decrease `S`.
- Higher `D` → smaller gain.
- Higher `S_prev` → smaller gain (saturation).
- Lower `R` at review time → larger gain (review-when-almost-forgotten).

Then `clamp(S_new, S_MIN, S_MAX)` where `S_MIN = 0.001`, `S_MAX = 36500`.

### 2.8 Stability update — lapse (Again)

For `G = Again` after any inter-day review:

```text
S_post_lapse = w11 * D^(-w12) * ((S_prev + 1)^w13 - 1) * exp((1 - R) * w14)

S_new = min(S_post_lapse, S_prev)
```

The `min(…, S_prev)` clamp guarantees that a lapse cannot increase stability.
Then clamp to `[S_MIN, S_MAX]` as above.

### 2.9 Short-term stability (same-day review)

If `t < 1` day (same calendar day in user's timezone — see §2.12 for the
Learning state mechanics), FSRS-6 uses a separate heuristic, not the main
formula:

```text
SInc = exp(w17 * (G - 3 + w18)) * S_prev^(-w19)

S_candidate = S_prev * SInc

// Guard: Good/Easy must not decrease S on same-day reviews
S_new = (G >= Good) ? max(S_candidate, S_prev) : S_candidate
```

Notes:

- For `G = Good` (`G - 3 = 0`), the exponent reduces to `w17 * w18` — a
  small constant.
- `w19` is the FSRS-6 addition that saturates same-day gains for large `S`.
- For same-day **Again**, this formula applies — not the inter-day lapse
  formula in §2.8.

> **Implementation note:** the boundary condition for "same-day" depends on
> the card's `state` and the scheduler's learning/relearning steps. Use
> `py-fsrs` as the canonical reference and port its behavior exactly.

### 2.10 Difficulty update

In three steps:

```text
// 1. Raw delta from grade
ΔD     = -w6 * (G - 3)

// 2. Linear damping (slows updates as D approaches 10)
ΔD_damped = ΔD * (10 - D) / 9

// 3. Tentative new D
D_tmp = D + ΔD_damped

// 4. Mean reversion toward D₀(Easy) (= D₀ for G=4)
D_target = clamp( w4 - exp(w5 * 3) + 1, 1.0, 10.0 )   // = D₀(Easy)
D_new = w7 * D_target + (1 - w7) * D_tmp

// 5. Clamp
D_new = clamp(D_new, 1.0, 10.0)
```

Behavior the formula encodes:

- Again → adds a lot to `D`. Hard → adds a little. Good → no change (before
  reversion). Easy → subtracts a little.
- As `D` approaches 10, each update gets smaller (linear damping).
- Over many "Good"s, `D` slowly converges to `D₀(Easy)` via mean reversion.

### 2.11 Card states

A card is in exactly one of three states. The state determines how a rating
is interpreted.

```text
enum State { Learning, Review, Relearning }
```

- **Learning** — a new card progressing through `learning_steps`. Default
  steps are `[1 minute, 10 minutes]`. Each correct grade advances by one
  step; Again resets to step 0. Once the final step is graduated (Good/Easy),
  the card transitions to **Review** with `(S₀, D₀)` initialized from §2.6.
  If the scheduler is configured with `learning_steps = []`, the card
  graduates to Review immediately on its first review.
- **Review** — a graduated card scheduled by the main FSRS math (§2.4–2.10).
  If the user rates it **Again**, it transitions to **Relearning** (provided
  `relearning_steps` is non-empty; otherwise it stays in Review with the
  post-lapse `S` from §2.8).
- **Relearning** — a lapsed card progressing through `relearning_steps`.
  Default is `[10 minutes]`. Behaves like Learning. Graduating returns it to
  Review.

### 2.12 Learning / relearning step intervals

While in Learning or Relearning, the next due time is the time to the next
step, not an FSRS-derived interval. Step intervals are typically minutes (so
the card is shown again the same study session).

Exact step-advancement rules — including how Hard interacts with steps, the
edge case where a card has more `step` than the current `learning_steps`
array, and how Easy on the last step grants a multi-day jump — match
`py-fsrs`. Port literally.

### 2.13 Fuzz

When `enable_fuzzing = true`, the integer-day interval `I` is randomized
slightly so that batches of cards reviewed on the same day don't all come due
on the same future day. Algorithm:

1. Compute the raw interval `I` in days.
2. Pick a small symmetric range `[I_min, I_max]` around `I` whose width grows
   with `I` (e.g. ±5% for short intervals, narrower for very long ones).
3. Sample a uniform integer in `[I_min, I_max]`.

Use the same range table as `py-fsrs` for compatibility. Fuzz must be
deterministic-on-demand for tests (accept an injectable RNG).

---

## 3. Public API

### 3.1 Design principles

1. **Stable types first.** Per the discussion in the prior chat, the public
   types are the contract — they must not break when the optimizer module is
   added later. Types that an optimizer will need to read/write are pulled
   forward into the v1 API even though v1 only consumes them.
2. **Pure functions.** `Scheduler.reviewCard(card, rating)` returns a new
   `Card` and a `ReviewLog` — it never mutates. `Card` is a `data class`.
3. **No platform leakage.** All public types live in `commonMain` with no
   `expect`/`actual`.
4. **No surprise dependencies.** The only library dependency is
   `kotlinx-datetime` for timestamps.

### 3.2 Package layout

```text
io.github.<namespace>.fsrs
├── Card           // public data class
├── Rating         // public enum
├── State          // public enum
├── ReviewLog      // public data class
├── Parameters     // public data class (the 21 weights + bounds)
├── Scheduler      // public class — the entry point
└── internal/      // package-private implementation
    ├── Formulas.kt    // pure math (testable in isolation)
    ├── Fuzz.kt        // interval fuzzing
    └── Steps.kt       // learning/relearning step logic
```

### 3.3 Types

```kotlin
public enum class Rating(public val value: Int) {
    Again(1), Hard(2), Good(3), Easy(4);

    public companion object {
        public fun fromValue(value: Int): Rating = entries.first { it.value == value }
    }
}

public enum class State(public val value: Int) {
    Learning(1), Review(2), Relearning(3);

    public companion object {
        public fun fromValue(value: Int): State = entries.first { it.value == value }
    }
}

public data class Card(
    public val cardId: Long,                  // caller-assigned identifier
    public val state: State = State.Learning,
    public val step: Int? = 0,                // current learning/relearning step, null in Review
    public val stability: Double? = null,     // null until first review
    public val difficulty: Double? = null,    // null until first review
    public val due: Instant,                  // when this card next becomes due
    public val lastReview: Instant? = null,
)

public data class ReviewLog(
    public val cardId: Long,
    public val rating: Rating,
    public val reviewDateTime: Instant,
    public val reviewDuration: Long? = null,  // milliseconds; null if unspecified
)

public data class Parameters(
    public val w: DoubleArray,                // size 21
    public val desiredRetention: Double = 0.9,
    public val learningStepsSeconds: LongArray = longArrayOf(60L, 600L),
    public val relearningStepsSeconds: LongArray = longArrayOf(600L),
    public val maximumIntervalDays: Int = 36_500,
    public val enableFuzzing: Boolean = true,
) {
    init {
        require(w.size == 21) { "FSRS-6 requires exactly 21 parameters" }
        require(desiredRetention in 0.01..0.999)
        // additional per-w range checks here
    }

    public companion object {
        public val Default: Parameters = Parameters(w = DEFAULT_W.copyOf())
    }
}

public class Scheduler(
    public val parameters: Parameters = Parameters.Default,
    private val clock: Clock = Clock.System,
    private val random: Random = Random.Default,
) {
    public fun reviewCard(
        card: Card,
        rating: Rating,
        reviewDateTime: Instant = clock.now(),
        reviewDuration: Long? = null,
    ): Pair<Card, ReviewLog>

    public fun getCardRetrievability(
        card: Card,
        now: Instant = clock.now(),
    ): Double

    /** Compute (interval-days, stability, difficulty) for each rating without committing. */
    public fun preview(card: Card, now: Instant = clock.now()): Map<Rating, Card>
}
```

Rationale:

- `Card.due` is an `Instant`, not a date or a day-count — sub-day precision
  matters for the Learning/Relearning steps.
- `stability` and `difficulty` are nullable to make a fresh card
  representable without sentinel values like `-1`. The first call to
  `reviewCard` initializes them via §2.6.
- `Parameters` holds steps as `LongArray` of seconds, not
  `kotlin.time.Duration`. Reasons: serialization-friendliness, no need to
  import `Duration` from `commonMain`, and seconds are plenty of resolution
  for SRS step intervals. (If needed we expose convenience constructors
  taking `Duration`.)
- `Scheduler` takes injectable `Clock` and `Random` — essential for
  deterministic tests and useful for fixed-time simulations.
- `preview` mirrors `ts-fsrs`'s "all four button preview" pattern, useful in
  UIs that want to label buttons with "in 1d / 3d / 7d / 14d" up front.

### 3.4 Time handling

- All timestamps are `kotlinx.datetime.Instant` and represent **UTC**
  internally. Matches `py-fsrs`'s "UTC only" rule.
- Day boundaries (for the same-day vs inter-day distinction in §2.7–2.9) are
  computed in UTC from the elapsed time between `card.lastReview` and the
  review time. The consumer's preferred "study day boundary" (e.g.
  `4 AM local`) is **out of scope for the scheduler**; the app layer can
  shift timestamps before passing them in if it cares.

### 3.5 Serialization

The data types are designed to be JSON-friendly (all-primitive fields,
deterministic constructors), but the library does **not** ship a
serialization module. Consumers add `kotlinx-serialization` or whatever they
prefer and serialize the types themselves. This keeps `fsrs-kt` zero-dep
besides `kotlinx-datetime`.

(Future consideration: an optional `:fsrs-kt-serialization` sibling module
with `@Serializable` annotations, if there's demand.)

### 3.6 Public API stability

- All types and functions documented above are part of the **public API**
  and follow SemVer from v1.0.0.
- The `internal/` package is non-public and may change at any time.
- Pre-1.0 versions (`0.x`) may break the public API across minor versions;
  this is normal for KMP libraries finding their feet.


---

## 4. Library structure

### 4.1 Module layout

**v1.0 is a single Gradle module.** Per the prior chat: don't over-engineer
day one. The structural pressure to split scheduler from optimizer that
existed in Rust (tensor types vs plain slice types) doesn't exist in Kotlin
— both halves would operate on `DoubleArray`.

```text
fsrs-kt/                           # root project
├── settings.gradle.kts
├── build.gradle.kts               # root build script (mostly empty)
├── gradle/
│   └── libs.versions.toml         # version catalog
├── gradle.properties
├── fsrs-kt/                       # the single library module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/...
│       ├── commonTest/kotlin/...
│       ├── jvmTest/kotlin/...     # only if needed
│       └── iosTest/kotlin/...     # only if needed
├── .github/workflows/
│   ├── ci.yml                     # build + test on every push
│   └── publish.yml                # publish on GitHub release
├── README.md
├── CHANGELOG.md
├── LICENSE
└── CONTRIBUTING.md
```

When (and only when) we add an optimizer in v2+, the layout becomes:

```text
fsrs-kt/
├── fsrs-kt-scheduler/             # was just "fsrs-kt"
└── fsrs-kt-optimizer/             # new; depends on :fsrs-kt-scheduler
```

This is a mechanical refactor — move files, update `build.gradle.kts`,
re-publish under new artifact IDs. Not a redesign.

### 4.2 Source set hierarchy

We don't need a custom hierarchy. The default template that ships with the
Kotlin Gradle plugin gives us, for our target set, exactly the structure we
want:

```text
commonMain
├── jvmMain
│   └── (Android consumers use the JVM artifact transparently)
├── jsMain
├── wasmJsMain
└── nativeMain
    ├── appleMain
    │   ├── iosMain
    │   │   ├── iosArm64Main
    │   │   ├── iosX64Main
    │   │   └── iosSimulatorArm64Main
    │   └── macosMain
    │       ├── macosX64Main
    │       └── macosArm64Main
    ├── linuxMain
    │   ├── linuxX64Main
    │   └── linuxArm64Main
    └── mingwMain
        └── mingwX64Main
```

**All FSRS code lives in `commonMain`.** No `expect`/`actual`. No
platform-specific source sets needed. Tests run in `commonTest`.

### 4.3 KMP targets

Recommended target set for v1.0:

| Target | Reason |
|--------|--------|
| `jvm()` | Backbone target; Android consumers can use this artifact directly. |
| `androidTarget()` | Optional: gives Android consumers an artifact with proper variant metadata. Adds an AGP dependency to the build. Consider deferring until first Android consumer asks. |
| `iosArm64()`, `iosX64()`, `iosSimulatorArm64()` | iOS — required for our own KMP app. |
| `macosArm64()`, `macosX64()` | Trivial to add; macOS Compose Multiplatform apps. |
| `js(IR) { browser(); nodejs() }` | Browser/Node consumers. |
| `wasmJs { browser(); nodejs() }` | Modern Wasm target. |
| `linuxX64()`, `linuxArm64()` | Trivial to add for pure-Kotlin. |
| `mingwX64()` | Windows native. |

The "everything" target set is cheap to maintain for a pure-Kotlin library
with no platform APIs. If any target proves annoying (e.g. due to
`kotlinx-datetime` not yet supporting it), drop it without ceremony — it
costs nothing on the consumer side.

**Pragmatic minimum** if we want to ship fast: `jvm()`, the three `ios*`,
and `wasmJs()`. Everything else can be added in a one-line diff later.

### 4.4 Dependencies

- **Runtime:** `org.jetbrains.kotlinx:kotlinx-datetime:<latest>` only.
- **Test:** `kotlin-test` and `kotlin-test-annotations-common`.

No coroutines, no serialization, no logging library. The scheduler is
synchronous pure math.

---

## 5. Build setup

### 5.1 Toolchain

| Component | Version (as of this spec) |
|-----------|---------------------------|
| Kotlin | 2.2.x or newer |
| Gradle | 8.10+ |
| JDK (for the build) | 21 (LTS) — toolchain auto-provisioning |
| Android Gradle Plugin | required only if `androidTarget()` is enabled |
| `kotlinx-datetime` | latest stable |
| `vanniktech/gradle-maven-publish-plugin` | 0.36.0+ |
| Dokka | v2 (the v1 plugin is being removed) |

### 5.2 Version catalog (`gradle/libs.versions.toml`)

```toml
[versions]
kotlin = "2.2.0"
kotlinx-datetime = "0.6.2"
android-gradle = "8.7.0"        # only if androidTarget is enabled
maven-publish = "0.36.0"
dokka = "2.0.0"

[libraries]
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "android-gradle" }
maven-publish = { id = "com.vanniktech.maven.publish", version.ref = "maven-publish" }
dokka = { id = "org.jetbrains.dokka", version.ref = "dokka" }
```

Pin exact versions; the project should not silently float.

### 5.3 Root `settings.gradle.kts`

```kotlin
rootProject.name = "fsrs-kt"
include(":fsrs-kt")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

### 5.4 Module `fsrs-kt/build.gradle.kts`

```kotlin
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    // alias(libs.plugins.android.library)  // only if androidTarget enabled
}

group = "io.github.<namespace>"
version = "0.1.0"   // semver; bump before release

kotlin {
    explicitApi()                              // require visibility modifiers on public API
    jvmToolchain(21)

    jvm()
    // androidTarget { publishLibraryVariants("release") }
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    macosArm64()
    macosX64()
    js(IR) { browser(); nodejs() }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser(); nodejs() }
    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(group.toString(), "fsrs-kt", version.toString())

    pom {
        name.set("fsrs-kt")
        description.set("Pure Kotlin Multiplatform implementation of the FSRS-6 spaced repetition scheduler.")
        inceptionYear.set("2026")
        url.set("https://github.com/<owner>/fsrs-kt/")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("<github-id>")
                name.set("<your name>")
                url.set("https://github.com/<owner>")
            }
        }
        scm {
            url.set("https://github.com/<owner>/fsrs-kt/")
            connection.set("scm:git:git://github.com/<owner>/fsrs-kt.git")
            developerConnection.set("scm:git:ssh://git@github.com/<owner>/fsrs-kt.git")
        }
    }
}
```

Key choices:

- `explicitApi()` — every public declaration must have `public` written out,
  or get a `private`/`internal`. Catches accidental API exposure at compile
  time. Mandatory for a library that promises SemVer.
- `jvmToolchain(21)` — Gradle auto-provisions JDK 21 if missing.
- `SonatypeHost.CENTRAL_PORTAL` — Maven Central's new portal (the old OSSRH
  Nexus is being sunset). The vanniktech plugin handles both, but new
  accounts (post-March 2024) must use the portal.

### 5.5 `gradle.properties`

```properties
# Performance
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# Kotlin
kotlin.code.style=official
kotlin.mpp.stability.nowarn=true

# Disable configuration cache only for the publish task (handled in CI workflow)
```

Note: the vanniktech plugin currently does not support Gradle configuration
cache for the publish task itself — pass `--no-configuration-cache` on the
publish command line (we do this in the GitHub Action).

---

## 6. Publishing

### 6.1 Pick a namespace

The Maven coordinate has the form `groupId:artifactId:version`. Two
realistic namespace options:

1. **GitHub-style** — `io.github.<your-gh-username>`. No domain required.
   Verify by creating a public repo named with the verification key Maven
   Central provides. **Recommended for solo projects.**
2. **Owned-domain** — `com.<yourdomain>`. Verify via DNS TXT record.

Artifact ID: `fsrs-kt`. So a release is e.g.
`io.github.<user>:fsrs-kt:1.0.0`.

### 6.2 One-time setup checklist

1. **Maven Central account** — sign in at `central.sonatype.com`.
2. **Verify namespace** — see §6.1. Wait for green check.
3. **Generate a PGP key** — `gpg --full-generate-key`. Use ECC / Curve25519
   when prompted (modern default). Save passphrase securely.
4. **Upload public key** to `keyserver.ubuntu.com`:
   `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`.
5. **Export private key** as ASCII armor for CI:
   `gpg --armor --export-secret-keys <KEY_ID> > key.gpg`.
6. **Generate a Maven Central user token** at `central.sonatype.com/usertoken`.
7. **Store as GitHub Secrets** (Settings → Secrets and variables → Actions):
   - `MAVEN_CENTRAL_USERNAME` — token user
   - `MAVEN_CENTRAL_PASSWORD` — token password
   - `SIGNING_KEY_ID` — last 8 chars of the PGP key ID
   - `SIGNING_PASSWORD` — PGP passphrase
   - `GPG_KEY_CONTENTS` — entire contents of `key.gpg`

### 6.3 Local verification

Before the first real publish, sanity-check locally:

```bash
./gradlew checkSigningConfiguration          # verifies key is on a keyserver
./gradlew checkPomFileForMavenPublication    # verifies the POM
./gradlew publishToMavenLocal                # writes to ~/.m2 for manual inspection
```

### 6.4 GitHub Actions release workflow

```yaml
# .github/workflows/publish.yml
name: Publish
on:
  release:
    types: [released, prereleased]

jobs:
  publish:
    name: Release build and publish
    runs-on: macos-latest                    # macOS required to build Apple targets
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: 21

      - uses: gradle/actions/setup-gradle@v3

      - name: Publish to Maven Central
        run: ./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
        env:
          ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
          ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
          ORG_GRADLE_PROJECT_signingInMemoryKeyId: ${{ secrets.SIGNING_KEY_ID }}
          ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.SIGNING_PASSWORD }}
          ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.GPG_KEY_CONTENTS }}
```

`publishAndReleaseToMavenCentral` uploads **and** releases without manual
approval. Use `publishToMavenCentral` instead if you want a manual sanity
check on the Maven Central UI before each release.

### 6.5 CI workflow (build + test)

```yaml
# .github/workflows/ci.yml
name: CI
on:
  push: { branches: [main] }
  pull_request: { branches: [main] }

jobs:
  test:
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'zulu', java-version: 21 }
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew build
```

Three-OS matrix ensures Apple targets compile on macOS, MinGW compiles on
Windows, and Linux/JVM is sanity-checked on Ubuntu.

### 6.6 Release procedure

1. Update `version` in `build.gradle.kts`.
2. Update `CHANGELOG.md`.
3. Commit and tag: `git tag v0.1.0 && git push --tags`.
4. Create a GitHub Release on that tag (this triggers `publish.yml`).
5. Watch the Actions tab; on success, the artifacts appear on Maven Central
   within ~15-30 minutes (sometimes longer for indexing).

---

## 7. Testing strategy

### 7.1 Test layers

1. **Pure formula tests** (`commonTest`) — call into the `internal/`
   functions directly with hand-computed inputs and expected outputs.
   Examples: `R(t=S, S) ≈ 0.9`, `SInc ≥ 1` for any successful review,
   `D` clamps at 10 and 1, lapse never increases `S`, etc. These are
   property checks first, value checks second.
2. **Reference vectors** — port `py-fsrs`'s test suite. Each test instantiates
   the scheduler with default parameters and the same fixed RNG seed,
   processes the same review history, and asserts the final `Card` state
   matches the recorded reference values to within a tight tolerance (e.g.
   `1e-9` for `S` and `D`, exact for `due` after rounding to whole days).
3. **End-to-end scenario tests** — a small set of "realistic" multi-day
   review sequences exercising every state transition (Learning →
   graduation, Review with all four ratings, lapse → Relearning →
   regraduation, custom parameters, fuzz off/on).
4. **Custom-parameters round-trip** — given user-supplied parameters,
   serialization round-trips them losslessly and the scheduler produces
   reproducible results.

### 7.2 Determinism

Every test injects a fixed `Clock` (a `FixedClock` test helper that returns
a pre-set sequence of `Instant`s) and a seeded `Random`. No `Clock.System`,
no `Random.Default` in tests.

### 7.3 Floating-point tolerance

Compare doubles with `assertEquals(expected, actual, tolerance = 1e-9)`.
Reference vectors generated on one platform/JDK should be reproducible to
this tolerance everywhere. If a target shows wider drift, investigate before
relaxing.

### 7.4 Coverage target

Aim for ~95%+ line coverage on `commonMain`. Use `kover` for KMP coverage
reporting (no setup overhead beyond the plugin).

---

## 8. Documentation

### 8.1 KDoc

Every public type, function, and property must have a KDoc comment
explaining:

- What it represents in the FSRS model.
- For functions, what each parameter does and what the return value means.
- For enums, what each case means and which integer value it serializes to.

KDoc on `commonMain` is the API documentation users see in IDE tooltips and
the rendered Dokka site.

### 8.2 Dokka HTML

Configure Dokka v2 (the v1 plugin is sunset) to generate an HTML site to
`build/dokka/html`. Publish to GitHub Pages via a small Action so each
release also publishes API docs. Link from the README to the deployed Pages
URL.

### 8.3 README

Includes:

- One-paragraph "what is this" + status badges (Maven Central version,
  build status, license, Kotlin version).
- Installation snippet (`implementation("io.github.<ns>:fsrs-kt:1.0.0")`).
- A 20-line quickstart matching the `py-fsrs` quickstart exactly, ported to
  Kotlin.
- A "custom parameters" snippet.
- Compatibility table: targets, Kotlin version.
- Links to Dokka docs and the FSRS papers/wiki.
- License + acknowledgments to OSR.

### 8.4 CHANGELOG

Keep a [Keep a Changelog](https://keepachangelog.com/) format file.
Mandatory entry per release, even if just "no user-visible changes." This
is what users skim before bumping versions.

---

## 9. Versioning

We follow [Semantic Versioning](https://semver.org/) from v1.0.0:

- **PATCH** — bug fixes, doc fixes, internal refactors. Safe to take blindly.
- **MINOR** — new features, additive API. Source-compatible.
- **MAJOR** — breaking changes. Requires migration notes in CHANGELOG.

**Pre-1.0** (`0.x.y`): the API may break across MINOR bumps. We commit to
the algorithm being correct, not to API stability. Aim to ship 1.0 once the
API has stayed unchanged across at least two `0.x` versions and reference
vectors fully pass.

---

## 10. Roadmap

### v1.0 — scheduler only

- Full FSRS-6 scheduler with default and custom parameters.
- Maven Central publication for the target set in §4.3.
- Reference vector parity with `py-fsrs` v6.x.

### v1.x — quality of life

- Optional `:fsrs-kt-serialization` companion module with
  `@Serializable` types.
- Helpers for common app concerns: query "cards due today," batch retrieval
  of retrievability, etc.
- Optional `Duration`-based step constructors.

### v2.0 — optimizer (separate module)

When (and only when) there's real demand and real review data to validate
against, add `:fsrs-kt-optimizer`. Two paths, in rough order of preference:

1. **Pure Kotlin gradient descent.** Build a small autodiff-by-hand
   implementation specialized to the FSRS forward model. Practical because
   the forward model has only ~10 multiplicative terms and 21 parameters —
   manual derivatives are tractable, just tedious. Risk: bugs are quiet
   (optimizer converges to slightly wrong weights without complaining).
   Mitigation: validate against `fsrs-optimizer` on a shared review log.
2. **FFI to `fsrs-rs`.** Wrap the Rust crate via Kotlin/Native cinterop on
   Apple/Linux/Windows and JNI on JVM. Eliminates the autodiff problem;
   adds toolchain weight (Rust build, native libs per target). Best path if
   we want Anki-compatible accuracy without re-implementing Burn.

Either way, the public types from v1 (`Parameters`, `ReviewLog`) are the
input/output surface. No changes needed to v1 consumers.

### Always-deferred non-goals

- Running the optimizer on the user's device for an SRS *flashcard* app
  is rarely worth the complexity. Server-side or one-shot scripts on
  desktop are usually fine. Build it only when consumers need it.

---

## Appendix A: Reference implementations to consult

- **`py-fsrs`** (canonical for our port; pure Python; same data types we
  use). FSRS-6 since v6.0.0; current v6.3.x.
- **`ts-fsrs`** — TypeScript; close API shape to ours.
- **`rs-fsrs`** — Rust scheduler-only; clean and well-commented.
- **`fsrs-rs`** — Rust scheduler + optimizer (Burn-based). The reference for
  any future optimizer work.
- **`dart-fsrs`** — straight port from `py-fsrs`; useful when porting
  test vectors.

## Appendix B: Background reading

- *A Stochastic Shortest Path Algorithm for Optimizing Spaced Repetition
  Scheduling* — Ye, ACM SIGKDD 2022.
- *Optimizing Spaced Repetition Schedule by Capturing the Dynamics of
  Memory* — Ye, IEEE.
- "ABC of FSRS" — OSR wiki; gentle intro.
- "A technical explanation of FSRS" by Expertium — the clearest single
  walkthrough of the FSRS-6 formulas, including the visual derivation of
  why a power forgetting curve fits better than exponential.
- *Implementing FSRS in 100 Lines* (Borretti) — tutorial; written against
  FSRS-5 but the structure is identical.
