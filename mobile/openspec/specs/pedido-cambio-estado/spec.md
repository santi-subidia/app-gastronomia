# Delta for Pedido Cambio Estado

## MODIFIED Requirements

### Requirement: CambiarEstado API Call

The `PATCH /api/pedidos/{id}/estado` endpoint SHALL accept a raw `int` body (the catalog-resolved estado ID) instead of a `CambiarEstadoRequest` wrapper object. The `PedidoApi.cambiarEstado()` method SHALL take `@Body int nuevoEstadoId` as its body parameter.

(Previously: The endpoint accepted a `CambiarEstadoRequest` object with a `nuevoEstado` string field containing the enum's apiValue.)

#### Scenario: Repository resolves estado ID via catalog

- GIVEN a pedido id and an `EstadoPedidoEnum` value
- WHEN `cambiarEstado()` is called on `PedidoRepository`
- THEN the repository resolves the enum's display name to a catalog ID via `CatalogoRepository.resolveEstadoId()`
- AND sends the raw int as the request body

#### Scenario: API sends raw int body

- GIVEN a resolved estado catalog ID (e.g., 3)
- WHEN `PedidoApi.cambiarEstado(id, nuevoEstadoId)` is called
- THEN the HTTP PATCH body is the raw integer value (not a JSON object)

#### Scenario: CambiarEstadoRequest is deleted

- GIVEN the codebase after migration
- WHEN searching for `CambiarEstadoRequest`
- THEN the class file does not exist
- AND no imports or references remain

### Requirement: Repository cambiarEstado Signature

The `PedidoRepository.cambiarEstado()` method SHALL accept `(int id, EstadoPedidoEnum estado)` and internally resolve the catalog ID before making the API call. The interface signature remains unchanged — the resolution is an implementation detail.

(Previously: The repository wrapped the enum's apiValue in a `CambiarEstadoRequest` object.)

#### Scenario: Successful estado change

- GIVEN a connected backend with valid catalog data
- WHEN `cambiarEstado(42, EstadoPedidoEnum.EN_PREPARACION)` is called
- THEN the repository looks up the catalog ID for "EnPreparacion"
- AND sends `PATCH /api/pedidos/42/estado` with body `3` (or the resolved int)
- AND emits SUCCESS with the updated `PedidoDetalleDto`

#### Scenario: Catalog lookup failure

- GIVEN the catalog cache is not yet loaded
- WHEN `cambiarEstado()` is called
- THEN the repository emits an ERROR with a clear message about missing catalog data
- AND no API call is made
