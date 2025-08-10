# Jetpack Compose Guidelines

This document outlines the general best‑practice guidelines we expect contributors to follow. It is not exhaustive, but it provides a baseline for the expected quality of contributions.

## Core principles

- Prefer stateless UI; hoist state to callers
- Unidirectional data flow: pass state down, send events up
- Favor immutable models; use `@Stable` and `@Immutable` on classes where appropriate
- Prefer cold `Flow`s over hot `StateFlow`s unless continuous observation is required

## Tips

### State management

- Hoist state: make UI composables stateless when possible; keep state in Presenters/ViewModels.
- Prefer explicit UI state (Loading/Empty/Error/Content) over `null`. but It’s fine to keep `null` when data is truly optional.
- Use remember/rememberSaveable appropriately; prefer rememberSaveable for process death resiliency.
- Keep state types stable (immutable data classes with proper equals)
- As a general rule of thumb, Always use `val`s unless It's not possible to do so. Avoid mutable public `var`s.
- Don't expose `MutableStateFlow` from public APIs. Expose `StateFlow` safely.
- Derive expensive/computed values with derivedStateOf to limit unnecessary recompositions.
- Expose events as lambdas; enforce unidirectional data flow from state down, events up.

```kotlin
// Good: stateless, events up
@Composable 
fun Counter(count: Int, onInc: () -> Unit) { 
  Button(onClick = onInc) { Text("$count") } 
}
```

```kotlin
// inside ViewModel/Presenter
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> get() = _uiState.asStateFlow()
```

### Performance

- Prefer delegated reads (`by`) to avoid eager reads.
- Keep parameters stable; prefer immutable data classes.
- Minimize recomposition scope: split large composables, read only needed state in each.
- Avoid expensive measure/layout work; cache results, and avoid `onGloballyPositioned` unless necessary.
- Don’t start long work in composition; use effects or Presenter/ViewModel
- Heavy mapping belongs in the Presenter/ViewModel or use-case, not in the composable.
- Avoid recreating objects/lambdas in composition (use remember).

```kotlin
// Good:
@Composable
fun Screen(presenter: Presenter) {
    // delegated reads prevents eager reads:
    val uiState by presenter.uiState.collectAsState()
    Text(uiState.title) // read triggered here
}

// Avoid:
@Composable
fun Screen(presenter: Presenter) {
    // Triggers read here, not at use site
    val uiState = presenter.uiState.collectAsState().value
    // use site:
    Text(uiState.title)
}
```
  
### Side-effects

- Use LaunchedEffect with stable keys for suspend work tied to composition.
- Use DisposableEffect for listeners/resources requiring cleanup on leaving composition.
- Use rememberCoroutineScope for event-driven jobs started by user actions.

### Lists and paging

- Use LazyColumn/LazyRow with stable keys. use `contentType` where appropriate.
- Keep `item` composables small and stable and possibly stateless; avoid heavy modifiers in hot paths.
- Implement pagination using paging-compose and collectAsLazyPagingItems; handle load states.
- Preserve scroll state with rememberLazyListState and rememberSaveable where needed.

### Modifiers & layout

- Order matters. General pattern:
  - size/weight -> padding -> clip -> background/border -> clickable/indication -> semantics
- Clickable after padding to expand hit area

### Theming and Constants

- Avoid hardcoded values and use the following where appropriate:
- Use sizes from `BisqUIConstants`
- Use colors from `BisqTheme`

### Accessibility

- Provide meaningful contentDescription for images/icons and `null` for decorative images/icons
- Use semantics for custom components.
- Maintain minimum 48dp touch targets and clear focus indicators whenever possible.
- Ensure sufficient color contrast.
- Try to support large font scales, meaning that try to keep UI usable even when user has increased their font size from device settings.
- Control reading order and grouping with semantics/traversal where needed.
- Offer reduced motion options; avoid excessive or disorienting animations.

## References for follow-up reading

- [Android docs: Compose best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- [a short article about recomposition](https://medium.com/@kacper.kalinowski/mastering-recomposition-in-jetpack-compose-284e2ce9f4e1)
