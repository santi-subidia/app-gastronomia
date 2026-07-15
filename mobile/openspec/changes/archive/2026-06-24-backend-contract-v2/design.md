# Design: backend-contract-v2

## Technical Approach

Adapt the Android client to the backend's simplified v2 contract via five ordered phases: catalog infrastructure, parallel DTO removals, CrearPedido UI refactor, estado body change, and cajas-abiertas endpoint. Each phase is a reviewable work unit. Delivered as 4 chained PRs respecting the 400-line budget.

## Architecture Decisions

### Decision: Retrofit raw int body for estado PATCH

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `@Body int` with Gson | Gson serializes to bare number string `3` — exact contract | **Chosen** |
| Scalar converter factory | Extra Retrofit config, unnecessary complexity | Rejected |
| Keep `CambiarEstadoRequest` wrapper | Backward contract already deployed | Rejected |

**Rationale**: Retrofit's `GsonConverterFactory` serializes `@Body int` as `3` (raw number). The backend v2 PATCH `/api/pedidos/{id}/estado` accepts this exact body. No custom converter needed. Tested: `Gson.toJson(3)` produces `"3"`.

### Decision: CatalogoRepository cache strategy

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `@Singleton` in-memory `Map<String,Integer>` | Simple, fast lookup, small data | **Chosen** |
| Room/DB cache | Overengineered for ~13 catalog rows | Rejected |
| Load-on-each-call | N+1 network requests, slow | Rejected |

**Rationale**: Three catalogs total ~13 entries. In-memory map with `resolveEstadoId(name)` lookup. Catalogs loaded on `CatalogoRepository` construction (eager) so `isReady()` gates dependents. Thread-safe via `synchronized` on load methods.

### Decision: DetalleLine UI model placement

**Choice**: Plain POJO in `ui/pedido/DetalleLine.java`. Same 4 fields as `CrearDetalleRequest` but no `@SerializedName` annotations.

**Rationale**: The fragment/adapter were directly using the DTO as a UI model. Separating lets the DTO stay a pure wire contract. Mapping happens in `CrearPedidoViewModel.buildRequest()` one-way at submit time.

## Data Flow

```
Catalog APIs (Retrofit) ──→ CatalogoRepository (cache)
                                    │
                           resolveEstadoId(name)
                                    │
                                    ▼
PedidoDetailFragment ──→ PedidoRepositoryImpl.cambiarEstado()
                                    │
                          PATCH /api/pedidos/{id}/estado
                          Body: 3  (raw int)
```

```
CrearPedidoFragment ──→ DetalleAdapter (List<DetalleLine>)
        │                       │
        ▼                       ▼
CrearPedidoViewModel.buildRequest()
        │  maps DetalleLine → CrearDetalleRequest
        ▼
PedidoRepository.crearPedido(CrearPedidoRequest)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `data/dto/catalogo/CatalogoItemDto.java` | **Create** | Catalog entry: `(int id, String nombre)` |
| `data/api/EstadosPedidoApi.java` | **Create** | `GET api/catalogo/estados-pedido` |
| `data/api/MetodoPagoApi.java` | **Create** | `GET api/catalogo/metodos-pago` |
| `data/api/MetodoVentaApi.java` | **Create** | `GET api/catalogo/metodos-venta` |
| `data/repository/contract/CatalogoRepository.java` | **Create** | Interface: load/resolve/isReady |
| `data/repository/CatalogoRepositoryImpl.java` | **Create** | Singleton impl with in-memory cache |
| `di/NetworkModule.java` | Modify | +3 `@Provides` for catalog APIs |
| `di/RepositoryModule.java` | Modify | +1 `@Binds` for CatalogoRepository |
| `data/dto/demora/CrearDemoraRequest.java` | Modify | Remove `sector` field and constructor param |
| `data/dto/demora/ActualizarDemoraRequest.java` | Modify | Remove `sector` field |
| `data/dto/demora/DemoraDto.java` | Modify | Remove `sector` field and `@SerializedName` |
| `data/dto/caja/AbrirCajaRequest.java` | Modify | Remove `usuarioAperturaId`; constructor takes `montoApertura` only |
| `data/dto/caja/CerrarCajaRequest.java` | Modify | Remove `usuarioCierreId`; constructor takes 2 doubles |
| `data/dto/caja/CajaDto.java` | Modify | Remove `usuarioAperturaId` and `usuarioCierreId` fields |
| `data/dto/pedido/CambiarEstadoRequest.java` | **Delete** | Replaced by raw int body |
| `data/api/PedidoApi.java` | Modify | `cambiarEstado()` takes `@Body int` instead of `CambiarEstadoRequest` |
| `data/repository/PedidoRepositoryImpl.java` | Modify | `cambiarEstado` resolves via `CatalogoRepository`, sends raw int |
| `data/repository/contract/PedidoRepository.java` | Modify | Update Javadoc only (signature unchanged) |
| `data/dto/signalr/PosicionGPSMessage.java` | **Rename** | → `PosicionGPSActualizadaMessage.java` |
| `core/SignalRService.java` | Modify | `getPosicionGPS()` → `getPosicionGPSActualizada()`; import update |
| `core/SignalRServiceImpl.java` | Modify | Handler `"PosicionGPS"` → `"PosicionGPSActualizada"`; field+getter rename |
| `ui/repartidor/MapaViewModel.java` | Modify | Update observer type and getter call |
| `ui/pedido/DetalleLine.java` | **Create** | UI model: `productoId, nombre, precio, cantidad` |
| `ui/pedido/DetalleAdapter.java` | Modify | `CrearDetalleRequest` → `DetalleLine` throughout |
| `ui/pedido/CrearPedidoFragment.java` | Modify | Use `List<DetalleLine>`; map to DTO in `buildRequestFromForm()` |
| `ui/pedido/CrearPedidoViewModel.java` | Modify | `buildRequest` maps `DetalleLine` → `CrearDetalleRequest` |
| `ui/cajero/CajaViewModel.java` | Modify | Remove `tokenManager.getUserId()` from `abrirCaja`/`cerrarCaja` constructors |
| `data/api/CajaApi.java` | Modify | Add `@GET("api/cajas/abiertas")` |
| `data/repository/contract/CajaRepository.java` | Modify | Add `getCajasAbiertas()` and `getCajasAbiertasState()` |
| `data/repository/CajaRepositoryImpl.java` | Modify | Add `_cajasAbiertasState` LiveData + impl |

### Test Files

| File | Action |
|------|--------|
| `test/.../CambiarEstadoRequestTest.java` | **Delete** |
| `test/.../AbrirCajaRequestTest.java` | Modify — remove `usuarioAperturaId` assertions |
| `test/.../CerrarCajaRequestTest.java` | Modify — remove `usuarioCierreId` assertions |
| `test/.../CajaDtoTest.java` | Modify — remove ID field assertions |
| `test/.../PedidoRepositoryImplTest.java` | Modify — update `cambiarEstado` mock to use `int` body |

## Interfaces / Contracts

### CatalogoRepository (new)

```java
public interface CatalogoRepository {
    void loadEstadosPedido();
    void loadMetodosPago();
    void loadMetodosVenta();
    int resolveEstadoId(String nombre);
    boolean isReady();
}
```

### PedidoApi.cambiarEstado (modified)

```java
@PATCH("api/pedidos/{id}/estado")
Call<PedidoDetalleDto> cambiarEstado(@Path("id") int id, @Body int nuevoEstadoId);
```

### CajaApi (addition)

```java
@GET("api/cajas/abiertas")
Call<List<CajaDto>> getCajasAbiertas();
```

## Chained PR Structure

| PR | Phase | Contents | Est. Lines |
|----|-------|----------|-----------|
| **PR1** → main | Catalog infrastructure | CatalogoItemDto, 3 APIs, CatalogoRepository contract+impl, DI wiring | ~135 |
| **PR2** → main | DTO cleanup + SignalR | Remove `sector` (3 files), remove user IDs (3 files), delete `CambiarEstadoRequest`, `PedidoApi` sig, SignalR rename (4 files), `CajaViewModel` callers | ~140 |
| **PR3** → main | CrearPedido UI | `DetalleLine`, `DetalleAdapter`, `CrearPedidoFragment`, `CrearPedidoViewModel.buildRequest` mapping | ~100 |
| **PR4** → main | Estado body + cajas abiertas | `PedidoRepositoryImpl.cambiarEstado` catalog resolution, `CajaApi.getCajasAbiertas`, `CajaRepository`+impl new methods | ~75 |

**Dependency graph**:
```
PR1 (catalog) ──────→ PR4 (estado body, needs resolveEstadoId)
                  └──→ PR3 (unrelated, but ships after PR2 for ordering)
PR2 (dto cleanup) ──→ PR3 (fragment uses CrearDetalleRequest still, no conflict)
                  └──→ PR4 (deletes CambiarEstadoRequest, changes PedidoApi sig)
```

PR1 and PR2 can be developed in parallel (no shared files). PR3 and PR4 depend on PR1+PR2 being merged. All target `main` (stacked-to-main).

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `CatalogoRepositoryImpl` cache/resolve | Mock APIs, verify cache hit/miss |
| Unit | `CrearPedidoViewModel` DetalleLine→DTO mapping | Given DetalleLine list, assert CrearDetalleRequest fields match |
| Unit | DTO serialization (all modified) | Gson.toJson asserts on field presence/absence |
| Integration | `PedidoRepositoryImpl.cambiarEstado` catalog resolution | Fake PedidoApi verifying int body; real CatalogoRepository with preset cache |
| Integration | `CajaRepositoryImpl.getCajasAbiertas` | Fake CajaApi; assert SUCCESS/ERROR states |

## Migration / Rollout

- Backend v2 already deployed — no feature flags needed.
- Catalog APIs must be live. If unreachable, `CatalogoRepository.load*()` fails on construction; `isReady()==false` gates estado changes with clear error.
- Rollback: revert PRs in reverse order (PR4 → PR3 → PR2 → PR1). No database migrations.

## Open Questions

- [ ] Confirm `@Body int` serialization on the specific Retrofit+Gson version in use (tested conceptually; runtime verification needed)
- [ ] Verify `DemoraRegistradaMessage.getSector()` callers in UI — spec only covers DemoraDto removal; SignalR message may need separate treatment
