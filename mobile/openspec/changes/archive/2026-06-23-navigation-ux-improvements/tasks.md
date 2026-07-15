# Tasks: Navigation UX Improvements

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 500–560 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (session) → PR 2 (XML) → PR 3 (MainActivity) → PR 4 (tests) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Session mgmt: TokenManager + AuthRepository | PR 1 | Base: main. Self-contained interface+impl changes. |
| 2 | XML resources: layouts, menus, drawables, strings | PR 2 | Base: main. No Java code; purely resource changes. |
| 3 | MainActivity wiring: splash, auto-login, nav, drawer, logout | PR 3 | Base: PR 2 branch. Depends on XML IDs from PR 2. |
| 4 | Tests: unit + integration + E2E | PR 4 | Base: PR 3 branch. Depends on runtime behavior from PR 3. |

## Phase 1: Session Management (TokenManager Interface + Impl)

- [x] 1.1 **TokenManager.java** — Add `decodeTokenExp()` returning `long?`; add `getNombreUsuario()`; change `saveToken(token, rolNombre, userId)` to `saveToken(token, rolNombre, userId, nombreUsuario)`
- [x] 1.2 **TokenManagerImpl.java** — Add `KEY_USER_NAME`; implement `decodeTokenExp()` via `Base64.decode` payload + `JSONObject.optLong("exp")` with try/catch; implement `getNombreUsuario()`; update `saveToken()` 4th param; `clearToken()` already clears all keys
- [x] 1.3 **AuthRepositoryImpl.java** — Pass `body.getUsuarioNombre()` as 4th arg to `saveToken()`

## Phase 2: XML Resources

- [x] 2.1 **activity_main.xml** — Add `NavigationView` inside `DrawerLayout`; add splash `LinearLayout` as sibling to `DrawerLayout` with `android:visibility="gone"`
- [x] 2.2 **layout-w1240dp/activity_main.xml** — Wrap content in `DrawerLayout`; add `NavigationView` child; add splash layout
- [x] 2.3 **app_bar_main.xml** — Remove FAB block (CoordinatorLayout + FloatingActionButton)
- [x] 2.4 **nav_header_main.xml** — Add `android:id="@+id/header_name"` to title `TextView`; add `android:id="@+id/header_role"` to subtitle `TextView`
- [x] 2.5 **bottom_navigation.xml** — Add placeholder `<group>` items; items populated programmatically per role
- [x] 2.6 **navigation_drawer.xml** — Add `<item android:id="@+id/nav_configuracion"` and `<item android:id="@+id/nav_cerrar_sesion"`
- [x] 2.7 **main_options_menu.xml** — Create: single `action_logout` item with `showAsAction="never"`
- [x] 2.8 **overflow.xml** — Delete (replaced by `main_options_menu.xml`)
- [x] 2.9 **drawable/ic_home_24dp.xml**, **ic_pedidos_24dp.xml**, **ic_productos_24dp.xml**, **ic_caja_24dp.xml**, **ic_mapa_24dp.xml** — Create Material vector drawables
- [x] 2.10 **strings.xml** — Add: `drawer_config`, `drawer_logout`, `menu_logout`, `splash_title`, `role_cajero`, `role_cocina`, `role_repartidor`, `header_fallback`

## Phase 3: MainActivity Wiring

- [x] 3.1 **MainActivity.java** — Inject `TokenManager`; add `isCheckingSession` flag; show splash, check `hasToken()`, decode expiry, route role→home or login
- [x] 3.2 **MainActivity.java** — `configureBottomNav(role)`: `menu.clear()` + `menu.add(id, title).setIcon()` per role; `NavigationUI.setupWithNavController()`
- [x] 3.3 **MainActivity.java** — Fix `AppBarConfiguration` to `Set.of(nav_cajero_home, nav_cocina_home, nav_repartidor_home)` + `setOpenableLayout(drawer)`
- [x] 3.4 **MainActivity.java** — Add `ActionBarDrawerToggle`; `bindDrawerHeader()` binds name+role from TokenManager; handle drawer `nav_cerrar_sesion` item
- [x] 3.5 **MainActivity.java** — `onCreateOptionsMenu` inflate `main_options_menu`; `onOptionsItemSelected` handle `action_logout`
- [x] 3.6 **MainActivity.java** — `performLogout()`: `clearToken()`, `sessionManager.consume()`, navigate to `nav_login` with `popUpTo(mobile_navigation, inclusive=true)`

## Phase 4: Testing

- [x] 4.1 Unit: `decodeTokenExp()` — valid JWT, expired JWT, malformed JWT, null token
- [x] 4.2 Unit: Role→destination mapping + unknown-role fallback to login with warning
- [x] 4.3 Integration: Bottom nav items per role — Cajero (4), Cocina (2), Repartidor (3)
- [x] 4.4 Integration: Logout clears token + navigates to login via toolbar and drawer
- [x] 4.5 E2E: Up-arrow on non-home destination; hamburger on home; no FAB visible

## Implementation Order

1. **Phase 1 first** — TokenManager is a dependency for everything else.
2. **Phase 2 second** — XML IDs must exist before Java code references them (except navigation IDs which already exist in the nav graph).
3. **Phase 3 third** — MainActivity depends on TokenManager methods and XML layout IDs.
4. **Phase 4 last** — Tests verify runtime behavior of all phases combined.

### Next Step

Estimated change is **500–560 lines** (High risk). Chained PRs are recommended. **Ask the user** which chain strategy to use: stacked PRs to main, feature-branch-chain, or size:exception.
