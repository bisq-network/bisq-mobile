# Agent instructions

## Architecture & conventions

Before changing product code (or writing tests that depend on layer boundaries), read [docs/architecture.md](docs/architecture.md) for:

- MVP layers — new screens always use `*UiState` + `*UiAction`; gradually convert older presenters when touched
- Client vs Node `ServiceFacade` split (what to mock)
- Presenter lifecycle modes (`RememberPresenterLifecycle` vs `RememberPresenterLifecycleBackStackAware`)
- Koin DI hierarchy and data naming (VO / Dto / Model)

Canonical narrative: [README § App Architecture](README.md#app-architecture-design-choice). Data suffixes: [replicated/README.md](shared/domain/src/commonMain/kotlin/network/bisq/mobile/data/replicated/README.md).

## Testing

Before adding or changing tests:

1. Read [docs/testing/README.md](docs/testing/README.md) and follow its contract.
2. Grep [docs/testing/catalog.md](docs/testing/catalog.md) before creating mocks, modules, or base classes.
3. Use only libraries in the [allowlist](docs/TESTING.md#library-allowlist) — never Mockito, Turbine, Kotest, AssertJ, Truth, or Hamcrest.
4. Extend a leaf base class when the catalog lists one — do not invent inline `startKoin` / `Dispatchers.setMain` (domain ServiceFacades use the inline recipe).
5. Prefer a same-layer sibling on the cataloged leaf; if unsure, open a [layer exemplar](docs/testing/catalog.md#proof-tests) (`FaqPresenterTest`, `OfferbookPresenterFilterTest`, `ClientSettingsServiceFacadeTest`, `SwitchUiTest`, `LinkButtonUiTest`, `PaymentAccountMethodIconUiTest`, `ClientSplashScreenUiTest`, `PeerProfileScreenUiTest`).
6. Copy skeleton from [docs/testing/recipes.md](docs/testing/recipes.md); run the module-scoped Gradle command from [docs/TESTING.md](docs/TESTING.md#commands).
7. Rationale for why shared presentation test fixtures live in `:shared:test-utils`: [docs/TESTING_FIXTURES.md](docs/TESTING_FIXTURES.md).
