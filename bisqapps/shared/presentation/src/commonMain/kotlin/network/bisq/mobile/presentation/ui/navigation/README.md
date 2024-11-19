# Navigation Overview

The app uses **Jetpack Compose Navigation**.

### 1. Routes
- All routes are defined in `Routes.kt` as an `enum` for unique screen identifiers.
- Like
  - /splash
  - /onboarding


### 2. Root Navigation
- `RootNavGraph.kt` defines the top-level flow using `NavHost`.
- Maps routes to Screens with transitions like
  - /splash - SplashScreen
  - /onboarding -> OnboardingScreen

### 3. Tab Navigation
- `TabNavGraph.kt` manages nested tab-based screens (`Home`, `Exchange`, `MyTrades`, `Settings`).

### 4. Bottom Navigation
- Composable that renders the bottom navigation bar.
- Exact tabbar items are received via `items` prop.

###

```
[Root Nav] -> Splash | Tab Container Screen | (Screens from any of the tabs will be pushed here)
                                |
                                v
                            [Tab Nav]
                                |
                                |- [Tab1] Getting Started
                                |- [Tab2] Buy / Sell
                                |- [Tab3] My Trades
                                |- [Tab4] Settings
```

Ref links:
 - https://developer.android.com/develop/ui/compose/navigation
 - https://developer.android.com/codelabs/basic-android-kotlin-compose-navigation#0
 - https://medium.com/@KaushalVasava/navigation-in-jetpack-compose-full-guide-beginner-to-advanced-950c1133740
