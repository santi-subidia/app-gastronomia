# Design: Navigation UX Improvements

## Technical Approach

Gate `MainActivity.onCreate()` behind a splash overlay (sibling `LinearLayout` in `activity_main.xml`'s `FrameLayout`). On cold start, decode the JWT payload locally using only `android.util.Base64` + `org.json.JSONObject` — extract `exp`, compare to `System.currentTimeMillis()/1000`, resolve the persisted `rolNombre` to a home destination ID, and navigate with `popUpTo` clearing the login entry. Wire `BottomNavigationView` programmatically via `menu.clear()` + `menu.add()` using nav-graph destination IDs as menu item IDs. Add `NavigationView` to the default `DrawerLayout` (already exists empty), bind header name/role from `TokenManager`. Fix `AppBarConfiguration` to use explicit `Set.of(nav_cajero_home, nav_cocina_home, nav_repartidor_home)`. Unify logout from toolbar overflow and drawer into a single `performLogout()` method that clears `EncryptedSharedPreferences`, resets `SessionManager` LiveData, and navigates to login with full back-stack clearing.

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| JWT decode library | None — `Base64.decode` + `JSONObject.optLong("exp")` | Zero new dependencies; both classes available on minSdk 24; 4-line implementation |
| `saveToken` signature | Add `nombreUsuario` as 4th parameter | Single call site (`AuthRepositoryImpl`), no separate setter needed |
| Bottom nav population | Programmatic: `menu.clear()` then `menu.add(id, title).setIcon(drawable)` | One code path, no XML per role, IDs guaranteed to match nav graph |
| Splash visibility | Overlay `LinearLayout` sibling to `DrawerLayout` in `activity_main.xml` | No Activity transition, same view hierarchy, toggle with `setVisibility` |
| Drawer on w600dp | Overlay only (already true — `NavigationView` inside `DrawerLayout`) | Spec requires mobile-first; current w600dp is correct, w1240dp gets `DrawerLayout` added |
| Logout navigation | `NavOptions.popUpTo(R.id.mobile_navigation, inclusive=true)` then `navigate(nav_login)` | Clears entire graph including stale fragments; no back-press to old screens |

## Data Flow

**Auto-login**: `onCreate` → show splash → `tokenManager.hasToken()` → if no, navigate `nav_login` and hide splash. If yes, `decodeTokenExp()` → if expired/malformed, `clearToken()` → `nav_login`. If valid, `getRole()` maps to home destination (`Cajero`→`nav_cajero_home`, `Cocina`→`nav_cocina_home`, `Repartidor`→`nav_repartidor_home`, unknown→`nav_login` with warning). Navigate with `popUpTo(mobile_navigation, true)` so back from home exits the app. After navigation: `configureBottomNav(role)`, `bindDrawerHeader()`, hide splash.

**Logout**: `performLogout()` calls `tokenManager.clearToken()` (clears all prefs including token, role, name, userId), `sessionManager.consume()` (resets 401 flag), then `navController.navigate(nav_login, popUpTo=mobile_navigation inclusive)`. Called from both toolbar `onOptionsItemSelected` and drawer `onNavigationItemSelected`.

## File Changes

| File | Action | Key Change |
|------|--------|------------|
| `core/TokenManager.java` | Modify | Add `getNombreUsuario()`, `decodeTokenExp()` methods; extend `saveToken` with 4th param `nombreUsuario` |
| `core/TokenManagerImpl.java` | Modify | Add `KEY_USER_NAME` pref key; implement base64+JSON JWT expiry decode in `decodeTokenExp()` (split on `.`, decode payload, check `exp`) |
| `data/repository/AuthRepositoryImpl.java` | Modify | Pass `body.getUsuarioNombre()` as 4th arg to `saveToken()` |
| `MainActivity.java` | Modify | Inject `TokenManager`; add splash gate in `onCreate` (isCheckingSession flag → toggle splash visibility); replace `AppBarConfiguration(navController.getGraph())` with explicit 3-destination set + `setOpenableLayout(drawer)`; wire bottom nav with `configureBottomNav(role)` + `NavigationUI.setupWithNavController()`; add `ActionBarDrawerToggle`; add `onCreateOptionsMenu`/`onOptionsItemSelected` with `main_options_menu`; add `performLogout()`; bind drawer header |
| `res/layout/activity_main.xml` | Modify | Add `NavigationView` inside `DrawerLayout` (copy from w600dp); add `splash_layout` `LinearLayout` as sibling to `DrawerLayout` with `visibility="gone"` |
| `res/layout/app_bar_main.xml` | Modify | Remove `FloatingActionButton` and its inner `CoordinatorLayout` wrapper |
| `res/layout/nav_header_main.xml` | Modify | Add IDs: `header_name` to title `TextView`, `header_role` to subtitle `TextView` |
| `res/layout-w1240dp/activity_main.xml` | Modify | Wrap content in `DrawerLayout` + add `NavigationView` (match default layout) |
| `res/menu/bottom_navigation.xml` | Modify | Define placeholder `<group>` for IDs; actual items populated programmatically |
| `res/menu/navigation_drawer.xml` | Modify | Add `nav_configuracion` (Configuración) and `nav_cerrar_sesion` (Cerrar sesión) items |
| `res/menu/main_options_menu.xml` | Create | Single item: `action_logout` (Cerrar sesión) with `showAsAction="never"` |
| `res/menu/overflow.xml` | Delete | Replaced by `main_options_menu.xml` |
| `res/drawable/ic_home_24dp.xml` | Create | Vector drawable — Material home icon |
| `res/drawable/ic_pedidos_24dp.xml` | Create | Vector drawable — Material receipt/list icon |
| `res/drawable/ic_productos_24dp.xml` | Create | Vector drawable — Material inventory icon |
| `res/drawable/ic_caja_24dp.xml` | Create | Vector drawable — Material point-of-sale icon |
| `res/drawable/ic_mapa_24dp.xml` | Create | Vector drawable — Material map icon |
| `res/values/strings.xml` | Modify | Add: `drawer_config`, `drawer_logout`, `menu_logout`, `splash_title`, `role_cajero`, `role_cocina`, `role_repartidor`, `header_fallback` ("Usuario") |

## Testing Strategy

| Layer | What | How |
|-------|------|-----|
| Unit | `decodeTokenExp()` — valid, expired, malformed JWTs | JUnit + Robolectric with mock `SharedPreferences` |
| Unit | Role → destination mapping and unknown-role fallback | Extract mapping to testable method |
| Integration | Bottom nav menu items per role | `ActivityScenario`, verify `menu.size()` and item IDs |
| Integration | Logout clears state + navigates to login | `ActivityScenario`, trigger menu click, assert `tokenManager.getToken() == null`, assert current destination is `nav_login` |
| E2E | Up-arrow on non-home, hamburger on home | Instrumentation, verify toolbar navigation icon |

## Open Questions

- [ ] Icon choices for `ic_productos_24dp` and `ic_caja_24dp` — use Material `inventory_2` and `point_of_sale` unless designer provides custom assets
- [ ] Splash indicator: show `ProgressBar` or keep static branding? (Static is sufficient for <100ms local decode)
