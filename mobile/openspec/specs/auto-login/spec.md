# Auto-Login Specification

## Purpose

Splash-gated session validation on cold start. The app MUST check for a valid, non-expired JWT token and route to the appropriate role-based home screen without showing the login form. If no valid token exists, the login screen is shown.

## Requirements

### Requirement: Splash Screen Gate

The system MUST display a splash layout immediately on `MainActivity.onCreate()` before any navigation decision. The splash screen remains visible until token validation completes and navigation is resolved.

#### Scenario: Cold start with valid non-expired token

- GIVEN the app is launched from cold start
- AND `TokenManager.hasToken()` returns true
- AND the JWT `exp` claim is in the future
- WHEN `MainActivity.onCreate()` executes
- THEN the splash layout is shown first
- AND the system decodes the JWT and validates expiry
- AND the system navigates to the role-appropriate home destination
- AND the splash layout is dismissed after navigation

#### Scenario: Cold start with expired token

- GIVEN the app is launched from cold start
- AND `TokenManager.hasToken()` returns true
- AND the JWT `exp` claim is in the past
- WHEN token validation executes
- THEN the system treats the token as invalid
- AND the system clears the expired token via `TokenManager.clearToken()`
- AND the login screen is shown

#### Scenario: Cold start with no token

- GIVEN the app is launched from cold start
- AND `TokenManager.hasToken()` returns false
- WHEN token validation executes
- THEN the login screen is shown without delay

#### Scenario: Malformed JWT token

- GIVEN the app is launched from cold start
- AND `TokenManager.hasToken()` returns true
- AND the stored JWT cannot be decoded (malformed)
- WHEN token validation executes
- THEN the decode is wrapped in try/catch
- AND the system treats the token as invalid
- AND the login screen is shown

### Requirement: Role-Based Routing

The system MUST route the user to the correct home destination based on their persisted role after successful token validation.

#### Scenario: Routing Cajero role

- GIVEN a valid token with role "cajero"
- WHEN role-based routing executes
- THEN the system navigates to `R.id.nav_cajero_home`

#### Scenario: Routing Cocina role

- GIVEN a valid token with role "cocina"
- WHEN role-based routing executes
- THEN the system navigates to `R.id.nav_cocina_home`

#### Scenario: Routing Repartidor role

- GIVEN a valid token with role "repartidor"
- WHEN role-based routing executes
- THEN the system navigates to `R.id.nav_repartidor_home`

#### Scenario: Unknown role

- GIVEN a valid token with an unrecognized role value
- WHEN role-based routing executes
- THEN the system defaults to the login screen
- AND logs a warning

### Requirement: No Login Flicker

The system MUST prevent the login screen from appearing briefly before the splash gate resolves.

#### Scenario: Single navigation decision

- GIVEN `MainActivity` is created
- WHEN the session check begins
- THEN a single `isCheckingSession` flag is set
- AND the UI remains on the splash layout until the decision is complete
- AND the login screen is only shown if the decision is "no valid session"
