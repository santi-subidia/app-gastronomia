# Navigation Drawer Specification

## Purpose

Secondary navigation via `NavigationView` inside `DrawerLayout`, with a real user profile header (name + role), configuration access, and logout. Mobile-first: no permanent drawer on tablets.

## Requirements

### Requirement: DrawerLayout with NavigationView

The system MUST include a `NavigationView` as a child of `DrawerLayout` in `res/layout/activity_main.xml` (default layout, not just w600dp).

#### Scenario: Drawer opens with hamburger icon

- GIVEN the user is on a home destination
- AND the toolbar displays a hamburger icon
- WHEN the user taps the hamburger icon
- THEN the `DrawerLayout` opens
- AND the `NavigationView` is visible with menu items

#### Scenario: Drawer closes on destination selection

- GIVEN the drawer is open
- WHEN the user selects a menu item
- THEN the drawer closes
- AND the corresponding navigation action executes

### Requirement: Real User Header

The system MUST bind the user's real name and role to `res/layout/nav_header_main.xml`, replacing hardcoded "Android Studio" text.

#### Scenario: Header shows name from TokenManager

- GIVEN `TokenManager.getNombreUsuario()` returns a non-empty string
- WHEN the drawer header is inflated
- THEN the header displays the user's name

#### Scenario: Header shows role label

- GIVEN `TokenManager.getRole()` returns a known role
- WHEN the drawer header is inflated
- THEN the header displays a human-readable role label (e.g., "Cajero", "Cocina", "Repartidor")

#### Scenario: Header with missing name

- GIVEN `TokenManager.getNombreUsuario()` returns null or empty
- WHEN the drawer header is inflated
- THEN the header displays a fallback (e.g., "Usuario")

### Requirement: Drawer Menu Items

The system MUST populate `res/menu/navigation_drawer.xml` with secondary navigation items.

#### Scenario: Drawer contains Configuración

- GIVEN the drawer menu is inflated
- THEN it contains a "Configuración" menu item

#### Scenario: Drawer contains Cerrar sesión

- GIVEN the drawer menu is inflated
- THEN it contains a "Cerrar sesión" menu item

### Requirement: Mobile-First Drawer Behavior

The system MUST NOT use a permanent drawer on any screen size, including tablets (w600dp).

#### Scenario: Drawer is overlay on tablet

- GIVEN the app runs on a tablet (width >= 600dp)
- WHEN the user opens the drawer
- THEN the drawer overlays the content (not permanent/side-by-side)
- AND the w600dp layout variant does not include a permanent `NavigationView`
