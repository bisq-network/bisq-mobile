# Architecture & conventions

Agent-oriented map of how this repo is structured. Canonical narrative lives in the [README](../README.md#app-architecture-design-choice); data naming lives in [replicated/README.md](../shared/domain/src/commonMain/kotlin/network/bisq/mobile/data/replicated/README.md). This file is the short pointer for agents (and humans) who need the conventions without re-reading the whole README.

> Naming: The project uses MVP (Model–View–Presenter). Screen contracts use sealed `*UiAction` (user intents) and immutable `*UiState` — not a separate pattern named “MVIP.”

Convention (confirmed): New screens always ship with `*UiState` + `*UiAction` + `onAction(...)`. Older presenters that expose ad-hoc methods / multiple `StateFlow`s are being gradually converted to this shape when touched. Prefer a Use Case when multi-step domain orchestration would bloat the presenter (see [README](../README.md#use-cases-encapsulate-complex-workflows)).

---

## Apps & modules

| Piece | Role |
|-------|------|
| `:apps:nodeApp` | Bisq Easy Node (Android) — embeds bisq2 core |
| `:apps:clientApp` + `iosClient/` | Bisq Connect — thin client to a trusted node (Tor/clearnet) |
| `:shared:domain` | Service facade contracts, repositories, replicated VO/Dto/Model, i18n |
| `:shared:presentation` | Compose UI, presenters, navigation, shared DI |
| `:shared:test-utils` | Shared testing infrastructure |
| `:shared:kscan` | QR scanning (client) |

Package note: Gradle module `:shared:domain` hosts both `network.bisq.mobile.data.*` and `network.bisq.mobile.domain.*`.

---

## MVP layers

| Layer | Typical types | Responsibility |
|-------|---------------|----------------|
| View | `*Screen`, `*Content` | Stateless Compose; collect `StateFlow`; emit actions |
| Intent | `*UiAction` | Sealed user events → `presenter.onAction(...)` |
| UI state | `*UiState` | Immutable screen state owned by the presenter |
| Presenter | `*Presenter` / `I*Presenter` | Orchestrates facades/repos/use cases; maps domain → `uiState`; handles `onAction` |
| Domain | Use cases, repositories, `*ServiceFacade` | Business workflows and data access |

New screen shape: `*Screen` / `*Content` + `*UiState` + `*UiAction` + presenter with `val uiState: StateFlow<*UiState>` and `fun onAction(action: *UiAction)`. Optional `I*Presenter` still fine when the view needs a narrow contract.

Base types: [`BasePresenter`](../shared/presentation/src/commonMain/kotlin/network/bisq/mobile/presentation/common/ui/base/BasePresenter.kt) / `ViewPresenter`. Root is `MainPresenter` (`ClientMainPresenter` / `NodeMainPresenter`); feature presenters take the root as a constructor arg.

Reuse rule: Reuse a presenter only for small sub-views; extend the view’s presenter interface and add the correct Koin `bind`. See [README](../README.md#when-its-acceptable-to-reuse-a-presenter-for-my-view).

Not presenters: `CreateOfferCoordinator` / `TakeOfferCoordinator` are Koin singletons holding wizard state across steps. Offer wizard steps extend `OfferFlowPresenter`.

Proof-style examples: `FaqPresenter` / `SettingsPresenter` / payment-account presenters under `apps/clientApp`. Compose guidance: [docs/compose-guidelines/README.md](compose-guidelines/README.md).

---

## Client vs Node `ServiceFacade`

```text
shared/domain  →  *ServiceFacade (interface or abstract)
apps/clientApp →  Client*ServiceFacade  (HTTP + WebSocket to trusted node)
apps/nodeApp   →  Node*ServiceFacade    (bisq2 core via AndroidApplicationService)
```

- Presenters and shared code depend only on the shared facade type.
- App DI modules swap the implementation (`ClientDomainModule` / `NodeDomainModule`).
- Node often talks to bisq2 models directly (core owns persistence). Client updates arrive via WebSocket; local repos (e.g. settings) still apply.

Tests: mock shared facade interfaces in presenter tests; use client/node integration bases when testing a concrete facade. See [testing catalog](testing/catalog.md).

### Feature availability services

For plain backend gating, use `BackendCapabilitiesService` directly in the presenter — one
capability, one consumer, no extra type (e.g. `MiscItemsPresenter` gating the Network item on
`Feature.NETWORK_INFO`). That is the default.

A dedicated availability service is the escalation, warranted only when the feature composes
MORE than the capability check — a staged-rollout constant, a dev feature-flag override — or
must answer "which parts of me exist right now" for more than one presenter (screen + chrome
such as a top-bar icon). Then that answer lives in a domain-level service under
`shared/domain/.../service/<feature>/`, not in any presenter or `UiState`:

- Composes `(shipped ∪ devOverride) ∩ backend capabilities`, where the capability check goes
  through `BackendCapabilitiesService` and **fails closed** — the dev override never bypasses it.
  On the node app the capability filter passes by construction (the node reports the full
  `Feature` set since it runs the core in-process), so node visibility depends only on shipped
  features and the dev override — no per-app fork needed.
- Exposes a reactive `StateFlow` computed on a process-lifetime scope (`stateIn(Eagerly)`),
  because presenters are view-bound and the answer is needed between screens.
- Screen-local state (e.g. which tab is selected) stays in the presenter's `UiState`; only the
  shared, app-lifetime parts belong here.

Current instance: `CommunityHubService`. If a second composite appears (e.g. MuSig-on-Connect
gating), extract the shared core then — not before.

---

## Presenter lifecycle modes

Canonical helpers:

- [`RememberPresenterLifecycle`](../shared/presentation/src/commonMain/kotlin/network/bisq/mobile/presentation/common/ui/utils/LifecycleHelper.kt) — default
- [`RememberPresenterLifecycleBackStackAware`](../shared/presentation/src/commonMain/kotlin/network/bisq/mobile/presentation/common/ui/utils/BackStackAwarePresenterLifecycleHelper.kt) — opt-in

| Helper | Scope on leave | Use for |
|--------|----------------|---------|
| `RememberPresenterLifecycle` | Disposed (`onViewUnattaching`) | Splash, onboarding, settings, dialogs, always-fresh screens |
| `RememberPresenterLifecycleBackStackAware` | Stays alive on back stack (`onViewHidden` / `onViewRevealed`); disposed when popped | Tabs, offerbook, trade, wizards, expensive loads; also survives Android config changes |

```kotlin
// Fresh each visit
val presenter: SettingsPresenter = koinInject()
RememberPresenterLifecycle(presenter)

// Survives back stack / config change
val presenter = RememberPresenterLifecycleBackStackAware<DashboardPresenter>()
```

Tests: for back-stack-aware screens, expect `onViewHidden` / `onViewRevealed` on tab switch — do not assume scope disposal. Drive attach/unattach via the lifecycle hooks the screen actually uses.

Full detail: [README § Presenter Lifecycle](../README.md#presenter-lifecycle).

---

## Koin DI hierarchy

Client (`ClientModules.kt` → `ClientMainApplication`):

`dataModule` + `presentationModule` + `clientDomainModule` + `clientPresentationModule` + `paymentsAccountsModule`  
(+ `androidClientDomainModule` / `androidClientPresentationModule` on Android; iOS equivalents on iOS)

Node (`NodeMainApplication`):

`dataModule` + `androidNodeDomainModule` + `presentationModule` + `androidNodePresentationModule`

| Convention | Example |
|------------|---------|
| Shared contract, app impl | `single<SettingsServiceFacade> { ClientSettingsServiceFacade(...) }` |
| Presenter + view interface | `factory { XPresenter(...) } bind IXPresenter::class` |
| Root | `single<MainPresenter> { ... } bind AppPresenter::class` |
| Per-presenter scope | `CoroutineJobsManager` as `factory` |

Key files: `DataModule.kt`, `PresentationModule.kt`, `ClientDomainModule.kt`, `ClientPresentationModule.kt`, `NodeDomainModule.kt`, `NodePresentationModule.kt`.

---

## Data conventions (VO / Dto / Model)

Canonical: [shared/.../data/replicated/README.md](../shared/domain/src/commonMain/kotlin/network/bisq/mobile/data/replicated/README.md).

| Suffix | Meaning |
|--------|---------|
| `Dto` | Immutable transfer / internal construction; not typically consumed directly by UI |
| `VO` | Immutable value object exposed to services/presentation |
| `Enum` | Replicated enums (not `VO`) |
| `Model` | Mutable wrapper around Dto; delegates fields; observables as `StateFlow` |
| `Item` (in name) | Presentation list items (e.g. `OfferItemPresentationModel`) |

Presentation UI types (separate): `*UiState`, `*UiAction`.

Node mapping from bisq2: `apps/nodeApp/.../mapping/Mappings.kt` (`fromBisq2Model`), with one carve-out.
The private-chat types map in per-type files under `apps/nodeApp/.../mapping/chat/`, as `toDomain()`
extension functions — message, channel and reaction for each of the two `PrivateChatMessage<R>`
subtypes, so six files: `BisqEasyOpenTrade*` for trade chat and `TwoPartyPrivateChat*` for DMs. They
are the ones carrying the generic hierarchy, so their mappings are long enough to be worth reading
next to the type, and a third subtype would add three more. Everything else still lives in
`Mappings.kt`, including the smaller chat mappings (`CitationMapping`, `ChatMessageTypeMapping`,
`BisqEasyOfferbookMessageReactionMapping` and the two channel enums).
Helpers use `*Extensions` / `*Factory` / `*Utils`.

---

## i18n in Compose (`LocalLanguageCode`)

In-app language changes update global `I18nSupport` bundles. Compose does not recompose when bundles change unless something in the tree observes language.

**Root provider:** `LocalLanguageCode` is a `staticCompositionLocalOf` provided under `BisqTheme` in `App.kt` from `I18nSupport.currentLanguage` (`StateFlow`, advanced only after bundles are swapped).

| Category | What | Observes applied language? |
|----------|------|----------------------------|
| **A** | Key-only strings → emit `UiString`; resolve in composable via `.resolve()` / `i18nText` | No |
| **B** | Locale formatting (amounts, dates, localized market names) | Yes — observe `I18nSupport.currentLanguage` |
| **A-with-B-args** | Key is A but args come from B (e.g. headline interpolating localized market name) | Yes — observe `I18nSupport.currentLanguage`; migrating the key does not drop it |

Forward path: Category A migrations delete presenter language threading. The static local backstops unmigrated `.i18n()` call sites **resolved inside composition** (full subtree recomposition on language change). Category B / A-with-B-args presenters observe `I18nSupport.currentLanguage` (applied signal), not `MainPresenter` / settings passthrough. Persisted settings remain on `SettingsServiceFacade.languageCode`. Do not inject `MainPresenter` solely to read language in UI.

Reference migrations: Splash screens, MiscItems menu (`UiString` in presenter, resolve in composable).

---

## Agent checklist

When changing or testing code:

1. New screens: always `*UiState` + `*UiAction` + `onAction`; convert old presenters to that shape when you touch them.
2. Depend on shared facade interfaces in `shared/`; never inject `Client*` / `Node*` types into shared presenters.
3. Pick the lifecycle helper that matches the screen (table above) before writing attach/unattach tests.
4. Mock facades + `MainPresenter`, not networking or bisq2 core, in shared presenter tests.
5. Follow [AGENTS.md](../AGENTS.md) testing rules (allowlist, catalog, leaf bases, recipes).
6. Wire a `factory` in the right presentation module; choose the lifecycle helper from the table above.

---

## Related docs

| Topic | Doc |
|-------|-----|
| Architecture narrative & lifecycle diagrams | [README](../README.md#app-architecture-design-choice) |
| VO / Dto / Model | [replicated/README.md](../shared/domain/src/commonMain/kotlin/network/bisq/mobile/data/replicated/README.md) |
| Compose | [compose-guidelines](compose-guidelines/README.md) |
| Navigation | [presentation/.../navigation/README.md](../shared/presentation/src/commonMain/kotlin/network/bisq/mobile/presentation/common/ui/navigation/README.md) |
| Testing | [TESTING.md](TESTING.md), [testing/](testing/README.md) |
| Android platform constraints | [android.md](android.md) |
| Design | [design/DESIGN_GUIDE.md](design/DESIGN_GUIDE.md) |
