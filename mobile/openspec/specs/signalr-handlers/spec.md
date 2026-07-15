# Delta for SignalR Handlers

## MODIFIED Requirements

### Requirement: PosicionGPS Handler Rename

The SignalR hub handler for GPS position updates SHALL listen to the `PosicionGPSActualizada` event name instead of `PosicionGPS`. The `PosicionGPSMessage` DTO class SHALL be renamed to `PosicionGPSActualizadaMessage`. The `SignalRService` interface method `getPosicionGPS()` SHALL be renamed to `getPosicionGPSActualizada()`.

(Previously: Handler listened to `PosicionGPS` event; DTO was `PosicionGPSMessage`; LiveData getter was `getPosicionGPS()`.)

#### Scenario: Handler registered with new event name

- GIVEN `SignalRServiceImpl` connects to the hub
- WHEN `registerHandlers()` is called
- THEN `conn.on("PosicionGPSActualizada", ...)` is registered
- AND `conn.on("PosicionGPS", ...)` is NOT registered

#### Scenario: Message deserialization

- GIVEN the server pushes a `PosicionGPSActualizada` event with `repartidorId`, `latitud`, `longitud`
- WHEN the handler receives the message
- THEN it deserializes into `PosicionGPSActualizadaMessage`
- AND posts the value to `_posicionGPSActualizada` LiveData

#### Scenario: Observer receives GPS updates

- GIVEN a fragment observing `signalRService.getPosicionGPSActualizada()`
- WHEN the server pushes a new GPS position
- THEN the observer receives the `PosicionGPSActualizadaMessage` with correct coordinates

#### Scenario: Old event name is not registered

- GIVEN the codebase after migration
- WHEN searching for `"PosicionGPS"` string literal in `SignalRServiceImpl`
- THEN no handler registration uses the old event name (only `EnviarPosicionGPS` outbound call remains)

### Requirement: Outbound GPS Method Unchanged

The `enviarPosicion()` method SHALL continue to invoke `EnviarPosicionGPS` on the hub — this outbound method name is NOT changing. Only the inbound handler name changes.

(Previously: Same behavior — no change to outbound GPS sending.)

#### Scenario: Send GPS position

- GIVEN a connected hub and repartidor with GPS coordinates
- WHEN `enviarPosicion(repartidorId, lat, lng)` is called
- THEN `conn.send("EnviarPosicionGPS", repartidorId, lat, lng)` is invoked
- AND the method name string remains `EnviarPosicionGPS`
