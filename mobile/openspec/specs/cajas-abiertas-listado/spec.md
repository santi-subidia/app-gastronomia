# Cajas Abiertas Listado Specification

## Purpose

Provides the capability to query all currently open cajas via a dedicated endpoint, enabling the UI to display active cash registers without filtering the full list client-side.

## Requirements

### Requirement: GET /api/cajas/abiertas Endpoint

The `CajaApi` interface SHALL declare a `getCajasAbiertas()` method that maps to `GET api/cajas/abiertas` and returns `Call<List<CajaDto>>`.

#### Scenario: API method declaration

- GIVEN the `CajaApi` interface
- WHEN `getCajasAbiertas()` is called
- THEN it sends a GET request to `api/cajas/abiertas`
- AND returns a `Call<List<CajaDto>>`

### Requirement: CajaRepository getCajasAbiertas Method

The `CajaRepository` interface SHALL declare `LiveData<UiState<List<CajaDto>>> getCajasAbiertas()` and `LiveData<UiState<List<CajaDto>>> getCajasAbiertasState()`. The implementation SHALL follow the existing pattern: emit LOADING, call the API, then post SUCCESS or ERROR.

#### Scenario: Repository emits loading then success

- GIVEN the backend returns a list of open cajas
- WHEN `getCajasAbiertas()` is called on `CajaRepository`
- THEN `_cajasAbiertasState` is set to LOADING
- AND on success, SUCCESS is posted with the list

#### Scenario: Repository emits error on network failure

- GIVEN no network connectivity
- WHEN `getCajasAbiertas()` is called
- THEN ERROR is posted with "No hay conexión a internet"

#### Scenario: Repository emits error on server error

- GIVEN the server returns a 500 response
- WHEN `getCajasAbiertas()` is called
- THEN ERROR is posted with the parsed `mensaje` from the error body

### Requirement: Empty Result Handling

When no cajas are open, the endpoint SHALL return an empty list (not null). The repository SHALL emit SUCCESS with an empty list.

#### Scenario: No open cajas

- GIVEN all cajas are closed
- WHEN `getCajasAbiertas()` is called
- THEN SUCCESS is posted with an empty list
- AND the UI can render "no hay cajas abiertas"
