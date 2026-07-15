# Delta for Demora Gestion

## MODIFIED Requirements

### Requirement: CrearDemoraRequest DTO

The `CrearDemoraRequest` DTO SHALL contain exactly three fields: `pedidoId` (int), `demoraMinutos` (int), and `observaciones` (String). The `sector` field SHALL be removed.

(Previously: DTO included `sector` (String) as a required field alongside the other three.)

#### Scenario: Construct CrearDemoraRequest without sector

- GIVEN pedidoId, demoraMinutos, and observaciones values
- WHEN a `CrearDemoraRequest` is constructed
- THEN it stores only the three fields — no `sector` parameter

#### Scenario: Serialize CrearDemoraRequest

- GIVEN a `CrearDemoraRequest` with valid values
- WHEN Gson serializes it
- THEN the JSON contains exactly `pedidoId`, `demoraMinutos`, and `observaciones`
- AND no `sector` key is present

### Requirement: ActualizarDemoraRequest DTO

The `ActualizarDemoraRequest` DTO SHALL contain exactly two fields: `demoraMinutos` (nullable Integer) and `observaciones` (nullable String). The `sector` field SHALL be removed.

(Previously: DTO included `sector` (nullable String) as an optional partial-update field.)

#### Scenario: Construct empty ActualizarDemoraRequest

- GIVEN no values are set
- WHEN a new `ActualizarDemoraRequest` is created via its no-arg constructor
- THEN all fields are null

#### Scenario: Serialize partial update without sector

- GIVEN an `ActualizarDemoraRequest` with only `demoraMinutos` set
- WHEN Gson serializes it
- THEN the JSON contains only `demoraMinutos`
- AND no `sector` key is present

### Requirement: DemoraDto Response

The `DemoraDto` SHALL no longer include the `sector` field. The response DTO SHALL contain: `id`, `pedidoId`, `usuarioId`, `demoraMinutos`, and `observaciones`.

(Previously: DTO included `sector` (String) as a response field.)

#### Scenario: Deserialize DemoraDto without sector

- GIVEN a server response JSON with `id`, `pedidoId`, `usuarioId`, `demoraMinutos`, and `observaciones`
- WHEN Gson deserializes it into `DemoraDto`
- THEN all five fields are populated correctly
- AND no `sector` field exists on the DTO
