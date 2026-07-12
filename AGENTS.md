# Agent instructions

Before adding or changing tests:

1. Read [docs/testing/README.md](docs/testing/README.md) and follow its contract.
2. Grep [docs/testing/catalog.md](docs/testing/catalog.md) before creating mocks, modules, or base classes.
3. Use only libraries in the [allowlist](docs/TESTING.md#library-allowlist) — never Mockito, Turbine, Kotest, AssertJ, Truth, or Hamcrest.
4. Extend a leaf base class — do not copy inline `startKoin` / `Dispatchers.setMain` from unmigrated siblings.
5. Open a proof test in the same layer before writing (`FaqPresenterTest`, `OfferbookPresenterFilterTest`, `ClientSettingsServiceFacadeTest`, `SwitchUiTest`).
6. Copy skeleton from [docs/testing/recipes.md](docs/testing/recipes.md); run the module-scoped Gradle command from [docs/TESTING.md](docs/TESTING.md#commands).
