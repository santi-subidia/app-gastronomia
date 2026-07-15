# Bottom Navigation Specification

## Purpose

Role-aware primary tab navigation using `BottomNavigationView`. Each role sees a different set of tabs, wired through `NavigationUI.setupWithNavController()` with per-tab back stack support.

## Requirements

### Requirement: Role-Conditional Menu Items

The system MUST populate `res/menu/bottom_navigation.xml` with menu items appropriate to the user's role, as determined by `TokenManager.getRole()`.

#### Scenario: Cajero sees 4 tabs

- GIVEN the user's role is "cajero"
- WHEN the bottom navigation is configured
- THEN the menu contains: Home, Pedidos, Productos, Caja
- AND each item has a valid icon from `res/drawable`

#### Scenario: Cocina sees 2 tabs

- GIVEN the user's role is "cocina"
- WHEN the bottom navigation is configured
- THEN the menu contains: Home, Pedidos

#### Scenario: Repartidor sees 3 tabs

- GIVEN the user's role is "repartidor"
- WHEN the bottom navigation is configured
- THEN the menu contains: Home, Pedidos, Mapa

### Requirement: NavigationUI Wiring

The system MUST call `NavigationUI.setupWithNavController(bottomNavigationView, navController)` to connect tab selection to the navigation graph.

#### Scenario: Tab selection navigates to destination

- GIVEN the bottom navigation is wired with `NavigationUI`
- AND the user is on the Home tab
- WHEN the user taps the Pedidos tab
- THEN `NavController` navigates to the Pedidos destination
- AND the tab selection state updates visually

#### Scenario: Per-tab back stack preservation

- GIVEN the user navigates within the Pedidos tab to a detail screen
- AND the user switches to the Home tab
- WHEN the user switches back to the Pedidos tab
- THEN the Pedidos detail screen is still visible (back stack preserved)

#### Scenario: Back button on root tab exits app

- GIVEN the user is on any role's home tab (root destination)
- WHEN the user presses the system back button
- THEN the app exits (or moves to background)
- AND no fragment navigation occurs

### Requirement: Missing Icon Graceful Handling

The system SHOULD handle missing drawable icons gracefully without crashing.

#### Scenario: Icon resource not found

- GIVEN a menu item references a drawable that does not exist
- WHEN the bottom navigation is inflated
- THEN the system catches the `NotFoundException`
- AND uses a default placeholder icon or skips the item
- AND the app does not crash
