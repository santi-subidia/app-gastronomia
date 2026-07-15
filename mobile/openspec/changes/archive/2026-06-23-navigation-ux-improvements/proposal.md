# Proposal: Navigation UX Improvements

## Intent
Eliminate friction in the app's navigation and login flow. Users currently see Login on every cold start despite holding a valid JWT, the bottom nav and drawer are non-functional templates, and the toolbar never shows an up-arrow. This change makes navigation role-aware, session-aware, and consistent with Material Design.

## Scope

### In Scope
- Auto-login with JWT expiry check and splash screen gate
- Role-based BottomNavigation items and wiring
- NavigationDrawer with real user name, role, and logout
- Fix AppBarConfiguration top-level bug
- Toolbar overflow menu with full logout cleanup
- Remove unused template FAB

### Out of Scope
- Token refresh endpoint (forced re-login on expiry)
- Permanent drawer on tablets (mobile-first, hidden on all sizes)
- Backend changes beyond consuming existing `nombreUsuario`
- Deep-link handling or notification navigation

## Capabilities

### New Capabilities
- `auto-login`: Splash-gated session validation and role-based routing
- `bottom-navigation`: Role-aware primary tab navigation
- `navigation-drawer`: Secondary navigation with user profile header
- `toolbar-logout`: Global logout action with full state cleanup

### Modified Capabilities
- `session-management`: Extend TokenManager to decode JWT `exp` and persist `nombreUsuario`
- `main-navigation-graph`: AppBarConfiguration top-level destination fix

## Approach
1. **TokenManager** — add `decodeTokenExp()` and `getSavedUserName()`/`getSavedRole()`; update `LoginResponse` consumer to persist `nombreUsuario`.
2. **MainActivity.onCreate()** — show splash layout, check token + expiry, route to role-appropriate home fragment; if invalid, show login.
3. **BottomNavigation** — populate `bottom_navigation.xml` with role-conditional item sets; call `NavigationUI.setupWithNavController()`.
4. **Drawer** — add `NavigationView` to default `activity_main.xml`, populate `navigation_drawer.xml`, bind header name/role from TokenManager, wire `ActionBarDrawerToggle`.
5. **AppBarConfiguration** — replace `navController.getGraph()` with explicit `Set<R.id.nav_cajero_home, R.id.nav_cocina_home, R.id.nav_repartidor_home>`.
6. **Toolbar** — add `main_options_menu.xml`, handle `onOptionsItemSelected` → clear token + caches + `LiveData`, navigate to login with `popUpTo` root.
7. **Cleanup** — remove FAB from `app_bar_main.xml`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `MainActivity.java` | Modified | Splash gate, auto-login, drawer toggle, options menu, nav wiring |
| `TokenManager.java` / `Impl` | Modified | Decode JWT `exp`, expose name/role getters |
| `res/layout/activity_main.xml` | Modified | Add `NavigationView` inside `DrawerLayout` |
| `res/layout/app_bar_main.xml` | Modified | Remove template FAB |
| `res/menu/bottom_navigation.xml` | New | Role-conditional menu items with icons |
| `res/menu/navigation_drawer.xml` | New | Secondary items: Configuración, Cerrar sesión |
| `res/menu/main_options_menu.xml` | New | Overflow logout action |
| `res/layout/nav_header_main.xml` | Modified | Bind real user name and role |
| `res/navigation/mobile_navigation.xml` | Modified | Ensure home fragments are graph roots |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Login flicker if splash dismiss is racy | Low | Gate navigation with a single `isCheckingSession` flag; UI stays on splash until decision |
| Drawer/bottom nav back-stack conflicts | Med | Use `NavigationUI` defaults; test `onBackPressed` on each role root |
| Token decode throws on malformed JWT | Low | Wrap decode in `try/catch`, treat as invalid |
| Logout leaves stale ViewModel state | Med | Clear `SessionManager` + `TokenManager` + instruct `NavController` to pop to root |

## Rollback Plan
- Revert `MainActivity.java` to current version (restores template behavior)
- Restore empty menu XMLs and remove new ones
- Revert TokenManager changes; login will always show (safe degraded state)

## Dependencies
- `nombreUsuario` is already present in `LoginResponse` (backend contract already satisfied)
- Existing icons in `res/drawable` for bottom nav items; add missing ones if absent

## Success Criteria
- [ ] Cold start with valid JWT routes directly to role home without login flicker
- [ ] Cold start with expired/missing JWT shows login
- [ ] Bottom nav shows correct items per role and switches tabs
- [ ] Drawer opens with hamburger, shows real name + role, and logout clears all state
- [ ] Up arrow appears on non-home destinations; home shows hamburger
- [ ] No template FAB visible
