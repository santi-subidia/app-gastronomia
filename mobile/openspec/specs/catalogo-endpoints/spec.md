# Catalogo Endpoints Specification

## Purpose

Provides catalog lookup endpoints for resolving display names (estado, metodoPago, metodoVenta) to their backend integer IDs. This infrastructure is required by the estado change flow and replaces hardcoded enum apiValue strings with catalog-resolved IDs.

## Requirements

### Requirement: CatalogoDto

The `CatalogoItemDto` SHALL represent a single catalog entry with `id` (int) and `nombre` (String). The `CatalogoResponseDto` SHALL represent the full list response as `List<CatalogoItemDto>`.

#### Scenario: Deserialize catalog item

- GIVEN a JSON `{"id": 3, "nombre": "EnPreparacion"}`
- WHEN Gson deserializes it into `CatalogoItemDto`
- THEN `id == 3` and `nombre == "EnPreparacion"`

### Requirement: Catalog API Interfaces

Three Retrofit API interfaces SHALL be created:
- `EstadosPedidoApi` with `GET api/catalogo/estados-pedido` returning `Call<List<CatalogoItemDto>>`
- `MetodoPagoApi` with `GET api/catalogo/metodos-pago` returning `Call<List<CatalogoItemDto>>`
- `MetodoVentaApi` with `GET api/catalogo/metodos-venta` returning `Call<List<CatalogoItemDto>>`

#### Scenario: EstadosPedidoApi declaration

- GIVEN the `EstadosPedidoApi` interface
- WHEN `getEstados()` is called
- THEN it sends GET to `api/catalogo/estados-pedido`
- AND returns `Call<List<CatalogoItemDto>>`

#### Scenario: MetodoPagoApi declaration

- GIVEN the `MetodoPagoApi` interface
- WHEN `getMetodosPago()` is called
- THEN it sends GET to `api/catalogo/metodos-pago`
- AND returns `Call<List<CatalogoItemDto>>`

#### Scenario: MetodoVentaApi declaration

- GIVEN the `MetodoVentaApi` interface
- WHEN `getMetodosVenta()` is called
- THEN it sends GET to `api/catalogo/metodos-venta`
- AND returns `Call<List<CatalogoItemDto>>`

### Requirement: CatalogoRepository

The `CatalogoRepository` SHALL provide methods to load and cache all three catalogs, and resolve a display name to its catalog ID. It SHALL expose:
- `loadEstadosPedido()`: fetches and caches estado catalog
- `loadMetodosPago()`: fetches and caches pago catalog
- `loadMetodosVenta()`: fetches and caches venta catalog
- `resolveEstadoId(String nombre)`: returns the int ID for a given estado nombre
- `isReady()`: returns true when all catalogs are loaded

#### Scenario: Load all catalogs

- GIVEN the backend is reachable
- WHEN `loadEstadosPedido()`, `loadMetodosPago()`, and `loadMetodosVenta()` are called
- THEN each fetches its respective endpoint
- AND caches the results in memory

#### Scenario: Resolve estado ID by name

- GIVEN the estado catalog is loaded with `{id: 3, nombre: "EnPreparacion"}`
- WHEN `resolveEstadoId("EnPreparacion")` is called
- THEN it returns `3`

#### Scenario: Resolve fails when catalog not loaded

- GIVEN the estado catalog has not been loaded
- WHEN `resolveEstadoId("EnPreparacion")` is called
- THEN it throws an `IllegalStateException` or returns an error indicator
- AND the caller can handle the failure gracefully

#### Scenario: isReady reflects cache state

- GIVEN no catalogs have been loaded
- WHEN `isReady()` is called
- THEN it returns false

- GIVEN all three catalogs are loaded
- WHEN `isReady()` is called
- THEN it returns true

### Requirement: DI Wiring

The `NetworkModule` (or equivalent Hilt module) SHALL provide the three catalog API interfaces and the `CatalogoRepository` implementation as injectable dependencies.

#### Scenario: Hilt provides catalog APIs

- GIVEN the app's DI graph is initialized
- WHEN `CatalogoRepository` is injected
- THEN it receives instances of `EstadosPedidoApi`, `MetodoPagoApi`, and `MetodoVentaApi`
