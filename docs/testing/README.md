# Testing entry point

1. Read production code; grep [catalog.md](catalog.md).
2. Pick path from [TESTING.md decision tree](../TESTING.md#decision-tree).
3. Prefer a same-layer sibling that already extends the cataloged leaf; if unsure, use a [layer exemplar](catalog.md#proof-tests). Do not invent inline `startKoin` / `Dispatchers.setMain` when a leaf base exists.
4. Use skeleton from [recipes.md](recipes.md).
5. Run Gradle command from [TESTING.md](../TESTING.md#commands); attach output or state tests were not run.

**Forbidden:** libraries not in [allowlist](../TESTING.md#library-allowlist); invented helper paths; double `startKoin` with `TestApplication`; tests for Kover-excluded code.
