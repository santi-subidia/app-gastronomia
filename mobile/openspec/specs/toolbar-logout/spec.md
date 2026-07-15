# Toolbar and Logout Specification

## Purpose

Global toolbar overflow menu with logout action. Logout MUST clear all state: token, session caches, LiveData, and navigate to login with full back stack reset.

## Requirements

### Requirement: Toolbar Overflow Menu

The system MUST provide `res/menu/main_options_menu.xml` with a "Cerrar sesión" action in the toolbar overflow.

#### Scenario: Overflow menu shows logout

- GIVEN the user is on any screen with a toolbar
- WHEN the user taps the overflow (three-dot) menu
- THEN the menu displays "Cerrar sesión"

### Requirement: Full State Cleanup on Logout

The system MUST perform complete cleanup when the user initiates logout from either the toolbar overflow or the navigation drawer.

#### Scenario: Logout clears token

- GIVEN the user triggers logout
- WHEN the logout handler executes
- THEN `TokenManager.clearToken()` is called
- AND the encrypted shared preferences no longer contain the token

#### Scenario: Logout clears SessionManager state

- GIVEN the user triggers logout
- WHEN the logout handler executes
- THEN `SessionManager` is cleared of all cached data
- AND all LiveData observers are notified of the cleared state

#### Scenario: Logout resets navigation back stack

- GIVEN the user triggers logout from a deep destination (e.g., order detail)
- WHEN the logout handler executes
- THEN `NavController` pops the back stack to the root
- AND the login screen is shown as the only destination

#### Scenario: Logout from toolbar overflow

- GIVEN the user selects "Cerrar sesión" from the toolbar overflow
- WHEN the menu item is handled
- THEN full state cleanup executes (token, session, LiveData, nav stack)
- AND the login screen is shown

#### Scenario: Logout from navigation drawer

- GIVEN the user selects "Cerrar sesión" from the navigation drawer
- WHEN the menu item is handled
- THEN full state cleanup executes (token, session, LiveData, nav stack)
- AND the login screen is shown

### Requirement: Remove Unused FAB

The system MUST NOT display the template FloatingActionButton in `res/layout/app_bar_main.xml`.

#### Scenario: FAB is removed from layout

- GIVEN `app_bar_main.xml` is inflated
- WHEN the layout is rendered
- THEN no `FloatingActionButton` is present in the view hierarchy
