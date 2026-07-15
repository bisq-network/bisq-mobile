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

Node mapping from bisq2: `apps/nodeApp/.../mapping/Mappings.kt` (`fromBisq2Model`). Helpers use `*Extensions` / `*Factory` / `*Utils`.

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
| Design | [design/DESIGN_GUIDE.md](design/DESIGN_GUIDE.md) |
