# Verification Report: backend-contract-v2

**Date**: 2026-06-24
**Mode**: Standard verify (Strict TDD active)
**Change**: backend-contract-v2
**Specs**: 7 delta spec files (caja-operaciones, signalr-handlers, pedido-creacion, catalogo-endpoints, cajas-abiertas-listado, pedido-cambio-estado, demora-gestion)
**Design**: `design.md` (170 lines, 4 PR phases)
**Tasks**: `tasks.md` (36/36 tasks complete across 4 work units/PRs)

---

## Completeness Table

| Artifact | Exists | Complete | Status |
|----------|--------|----------|--------|
| Proposal | No | N/A | Skipped — not present; `backend-contract-v2` was a direct spec/design/tasks change |
| Specs | 7 delta files | All fully specified | ✅ |
| Design | `design.md` | 4 phases, 10 decisions | ✅ |
| Tasks | `tasks.md` | 36/36 tasks checked | ✅ |
| Tests | 29 test classes | 241 tests, 0 failures | ✅ |

### Task Completion Detail

| PR | Tasks | Status |
|----|-------|--------|
| PR1 (Catalog infrastructure) | 10/10 | ✅ All done |
| PR2 (DTO cleanup + SignalR) | 15/15 | ✅ All done |
| PR3 (CrearPedido UI) | 5/5 | ✅ All done |
| PR4 (Estado body + cajas abiertas) | 6/6 | ✅ All done |
| **Total** | **36/36** | ✅ |

---

## Build & Tests

| Metric | Value |
|--------|-------|
| Build | `BUILD SUCCESSFUL in 39s` (clean build, 63 tasks executed) |
| Total tests | 241 |
| Passed | 241 |
| Failed | 0 |
| Skipped | 0 |
| Errors | 0 |
| Test runner | `./gradlew clean test` (debug + release variants) |

### Test Coverage by Spec Domain

| Spec Domain | Test Classes | Test Count |
|-------------|-------------|------------|
| Caja Operaciones | `AbrirCajaRequestTest`, `CerrarCajaRequestTest`, `CajaDtoTest` | 15 |
| SignalR Handlers | (static analysis — no unit test for hub registration) | — |
| Pedido Creacion | `CrearPedidoViewModelTest`, `CrearPedidoRequestTest` | 11 |
| Catalogo Endpoints | `CatalogoItemDtoTest`, `CatalogApisTest`, `CatalogoRepositoryImplTest` | 23 |
| Cajas Abiertas | `CajaRepositoryImplTest` (5 tests for getCajasAbiertas) | 24 total |
| Pedido Cambio Estado | `PedidoRepositoryImplTest` (rows for cambiarEstado catalog resolution) | 33 total |
| Demora Gestion | `CrearDemoraRequestTest`, `ActualizarDemoraRequestTest`, `DemoraDtoTest` | 12 |

---

## Spec Compliance Matrix

### 1. Caja Operaciones (`caja-operaciones/spec.md`)

| Requirement | Scenario | Status | Evidence |
|------------|----------|--------|----------|
| AbrirCajaRequest — only `montoApertura` | Construct with monto only | ✅ PASS | `AbrirCajaRequest.java`: constructor takes `double montoApertura` only |
| AbrirCajaRequest — serialize | JSON contains exactly `{"montoApertura":5000.0}` | ✅ PASS | `AbrirCajaRequestTest`: `serializeWithMontoSolo()` |
| CerrarCajaRequest — only two montos | Construct without user ID | ✅ PASS | `CerrarCajaRequest.java`: constructor takes 2 doubles only |
| CerrarCajaRequest — serialize | JSON contains exactly `montoCierreTeorico` + `montoCierreReal` | ✅ PASS | `CerrarCajaRequestTest` |
| CajaDto — no user IDs | Deserialize without `usuarioAperturaId`/`usuarioCierreId` | ✅ PASS | `CajaDto.java`: 9 fields, no ID primitives; `CajaDtoTest`: `deserializeWithoutUserIds()` |

### 2. SignalR Handlers (`signalr-handlers/spec.md`)

| Requirement | Scenario | Status | Evidence |
|------------|----------|--------|----------|
| Handler registered with new event name | `conn.on("PosicionGPSActualizada", ...)` | ✅ PASS | `SignalRServiceImpl.java:262-264` — handler string is `"PosicionGPSActualizada"` |
| Message deserialization | Deserializes into `PosicionGPSActualizadaMessage` | ✅ PASS | `PosicionGPSActualizadaMessage.java` exists; `PosicionGPSMessage.java` deleted (confirmed via glob) |
| Observer receives GPS updates | `signalRService.getPosicionGPSActualizada()` | ✅ PASS | `SignalRService.java:84`, `SignalRServiceImpl.java:216` — getter renamed |
| Old event name not registered | `"PosicionGPS"` not in `SignalRServiceImpl` handlers | ✅ PASS | `registerHandlers()` only uses `"PosicionGPSActualizada"` (line 262); `EnviarPosicionGPS` outbound is unchanged per spec |
| Outbound method unchanged | `conn.send("EnviarPosicionGPS", ...)` | ✅ PASS | `SignalRServiceImpl.java:185` — unchanged |
| MapaViewModel updated | Uses renamed getter and message class | ✅ PASS | `MapaViewModel.java:28` imports `PosicionGPSActualizadaMessage`; line 174 calls `getPosicionGPSActualizada()` |

### 3. Pedido Creacion (`pedido-creacion/spec.md`)

| Requirement | Scenario | Status | Evidence |
|------------|----------|--------|----------|
| CrearDetalleRequest — 4 fields | Construct and serialize | ✅ PASS | Existing DTO; `CrearPedidoRequestTest` covers serialization |
| DetalleLine UI model | Holds `productoId`, `nombre`, `precio`, `cantidad` | ✅ PASS | `DetalleLine.java` — plain POJO with 4 fields, no `@SerializedName` |
| DetalleLine → CrearDetalleRequest mapping | Maps fields 1:1 at submit time | ✅ PASS | `CrearPedidoViewModel.mapDetalles()` line 228-242; `CrearPedidoViewModelTest` covers mapping |
| DetalleAdapter renders DetalleLine | Uses `DetalleLine` instead of DTO | ✅ PASS | `DetalleAdapter.java` type changed (confirmed in apply-progress PR3) |
| Validation: passes with valid data | Returns null | ✅ PASS | `CrearPedidoViewModel.validate()` line 149-172; test: `validationPassesWithValidData()` |
| Validation: fails for empty details | Returns "Agregá al menos un producto" | ✅ PASS | Line 159; test: `validationFailsForEmptyDetails()` |
| Validation: fails for delivery without coords | Returns "Dirección y coordenadas requeridas para Delivery" | ✅ PASS | Lines 163-169; test covers this path |

### 4. Catalogo Endpoints (`catalogo-endpoints/spec.md`)

| Requirement | Scenario | Status | Evidence |
|------------|----------|--------|----------|
| CatalogoItemDto | Deserialize `{"id":3, "nombre":"EnPreparacion"}` | ✅ PASS | `CatalogoItemDto.java`: fields `id` + `nombre`; `CatalogoItemDtoTest`: `deserializeFromJson()` |
| EstadosPedidoApi | `GET api/catalogo/estados-pedido` | ✅ PASS | `EstadosPedidoApi.java:20` — `@GET("api/catalogo/estados-pedido")`; `CatalogApisTest` |
| MetodoPagoApi | `GET api/catalogo/metodos-pago` | ✅ PASS | `MetodoPagoApi.java:20` — `@GET("api/catalogo/metodos-pago")`; `CatalogApisTest` |
| MetodoVentaApi | `GET api/catalogo/metodos-venta` | ✅ PASS | `MetodoVentaApi.java:21` — `@GET("api/catalogo/metodos-venta")`; `CatalogApisTest` |
| CatalogoRepository — load | Fetches and caches all 3 catalogs | ✅ PASS | `CatalogoRepositoryImpl.java:80-82` — eager load on construction; test: `CatalogoRepositoryImplTest` |
| Resolve estado ID by name | Returns `3` for `"EnPreparacion"` | ✅ PASS | `CatalogoRepositoryImpl.java:100-109`; test: `resolveEstadoId_returnsCorrectId()` |
| Resolve fails when not loaded | Throws or returns error | ✅ PASS | Line 103-105: throws `IllegalStateException`; test: `resolveEstadoId_whenNotLoaded_throws()` |
| isReady reflects cache state | false before load, true after all 3 | ✅ PASS | `CatalogoRepositoryImpl.java:134-136`; tests cover both states |
| DI wiring | Hilt provides catalog APIs + repository | ✅ PASS | `NetworkModule.java:119-133` (3 @Provides); `RepositoryModule.java:34` (@Binds) |

### 5. Cajas Abiertas (`cajas-abiertas-listado/spec.md`)

| Requirement | Scenario | Status | Evidence |
|------------|----------|--------|----------|
| GET /api/cajas/abiertas | `CajaApi.getCajasAbiertas()` → `api/cajas/abiertas` | ✅ PASS | `CajaApi.java:33-34` — `@GET("api/cajas/abiertas")` |
| CajaRepository — LOADING→SUCCESS | Emits LOADING then SUCCESS with list | ✅ PASS | `CajaRepositoryImpl.java:102-130`; test: `getCajasAbiertas_successNonEmpty()` |
| CajaRepository — network error | Emits ERROR "No hay conexión a internet" | ✅ PASS | Line 125; test: `getCajasAbiertas_networkError()` |
| CajaRepository — server error | Emits ERROR with parsed `mensaje` | ✅ PASS | Line 117-118 via `parseMensaje`; test: `getCajasAbiertas_serverError()` |
| Empty result handling | SUCCESS with empty list, not null | ✅ PASS | Line 114: `response.body() != null` passes for empty list; test: `getCajasAbiertas_successEmpty()` |

### 6. Pedido Cambio Estado (`pedido-cambio-estado/spec.md`)

| Requirement | Scenario | Status | Evidence |
|------------|----------|--------|----------|
| CambiarEstado API — raw int body | `@Body int` in `PedidoApi` | ✅ PASS | `PedidoApi.java:50` — `@Body int nuevoEstadoId` |
| CambiarEstadoRequest deleted | Class file does not exist | ✅ PASS | Glob + grep confirm no `CambiarEstadoRequest.java` anywhere |
| Repository resolves via catalog | `catalogoRepository.resolveEstadoId()` | ✅ PASS | `PedidoRepositoryImpl.java:252` — `catalogoRepository.resolveEstadoId(estado.getApiValue())` |
| Successful estado change | Sends PATCH with catalog ID | ✅ PASS | Line 261: `pedidoApi.cambiarEstado(id, nuevoEstadoId)` with resolved int; test confirms |
| Catalog lookup failure (not ready) | Emits ERROR, no API call | ✅ PASS | Lines 247-251: `!isReady()` guard; test: `cambiarEstadoEmitsErrorWhenCatalogNotReady()` |
| Catalog lookup failure (name missing) | Emits ERROR when name not in cache | ✅ PASS | Lines 253-257: `<=0` guard; test: `cambiarEstadoEmitsErrorWhenEstadoNotInCatalog()` |

### 7. Demora Gestion (`demora-gestion/spec.md`)

| Requirement | Scenario | Status | Evidence |
|------------|----------|--------|----------|
| CrearDemoraRequest — no sector | 3 fields: pedidoId, demoraMinutos, observaciones | ✅ PASS | `CrearDemoraRequest.java`: only 3 `@SerializedName` fields, constructor takes 3 params |
| CrearDemoraRequest — serialize | JSON without `sector` key | ✅ PASS | `CrearDemoraRequestTest` |
| ActualizarDemoraRequest — no sector | 2 fields: demoraMinutos, observaciones | ✅ PASS | `ActualizarDemoraRequest.java`: only 2 fields |
| ActualizarDemoraRequest — partial update | Null fields omitted | ✅ PASS | `ActualizarDemoraRequestTest` |
| DemoraDto — no sector | 5 fields: id, pedidoId, usuarioId, demoraMinutos, observaciones | ✅ PASS | `DemoraDto.java`: 5 `@SerializedName` fields, no `sector` |
| DemoraDto — deserialize | All 5 fields populated, no `sector` | ✅ PASS | `DemoraDtoTest` |

---

## Design Coherence Table

| Design Decision | Source | Implementation Match | Status |
|----------------|--------|---------------------|--------|
| Retrofit raw `int` body for estado PATCH | `design.md:9-18` | `PedidoApi.java:50` — `@Body int` | ✅ MATCH |
| CatalogoRepository eager singleton with `Map<String,Integer>` cache | `design.md:19-27` | `CatalogoRepositoryImpl.java:46-83` — `@Singleton`, eager load, `HashMap` | ✅ MATCH |
| DetalleLine placed in `ui/pedido/DetalleLine.java` | `design.md:29-33` | `ui/pedido/DetalleLine.java` — plain POJO, 4 fields, no annotations | ✅ MATCH |
| PR1→PR4 stacked-to-main chain | `design.md:132-147` | All 4 PRs committed to main, tasks 36/36 complete | ✅ MATCH |
| CatalogoRepository `isReady()` gates estado changes | `design.md:108-115` | `PedidoRepositoryImpl.java:247-251` — `!isReady()` guard | ✅ MATCH |
| `CajaApi.getCajasAbiertas()` — dedicated endpoint | `design.md:126-130` | `CajaApi.java:33-34` | ✅ MATCH |
| 3 catalog APIs + 1 repository interface + impl | `design.md:60-68` | All created and DI-wired | ✅ MATCH |
| SignalR rename: `PosicionGPS` → `PosicionGPSActualizada` | `design.md:80-84` | `SignalRServiceImpl.java:262`, `SignalRService.java:84`, DTO renamed | ✅ MATCH |
| DTO cleanup: no `sector`, no user IDs | `design.md:72-76` | All 6 DTO files match | ✅ MATCH |

---

## Issues

### CRITICAL (0)

None. All 7 spec domains pass with zero runtime test failures. No missing files, no stale references, no hardcoded IDs.

### WARNING (2)

1. **Design open question unresolved**: `DemoraRegistradaMessage.getSector()` callers in UI. The design.md (line 170) explicitly flagged: *"DemoraRegistradaMessage.getSector() callers in UI — spec only covers DemoraDto removal; SignalR message may need separate treatment."* This was not addressed in any PR. The SignalR message `DemoraRegistradaMessage` may still carry a `sector` field that the UI reads but was not removed. Not in any spec's scope, but the design's open question remains open.

2. **`<=0` guard vs `==-1` sentinel**: The `CatalogoRepository.resolveEstadoId()` contract says it returns `-1` on a miss. `PedidoRepositoryImpl.cambiarEstado()` uses `<=0` as a defensive superset. While this is functionally correct (it catches the documented `-1` case and also guards against a malformed catalog returning `0`), it introduces a slight semantic gap between the documented sentinel value and the guard condition. The spec only requires that unknown names produce an error — the current implementation meets that requirement.

### SUGGESTION (3)

1. **UI cleanup — `inputSector` EditText**: The `fragment_demora.xml` layout may still contain a deprecated `inputSector` EditText field. This is purely UI cleanup, out of scope for the backend contract migration. Not blocking.

2. **Cajas-abiertas has no UI consumer**: The `GET /api/cajas/abiertas` endpoint is fully implemented at the API + repository layer (with 5 tests), but no ViewModel or Fragment calls `cajaRepository.getCajasAbiertas()` yet. This is a UI concern, not a contract one — the v2 contract migration adds the endpoint; wiring a UI consumer is a future task.

3. **Pairwise-distinct test naming**: `CajaRepositoryImplTest.allFourStateInstancesArePairwiseDistinct` still uses the name "Four" despite now covering 5 state instances. The inline comment was updated but the method name was kept for grep-history stability. Cosmetic — rename at leisure.

---

## Final Verdict

**PASS**

All 36 tasks complete. 241/241 tests passing with zero failures. All 7 spec domains verified against source code with matching test evidence. Design decisions implemented faithfully. No CRITICAL issues. 2 WARNINGs (open design question and defensive guard granularity), 3 SUGGESTIONs (future UI work).

### Ready for Archive: ✅ YES

The change is complete, tested, and verified. All blocking quality gates pass. The 2 WARNINGs do not block archive — one is a pre-existing open design question (not a regression), and the other is a documented defensive implementation choice that meets the spec.
