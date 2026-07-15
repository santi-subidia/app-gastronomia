# Main Navigation Graph Specification

## Purpose

Fix `AppBarConfiguration` top-level destinations so that only role-specific home screens are treated as top-level. The current bug uses `navController.getGraph()` which marks ALL destinations as top-level, preventing the up-arrow from appearing on non-home screens.

## Requirements

### Requirement: Explicit Top-Level Destinations

The system MUST configure `AppBarConfiguration` with an explicit set of top-level destination IDs, not the entire navigation graph.

#### Scenario: Only home destinations are top-level

- GIVEN `AppBarConfiguration` is built
- WHEN the builder is configured
- THEN it uses `Set.of(R.id.nav_cajero_home, R.id.nav_cocina_home, R.id.nav_repartidor_home)` as top-level destinations
- AND `navController.getGraph()` is NOT used as the top-level set

#### Scenario: Up arrow on non-home destination

- GIVEN the user navigates from a home screen to a detail screen
- AND the detail screen is NOT in the top-level set
- WHEN the toolbar is updated via `NavigationUI.setupActionBarWithNavController()`
- THEN the toolbar shows an up-arrow (not the hamburger icon)

#### Scenario: Hamburger on home destination

- GIVEN the user is on their role's home screen (e.g., `nav_cajero_home`)
- AND the home screen IS in the top-level set
- WHEN the toolbar is updated
- THEN the toolbar shows the hamburger icon (drawer toggle)

#### Scenario: Up arrow navigates up

- GIVEN the user is on a non-home destination with an up-arrow
- WHEN the user taps the up-arrow
- THEN `NavController.navigateUp()` is called
- AND the user returns to the parent destination
