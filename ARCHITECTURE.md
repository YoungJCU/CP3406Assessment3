# BuildPC Academy — Architecture Plan

## 1. Scope and learning contract

BuildPC Academy is an educational simulator for university students (18–25), not a storefront. The learner completes guided missions by selecting a CPU, motherboard, GPU, RAM, storage, power supply and case. The app evaluates the completed build and explains the result in plain language.

The first release contains three small missions:

- **Programming workstation** — budget S$1,500; AM5-compatible; adequate CPU, RAM and storage for programming.
- **Entry gaming PC** — budget S$1,800; balanced CPU/GPU performance and adequate PSU headroom.
- **Compact study PC** — budget S$1,200; components must fit a micro-ATX case and use a compatible socket.

The catalogue is deliberately limited to a curated set of parts. This keeps the compatibility rules explainable, testable and achievable within an assignment. Prices are educational example values rather than purchase offers.

## 2. Technology choices

- Kotlin, Jetpack Compose and Material Design 3 for the Android UI.
- Navigation Compose for type-safe route arguments and back-stack navigation.
- Hilt for constructor injection.
- ViewModel plus StateFlow for screen state and lifecycle-aware collection.
- Retrofit with Moshi for remote catalogue retrieval.
- Room for learner-owned data only: progress, submitted mission results, favourite build snapshots and preferences.
- DataStore Preferences for lightweight appearance/accessibility settings.
- JUnit for pure logic and repository/ViewModel tests; Compose UI tests for the key learner journey.

## 3. Project layout

```text
app/
  src/main/java/com/youngjcu/pclab/
    PcLabApplication.kt
    MainActivity.kt
    di/
      AppModule.kt
    data/
      remote/
        HardwareApi.kt
        HardwareRemoteDataSource.kt
      local/
        AppDatabase.kt
      repository/
        HardwareRepository.kt
        LearningRepository.kt
        SettingsRepository.kt
    domain/
      model/
      rules/
        BuildEvaluator.kt
    ui/
      theme/
      AppViewModel.kt
      BuilderViewModel.kt
      PcLabApp.kt
      screens/
  src/test/
    domain/rules/
    data/repository/
    ui/*ViewModelTest.kt
  src/androidTest/
    ui/
```

Feature packages own their screen, ViewModel, state, events and private UI components. Shared components remain in `ui/component`; domain rules never import Android or Compose classes.

## 4. Navigation

```text
Home
 ├─ Continue learning / Start mission ──> Builder(missionId)
 │                                         ├─ Part picker (category)
 │                                         └─ Submit ──> Result(missionId, resultId)
 │                                                        ├─ Save as favourite
 │                                                        └─ Back to Home
 ├─ Statistics ──> Statistics
 └─ Settings ──> Settings
```

`Home`, `Statistics` and `Settings` use the same bottom navigation. `Builder` and `Result` are focused learning routes and show an up action. The selected build remains in `BuilderViewModel` through configuration changes; only a completed build becomes persistent history.

## 5. Domain model

`HardwarePart` is a sealed interface with shared fields: `id`, `name`, `category`, `priceSgd`, `performanceScore`, `powerWatts`, `description` and `learningNote`.

Category-specific fields are intentionally explicit:

- `CpuPart`: socket, supported RAM generation, integrated graphics flag.
- `MotherboardPart`: socket, RAM generation, form factor, PCIe generation and M.2 count.
- `GpuPart`: required wattage, length in mm and performance score.
- `RamPart`: RAM generation, capacity GB and speed MT/s.
- `StoragePart`: interface, capacity GB and form factor.
- `PowerSupplyPart`: wattage and efficiency rating.
- `CasePart`: supported form factors, maximum GPU length and drive bays.

`Mission` contains the learning brief, budget, minimum performance score, ordered requirements and instructional hints. `BuildDraft` holds one selection per category. `Evaluation` holds total cost, estimated power, score, a list of `RuleOutcome`s and a learner-facing explanation for each outcome.

## 6. Compatibility and scoring rules

Rules are pure functions and always return an explanation, including on success.

1. CPU socket must match motherboard socket.
2. RAM generation must be supported by both CPU and motherboard.
3. Case must accept the motherboard form factor.
4. GPU length must not exceed the case maximum.
5. PSU capacity must be at least `(CPU watts + GPU watts + 120W) × 1.25`.
6. Build total must not exceed the mission budget.
7. The weighted performance score must meet the mission threshold; programming weights CPU, RAM and storage more heavily, while gaming weights GPU and CPU.

Scoring is transparent: 40 points for compatibility, 25 for budget, 25 for mission performance and 10 for completing all categories. An invalid selection is never silently blocked: it remains selectable, produces an immediate warning and can be corrected by the learner.

## 7. Remote catalogue and repository flow

The catalogue comes from public JSON files accessed through GitHub Raw. `HardwareApi` retrieves each category endpoint directly, `HardwareRemoteDataSource` loads the catalogue, and `HardwareRepository` maps DTOs into domain parts. The initial load displays a retryable loading/error state and keeps the current in-memory catalogue during the app session.

```text
GitHub REST API -> Retrofit -> HardwareRemoteDataSource
               -> HardwareRepository -> BuilderViewModel -> Compose

Room / DataStore -> Learning and Settings repositories -> ViewModels -> Compose
```

The remote JSON is versioned in a separate public catalogue repository: `YoungJCU/CP3406-A3-API`. It contains only fictionalised educational pricing and technical specifications. No API key, account data or device identifier is required. If the network is unavailable, the app states that a connection is required to start a new mission and provides Retry; it does not falsely claim live data.

Room deliberately does **not** mirror the full hardware catalogue. That separation demonstrates the requested database use without adding an unnecessary sync engine.

## 8. Local persistence

`mission_results`

- `id`, `missionId`, `completedAt`, `score`, `totalCost`, `isCompatible`, `isWithinBudget`, `performanceScore`, `buildSnapshotJson`.

`favourite_builds`

- `id`, `label`, `createdAt`, `buildSnapshotJson`, `totalCost`, `notes`.

`learning_progress`

- one row per mission: `missionId`, `isCompleted`, `bestScore`, `lastAttemptedAt`.

`user_preferences` lives in DataStore rather than Room:

- `darkMode` (`system`, `light`, `dark`), `fontScale` (`normal`, `large`, `extra_large`) and `colourBlindMode`.

Room DAOs expose `Flow`, so home and statistics figures update automatically after a mission result is saved. A compact build-summary string is stored with each result and favourite build so history remains readable even if the remote catalogue later changes.

## 9. Screen specifications

### Home

Shows the next incomplete mission, its learning goal and a prominent Continue/Start button. Small progress cards display completed missions, average score and favourite count. It also offers direct Statistics and Settings shortcuts.

### Builder

Shows mission context, budget remaining, seven ordered component steps and a live compatibility summary. Selecting a part opens a bottom sheet with description, specifications, price and a “Why this matters” lesson. Warnings use icon, text and colour rather than colour alone. Submit is available only after all categories are selected; validation still displays every rule outcome.

### Result

Shows score, budget, compatibility and performance as separate cards. The central learning content is the explanation list, e.g. “Ryzen 7600 uses AM5, but B550 is AM4; choose an AM5 board.” Learners can save a successful or unsuccessful attempt as a favourite for comparison.

### Statistics

Shows completed missions / total, average score, learning progress by mission, favourite builds and five recent attempts. Empty states explain how to produce each measure.

### Settings

Provides theme selection, font scale, colour-blind palette and a reset-progress action. Reset requires confirmation and removes only Room learning records; it does not collect, upload or delete unrelated device data.

## 10. Ethical and accessible design decisions

- **Privacy:** no login, analytics, advertising, background tracking or unnecessary permission. Data is local to the device.
- **Transparency:** all recommendations expose their rule and calculation; catalogue values identify their educational/example nature.
- **Autonomy:** the learner can deliberately explore incompatible combinations and is never funnelled towards an expensive component.
- **Accessibility:** semantic content descriptions, 48dp touch targets, scalable text, non-colour indicators, dark theme and colour-blind-safe status tokens.
- **Inclusiveness:** neutral, adult-oriented wording; missions are skill-based rather than assuming prior hardware ownership or gaming identity.

## 10.1 Review-document evidence to capture during development

The assessment's review document needs to be descriptive rather than a short feature list. Capture the following evidence as each feature is built:

- **Variables and state:** document `BuildDraft`, `BuilderUiState`, `Evaluation`, `MissionResult`, selected-part IDs, budget remaining, loading/error state and why each belongs in the ViewModel or domain layer.
- **Assets:** record the app icon, Material symbols and any component illustrations; include source/licence, purpose, dark-theme treatment and content description. Prefer Material icons and self-made simple illustrations to avoid unclear licences.
- **User data:** list exactly what Room and DataStore retain, why it is needed, where it resides and the Reset Learning Progress behaviour. State explicitly that no identity, location, contacts, advertising ID or analytics data is collected.
- **Features:** for every screen, include an annotated screenshot, user goal, inputs, outputs, data source and a short explanation of its educational value.
- **Design decisions:** record the limited catalogue, mission-first flow, visible explanations, non-colour status labels, choice of local-first progress and no-shopping design. Connect each to learning, ethics or a rubric requirement.
- **System explanation:** include the data-flow diagram above, navigation map, Room schema, API request/response sample (with secrets removed), test results and a brief known-limitations section.

## 11. Test plan and acceptance criteria

Unit tests cover:

- socket, RAM generation, case-size, GPU-length and PSU-headroom rule outcomes;
- exact budget boundary and weighted performance calculations;
- mission evaluation aggregation and score calculation;
- repository mapping and remote success/failure behaviour with a fake API;
- ViewModel loading, selection, submit and persistence state using fake repositories.

Compose UI tests cover:

- Home navigates to the selected mission;
- a learner can select all seven categories and submit;
- an incompatible CPU/motherboard pair presents its explanation;
- Settings changes are visible after navigation/recreation.

Completion criteria: all four screens are reachable, catalogue loading/retry is demonstrated, a completed attempt updates Room-backed statistics, settings persist, and the listed test suite passes.

## 12. Build sequence

1. Create the Gradle/Compose/Hilt project and base navigation/theme.
2. Implement domain models, missions and pure compatibility tests first.
3. Add Retrofit catalogue retrieval and its test fake.
4. Add Room/DataStore repositories and Hilt bindings.
5. Implement Home and Builder, then Result persistence.
6. Add Statistics and Settings.
7. Add Compose tests, accessibility pass, README, screenshots and meaningful Git commits.

This order produces a demonstrable, rubric-aligned app without an unnecessary backend, authentication system, shopping cart or catalogue database.
