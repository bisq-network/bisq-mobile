# Testing entry point

1. Read production code; grep [catalog.md](catalog.md).
2. Pick path from [TESTING.md decision tree](../TESTING.md#decision-tree).
3. Copy a [proof test](catalog.md#proof-tests) in the same layer — **not** legacy siblings with inline `startKoin`.
4. Use skeleton from [recipes.md](recipes.md).
5. Run Gradle command from [TESTING.md](../TESTING.md#commands); attach output or state tests were not run.

**Forbidden:** libraries not in [allowlist](../TESTING.md#library-allowlist); invented helper paths; double `startKoin` with `TestApplication`; tests for Kover-excluded code.
