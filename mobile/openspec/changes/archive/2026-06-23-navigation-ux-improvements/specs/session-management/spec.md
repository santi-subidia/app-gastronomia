# Session Management Specification

## Purpose

Extend `TokenManager` to decode JWT `exp` claim for local expiry validation and persist `nombreUsuario` from `LoginResponse` for display in the navigation drawer header.

## Requirements

### Requirement: JWT Expiry Decoding

The system MUST provide a method to decode the JWT `exp` (expiration) claim locally without network calls.

#### Scenario: Decode valid non-expired token

- GIVEN a valid JWT string with an `exp` claim in the future
- WHEN `TokenManager.decodeTokenExp()` is called
- THEN the method returns the expiration timestamp
- AND no exception is thrown

#### Scenario: Decode expired token

- GIVEN a JWT string with an `exp` claim in the past
- WHEN `TokenManager.decodeTokenExp()` is called
- THEN the method returns the expiration timestamp
- AND the caller can compare it to `System.currentTimeMillis()` to determine expiry

#### Scenario: Decode malformed JWT

- GIVEN a string that is not a valid JWT
- WHEN `TokenManager.decodeTokenExp()` is called
- THEN the method throws or returns an error indicator
- AND the caller treats the token as invalid

### Requirement: Persist User Name from Login

The system MUST store the `nombreUsuario` field from `LoginResponse` in `EncryptedSharedPreferences` during login.

#### Scenario: Save name on successful login

- GIVEN the backend returns a `LoginResponse` with `nombreUsuario` = "Juan Pérez"
- WHEN the login success handler processes the response
- THEN `TokenManager.saveToken()` (or equivalent) persists the name
- AND `TokenManager.getNombreUsuario()` returns "Juan Pérez"

#### Scenario: Retrieve persisted name

- GIVEN a name was persisted during a previous login session
- WHEN `TokenManager.getNombreUsuario()` is called
- THEN the method returns the stored name string

#### Scenario: Name cleared on logout

- GIVEN the user logs out
- WHEN `TokenManager.clearToken()` is called
- THEN the persisted name is also cleared
- AND `getNombreUsuario()` returns null or empty

### Requirement: Role Persistence

The system MUST store and retrieve the user's role via `TokenManager.getRole()` using `EncryptedSharedPreferences`.

#### Scenario: Role available after login

- GIVEN the user logs in with role "cajero"
- WHEN `TokenManager.getRole()` is called
- THEN it returns "cajero"
