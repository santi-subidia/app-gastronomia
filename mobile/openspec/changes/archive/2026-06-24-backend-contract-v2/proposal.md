# Proposal: backend-contract-v2

## Intent
Adapt the Android client to the backend's simplified v2 contract. Remove deprecated fields from DTOs, replace estado name-based bodies with raw int IDs resolved via catalog endpoints, introduce a UI-level model for pedido details, and add a new cajas endpoint.

## Scope

### In Scope
- Simplify `CrearPedidoRequest` / `CrearDetalleRequest` and introduce `DetalleLine` UI model
- Change estado PATCH to raw `int` body with catalog-based ID resolution
- Remove `sector` from demora DTOs; remove `usuarioAperturaId` / `usuarioCierreId` from caja DTOs
- Recreate catalog endpoints (`CatalogoDto`, APIs, `CatalogoRepository`)
- Add `GET /api/cajas/abiertas`
- Rename SignalR handler `PosicionGPS` → `PosicionGPSActualizada`
- Maintain all existing `CrearPedido` validations

### Out of Scope
- Backend contract changes (already deployed)
- New business logic or UI flows beyond DTO/model alignment
- Refactoring unrelated repositories or fragments

## Capabilities

### New Capabilities
- `catalogo-endpoints`: Catalog DTOs, APIs (`EstadosPedidoApi`, `MetodoPagoApi`, `MetodoVentaApi`), and `CatalogoRepository` for lookup resolution
- `cajas-abiertas-listado`: New `GET api/cajas/abiertas` endpoint, repository method, and wiring

### Modified Capabilities
- `pedido-creacion`: Simplified `CrearPedidoRequest`/`CrearDetalleRequest`; `DetalleLine` UI model mapped to DTO on submit
- `pedido-cambio-estado`: Delete `CambiarEstadoRequest`; use raw `int` body with `CatalogoRepository.resolveEstadoId()`
- `demora-gestion`: Remove `sector` from `CrearDemoraRequest` and `ActualizarDemoraRequest`
- `caja-operaciones`: Remove `usuarioAperturaId` from `AbrirCajaRequest`; remove `usuarioCierreId` from `CerrarCajaRequest`
- `signalr-handlers`: Rename `PosicionGPS` handler to `PosicionGPSActualizada`

## Approach
Implement in dependency order:
1. **Catalog infrastructure** first (change 8) — required by estado resolution.
2. **Parallel DTO removals** (changes 3, 4, 5, 6, 9) — independent deletions.
3. **CrearPedido refactor** (change 1) — most complex; UI model separation in Fragment/Adapter/ViewModel.
4. **Estado body change** (change 2) — depends on catalogs.
5. **Cajas abiertas** (change 7) — new code, independent.

Estimated ~350–400 lines across 20+ files. Exceeds 400-line review budget; deliver as chained PRs.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `data/dto/pedido/` | Modified | Simplify `CrearPedidoRequest`, `CrearDetalleRequest`; delete `CambiarEstadoRequest` |
| `data/dto/demora/` | Modified | Remove `sector` from request DTOs |
| `data/dto/caja/` | Modified | Remove user IDs from apertura/cierre DTOs |
| `data/api/` | Modified | `PedidoApi` int body; new `CajaApi` method; new catalog APIs |
| `data/repository/` | Modified | `PedidoRepositoryImpl` catalog resolution; `CajaRepositoryImpl` new method; new `CatalogoRepository` |
| `ui/pedido/` | Modified | `DetalleLine` model; `CrearPedidoFragment`, `ViewModel`, `DetalleAdapter` mapping |
| `core/` | Modified | `SignalRServiceImpl` handler rename |
| `di/NetworkModule` | Modified | Wire catalog APIs |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| PR exceeds 400-line review budget | High | Split into chained PRs by dependency order |
| Runtime null from catalog lookup | Low | Validate catalog cache on app start; fail fast with clear error |
| UI mapping drift between `DetalleLine` and DTO | Low | Single mapping function in `ViewModel`; unit test mapping |

## Rollback Plan
- Revert merged PRs in reverse dependency order.
- If catalog APIs are unavailable, fallback is not supported by design (server contract is authoritative). Rollback to previous app version if critical.

## Dependencies
- Backend v2 contract deployed and accessible
- Catalog endpoints (`/api/catalogo/...`) available for estado/metodo lookups

## Success Criteria
- [ ] All 10 changes compile and pass existing tests (minus deleted `CambiarEstadoRequestTest`)
- [ ] `CrearPedido` flow submits successfully with simplified DTOs and `DetalleLine` mapping
- [ ] Estado change resolves name→ID via catalog and sends raw int
- [ ] `getCajasAbiertas()` returns data end-to-end
- [ ] SignalR receives `PosicionGPSActualizada` messages correctly
- [ ] No hardcoded estado IDs or lookup values remain in the codebase
