# Bisq Mobile Design Guide

This document serves as the design reference for the Bisq Mobile project. Since the team has no dedicated designer, this guide captures design decisions, conventions, and references so that any contributor (human or AI agent) can propose consistent mobile UX.

---

## Design Philosophy

- **Privacy-first**: Never expose sensitive data by default. Prefer opt-in over opt-out.
- **Serve both novice and experienced users**: Onboarding and trade guides for newcomers, efficient flows for power users.
- **Dark theme only**: The app uses a single dark theme optimized for mobile OLED screens.
- **Consistency with Bisq2 Desktop**: The mobile app ports desktop features. The desktop codebase (symlinked at `./bisq2`) is the primary design reference.

## Primary Design Reference: Bisq2 Desktop

The desktop app's UI code is accessible at `bisq2/apps/desktop/desktop/src/main/java/bisq/desktop/`. Key resources:

- **Views**: `bisq2/apps/desktop/desktop/src/main/java/bisq/desktop/main/content/` — 228 view classes organized by feature
- **CSS/Themes**: `bisq2/apps/desktop/desktop/src/main/resources/css/` — 13 CSS files defining colors, typography, component styles
- **Components**: `bisq2/apps/desktop/desktop/src/main/java/bisq/desktop/components/` — reusable controls, tables, overlays, containers

When porting a desktop feature to mobile, **read the desktop view code** to understand the layout, data displayed, user interactions, and flow. Then adapt for mobile constraints (smaller screen, touch input, single-column layouts, bottom navigation).

### Desktop Layout (for context)

The desktop uses a left sidebar navigation (190px expanded / 70px collapsed) with 13+ main sections. Content appears in a dynamic main area. Key patterns:
- Split-pane layouts (e.g., offerbook: market list + chat side-by-side)
- Wizard flows (create offer, take offer, wallet setup)
- Master-detail (profile cards, trade list → trade detail)
- Tabbed views (settings, market filters)
- Modal dialogs for confirmations and warnings

### Mobile Adaptation Principles

- **Left sidebar → Bottom tab navigation** (4 tabs: Home, Offerbook, Trades, More)
- **Split panes → Sequential screens** (e.g., market list screen → offerbook screen)
- **Wizards → Step-by-step full-screen flows** with back navigation
- **Master-detail → List screen → Detail screen**
- **Modal dialogs → Bottom sheets or full-screen dialogs**
- **Tables → Scrollable card lists**

---

## Color Palette

Defined in `shared/presentation/.../ui/theme/BisqColor.kt`. Uses gamma adjustment (1.12f) for mobile.

| Token | Hex | Usage |
|-------|-----|-------|
| Primary (green) | #56AE48 | Main actions, success states |
| Danger (red) | #D23246 | Destructive actions, errors |
| Warning (orange) | #FF9823 | Warnings, caution states |
| Background | #1C1C1C | Screen background |
| Secondary | #2C2C2C | Text field backgrounds, cards |
| White | #FAFAFA | Primary text |
| Grey scale | dark_grey10-50, mid_grey10-30, light_grey10-50 | Secondary text, borders, dividers |

## Typography

Defined in `shared/presentation/.../ui/theme/BisqTypography.kt`.

- **Font family**: IBM Plex Sans (matches desktop)
- **Size scale**: 12sp (XSMALL) → 34sp (H1)
- **Line height**: 1.35x multiplier
- **Weights**: Thin, Light, Normal (400), Medium (500), Bold (700)

Access: `BisqTheme.typography.h1Bold`, `BisqTheme.typography.baseRegular`, etc.

## Spacing & Sizing

Defined in `shared/presentation/.../ui/theme/BisqUIConstants.kt`.

- **Base unit**: 12dp (`ScreenPadding`)
- **Scale**: 0, 1, 2, 3, 6, 12, 24, 36, 48, 72, 96 dp
- **Key values**: StaticTopPadding (36dp), ScrollTopPadding (24dp), topBarAvatarSize (38dp), textFieldBorderRadius (6dp), BorderRadius (9dp)

## Component Library

Located at `shared/presentation/.../ui/components/`. Follows Atoms → Molecules → Organisms.

### Atoms (43 components)
Single-purpose, stateless: `BisqText`, `Button`, `TextField`, `Checkbox`, `Switch`, `BisqCard`, `BisqGap`, `BisqHDivider`, `StarRating`, `ProgressBar`, etc.

### Molecules (44 components)
Composites: `TopBar`, `InfoBox`, `BisqDialog`, `ChatInputField`, `UserProfileRow`, `PaymentMethodCard`, `BottomSheet`, `SearchField`, `AmountWithCurrency`, etc.

### Organisms (23 components)
Complex features: `ChatMessageList`, `BisqPagerView`, `MarketFilters`, trade dialogs, reputation popups, etc.

### Layout Wrappers
- `ScrollScaffold` / `StaticScaffold` — full-screen with top bar
- `ScrollLayout` / `StaticLayout` — content containers

---

## Screen Inventory (31 screens)

### Startup Flow
Splash → UserAgreement → Onboarding → CreateProfile

### Main Tabs (TabContainer)
1. **Home** (Dashboard)
2. **Offerbook Markets** (market list → offerbook)
3. **Open Trades** (trade list → trade detail + chat)
4. **More** (settings, profile, payment accounts, reputation, support, resources, backup)

### Create Offer Wizard (7 steps)
Direction → Market → Amount → Price → Payment Method → Settlement Method → Review

### Take Offer Wizard (4 steps)
Amount → Payment Method → Settlement Method → Review

### Educational Guides
- Trade Guide: Overview → Security → Process → Rules
- Wallet Guide: Intro → Download → New Wallet → Receiving

---

## Navigation

- Type-safe routes via `NavRoute` sealed objects with kotlinx-serialization
- Deep linking: `bisq://` URI scheme
- Parametrized routes: `OpenTrade(tradeId)`, `TradeChat(tradeId)`

---

## UX Patterns In Use

| Pattern | Where Used | Notes |
|---------|-----------|-------|
| Bottom tabs | Main navigation | 4 tabs |
| Step wizard | Create/take offer | Back navigation, progress indicator |
| Pull-to-refresh | Trade list, offerbook | Standard mobile pattern |
| Bottom sheet | Payment details, confirmations | Partial screen overlay |
| Floating action button | Chat (jump to bottom) | Context-specific |
| Segmented control | Buy/sell toggle, filters | `SegmentButton` atom |
| Search + filter | Market selection, offers | `SearchField` molecule |
| Swipe/gesture | Not extensively used | Prefer explicit buttons |
| Snackbar/toast | Status messages | `BisqSnackbar` organism |

---

## Design Process for New Features

When porting a desktop feature to mobile:

1. **Read the desktop view code** at `bisq2/apps/desktop/desktop/src/main/java/bisq/desktop/main/content/{feature}/`
2. **Read the desktop CSS** at `bisq2/apps/desktop/desktop/src/main/resources/css/` for visual styling clues
3. **Identify the data and actions** the user needs on that screen
4. **Adapt layout** using the mobile adaptation principles above
5. **Reuse existing components** from the Atoms/Molecules/Organisms library
6. **Follow the MVP pattern**: define a presenter interface, implement the composable as stateless
7. **Consider mobile-specific constraints**: thumb reach zones, one-handed use, limited screen width

---

## Screenshots / Visual References

Place desktop screenshots or mockups in `docs/design/screenshots/` for reference when porting specific features. The agent can also read the desktop code directly to infer layouts.
