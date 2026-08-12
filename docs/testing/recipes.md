# Test recipes

Copy-paste skeletons. Grep production code for constructor params marked `VERIFY`. Paths: [catalog.md](catalog.md).

---

## Presenter {#presenter}

Base: `PresentationKoinTestBase` (default) or `PlatformPresentationKoinTestBase` (`getScreenWidthDp`). Proof: `FaqPresenterTest`, `OfferbookPresenterFilterTest`.

Base already provides `navigationManager` + `globalUiManager`. Setup goes in `onKoinReady()`. If you override `beforeStartKoin` / `onTearDown`, **always call `super`** (`try/finally` for tear-down). Do not remock the managers after `super.beforeStartKoin()` unless replacing with a real/`spyk` instance.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyPresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true) // VERIFY: grep constructor deps

    @Test
    fun `when action fired then updates ui state`() = runTest {
        val presenter = MyPresenter(mainPresenter = mainPresenter) // VERIFY: grep constructor
        presenter.onAction(MyUiAction.SomeClick)
        advanceUntilIdle()
        assertEquals(expected, presenter.uiState.value.field)
    }

    @Test
    fun `when action fired then side effect on collaborator`() = runTest {
        val presenter = MyPresenter(mainPresenter = mainPresenter)
        presenter.onAction(MyUiAction.SomeClick)
        advanceUntilIdle()
        coVerify(exactly = 1) { mainPresenter.someMethod(any()) }
        verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
    }
}
```

Pitfalls: no `startKoin` in test class; no `ClientKoinIntegrationTestBase` for `:shared:presentation`; call `advanceUntilIdle()` before `StateFlow` asserts; abstract presenters need a test subclass in-file. If the test asserts localized copy (`.i18n()`, snackbar/error text), call `I18nSupport.initialize("en")` in `onKoinReady()` — VERIFY: needed for i18n keys? Skip it for guard-only tests.

---

## Compose {#compose}

| Scenario | Base |
| --- | --- |
| No Koin | `BisqComposeUiTestBase` |
| Presentation + Koin | `PresentationKoinComposeTestBase` / `PlatformPresentationKoinComposeTestBase` |
| Client + `TestApplication` | `BisqComposeUiTestBase` + `@Config(application = TestApplication::class)` — Koin from Application |
| Client + inject overrides | `ClientInjectComposeUiTestBase` — plain `Application`, owned `startKoin` |

Always set content via `setBisqTestContent` / `setTestContent` (`LocalIsTest` + `BisqTheme`). Leaf-base `setTestContent` calls `waitForIdle()` after set — still `waitForIdle()` after interactions. Proof: `SwitchUiTest`, `LinkButtonUiTest`, `PaymentAccountMethodIconUiTest` (client + `TestApplication`).

### No Koin

```kotlin
class MyComponentUiTest : BisqComposeUiTestBase() {
    @Test
    fun `when tapped then invokes callback`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setTestContent {
            MyComponent(label = "Save", enabled = true, onClick = onClick) // VERIFY
        }
        composeTestRule.onNodeWithText("Save").performClick()
        verify(exactly = 1) { onClick() }
    }
}
```

### Presentation + Koin

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyScreenUiTest : PresentationKoinComposeTestBase() {
    private lateinit var mainPresenter: MainPresenter // VERIFY

    override fun onKoinReady() {
        mainPresenter = mockk(relaxed = true)
        coEvery { mainPresenter.navigateToUrlWithLauncher(any()) } returns true
    }

    @Test
    fun `when button clicked then shows dialog`() {
        setTestContent { MyScreenContent(/* VERIFY */) }
        composeTestRule.onNodeWithText("Open").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Confirm title".i18n()).assertIsDisplayed()
    }
}
```

### Client + TestApplication

Base: `BisqComposeUiTestBase` with Robolectric `@Config(application = TestApplication::class)`. Do **not** call `startKoin` / `createComposeRule` yourself — the leaf base owns Compose UI Test v2 setup (`junit4.v2.createComposeRule`); `TestApplication.onCreate()` owns Koin. Proof: `PaymentAccountMethodIconUiTest`.

```kotlin
@Config(application = TestApplication::class)
class MyClientContentUiTest : BisqComposeUiTestBase() {
    @Test
    fun `when card renders then shows account name`() {
        setTestContent { MyClientCard(account = sampleAccount()) } // VERIFY
        composeTestRule.onNodeWithText("Account A").assertIsDisplayed()
    }
}
```

Pitfalls: no double `startKoin` with `TestApplication`; never combine `TestApplication` with `PresentationKoinComposeTestBase` / `ClientKoinIntegrationTestBase` / `ClientInjectComposeUiTestBase`; use Compose UI Test v2 (`androidx.compose.ui.test.junit4.v2.createComposeRule`) — leaf bases already do; `SecureScreenEffectUiTest` is the only allowed `createAndroidComposeRule` exception; if a test owns both `Dispatchers.setMain(testDispatcher)` and a local compose rule, pass `createComposeRule(effectContext = testDispatcher)` so composition and Main share one scheduler; leaf-base `setTestContent` already idles — still `waitForIdle()` after clicks/actions; prefer that over `advanceUntilIdle()` in Compose+Koin tests; use `.i18n()` for localized strings.

### Client + inject overrides

Base: [`ClientInjectComposeUiTestBase`](catalog.md#leaf-bases). For screens that `koinInject` presenters (often via `RememberPresenterLifecycleBackStackAware`) and need per-test doubles. Proof: `ClientSplashScreenUiTest`.

```kotlin
class MyScreenUiTest : ClientInjectComposeUiTestBase() {
    private lateinit var facade: PaymentAccountsServiceFacade // VERIFY
    // Only if MyPresenter (or another binding) needs it — omit otherwise.
    private lateinit var mainPresenter: MainPresenter // VERIFY

    override fun onBeforeKoinStart() {
        facade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true) // omit if unused
    }

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<PaymentAccountsServiceFacade> { facade }
                factory { MyPresenter(facade, mainPresenter) } // VERIFY — drop mainPresenter if unused
            },
        )

    @Test
    fun `when rendered then shows title`() {
        setInjectTestContent { MyScreen() } // VERIFY
        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
    }
}
```

Pitfalls: build mocks in `onBeforeKoinStart()` before `additionalModules()` captures them (no inline `mockk()` inside factories); use `setInjectTestContent` (not bare `setTestContent`) so BackStackAware gets a fresh `ViewModelStore`; do not call `startKoin` / `stopKoin` yourself.

---

## Client facade {#client}

Base: `ClientKoinIntegrationTestBase`. Proof: `ClientSettingsServiceFacadeTest`.

```kotlin
class MyClientFacadeTest : ClientKoinIntegrationTestBase() {
    private lateinit var facade: MyClientFacade // VERIFY
    private lateinit var apiGateway: MyApiGateway // VERIFY

    override fun onSetup() {
        apiGateway = mockk(relaxed = true)
        facade = MyClientFacade(apiGateway, SettingsRepositoryMock()) // VERIFY
    }

    @Test
    fun `when getSettings succeeds then updates flows`() = runTest {
        coEvery { apiGateway.getSettings() } returns Result.success(settingsVo) // VERIFY
        facade.activate()
        advanceUntilIdle()
        val result = facade.getSettings()
        advanceUntilIdle()
        assertTrue(result.isSuccess)
        assertEquals("es", facade.languageCode.value) // VERIFY: grep StateFlow names
    }
}
```

Pitfalls: construct facade manually in `onSetup()`; mock at gateway/repository boundary; call `activate()` before flow asserts; no `TestApplication` in same class.

---

## Domain / commonTest {#domain}

No base class. Formatters: plain `kotlin.test`. ServiceFacades: inline Koin. Proof: `UserDefinedAccountsServiceFacadeTest`.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyServiceFacadeTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var facade: TestMyServiceFacade // VERIFY: test subclass

    @BeforeTest fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(testModule) }
        facade = TestMyServiceFacade()
    }

    @AfterTest fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `when operation succeeds then updates state`() = runTest(testDispatcher) {
        facade.mockExecuteGetItems = { Result.success(items) } // VERIFY
        val result = facade.getItems()
        advanceUntilIdle()
        assertTrue(result.isSuccess)
    }
}
```

Pitfalls: use `@BeforeTest`/`@AfterTest` not JUnit `@Before`/`@After`; no `CoroutineTestBase`; use `testModule` not `clientTestModule` in `shared/domain`.

---

## iOS {#ios}

`kotlin.test` lifecycle only — no JUnit, MockK Android, Robolectric. Proof: `LocalEncryptionIosTest`.

```kotlin
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class MyPlatformBridgeIosTest {
    @AfterTest
    fun cleanup() {
        deleteTestArtifacts("test_key_1") // VERIFY: match Keychain aliases written
    }

    @Test
    fun encryptAndDecryptRoundTrip() {
        val plaintext = "Hello".encodeToByteArray()
        val keyAlias = "test_key_1"
        val encrypted = encryptSync(plaintext, keyAlias) // VERIFY: grep bridge API
        val decrypted = decryptSync(encrypted, keyAlias)
        assertContentEquals(plaintext, decrypted)
    }
}
```

```bash
./gradlew :shared:domain:iosSimulatorArm64Test --tests "network.bisq.mobile.crypto.LocalEncryptionIosTest"
```

Pitfalls: macOS + Xcode required; clean up Keychain/files in `@AfterTest`; Intel CI uses `iosSimulatorX64Test`.
