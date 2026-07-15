# Delta for Pedido Creacion

## MODIFIED Requirements

### Requirement: CrearPedidoRequest DTO

The `CrearPedidoRequest` DTO SHALL contain only the fields required by the backend v2 contract: `cajaId` (nullable Integer), `metodoPagoId` (int), `metodoVentaId` (int), `clienteNombre` (String), `clienteDireccion` (String), `latitudDestino` (nullable Double), `longitudDestino` (nullable Double), `totalEstimado` (double), `demoraAprox` (nullable Integer), and `detalles` (List of `CrearDetalleRequest`).

(Previously: DTO included all the same fields; no structural change to CrearPedidoRequest itself — simplification applies to CrearDetalleRequest.)

#### Scenario: Serialize CrearPedidoRequest with all fields

- GIVEN a `CrearPedidoRequest` with all fields populated
- WHEN Gson serializes the request
- THEN the JSON contains exactly the 10 keys matching `@SerializedName` annotations
- AND null fields (`cajaId`, `latitudDestino`, `longitudDestino`, `demoraAprox`) are omitted from the JSON

#### Scenario: Serialize CrearPedidoRequest for delivery

- GIVEN a `CrearPedidoRequest` with `metodoVentaId == 1` (delivery) and valid `latitudDestino`/`longitudDestino`
- WHEN Gson serializes the request
- THEN the JSON includes both `latitudDestino` and `longitudDestino`

### Requirement: CrearDetalleRequest DTO

The `CrearDetalleRequest` DTO SHALL contain exactly four fields: `productoId` (int), `nombre` (String), `precio` (double), and `cantidad` (int). All fields are required by the server and use primitives.

(Previously: DTO had the same four fields with the same types — no change to field structure.)

#### Scenario: Construct CrearDetalleRequest

- GIVEN productoId, nombre, precio, and cantidad values
- WHEN a `CrearDetalleRequest` is constructed via its constructor
- THEN all four fields are set correctly

#### Scenario: Serialize CrearDetalleRequest

- GIVEN a `CrearDetalleRequest` with valid values
- WHEN Gson serializes it
- THEN the JSON contains exactly `productoId`, `nombre`, `precio`, and `cantidad`

### Requirement: DetalleLine UI Model

The `DetalleLine` model SHALL serve as the UI-layer representation of a pedido detail line in `CrearPedidoFragment` and `DetalleAdapter`. It SHALL be mapped to `CrearDetalleRequest` at submit time in the ViewModel.

(Previously: `DetalleAdapter` and `CrearPedidoFragment` used `CrearDetalleRequest` directly as the UI model.)

#### Scenario: DetalleLine holds UI state

- GIVEN a product with id, name, price, and a user-selected quantity
- WHEN a `DetalleLine` is created
- THEN it stores `productoId`, `nombre`, `precio`, and `cantidad`

#### Scenario: DetalleLine maps to CrearDetalleRequest

- GIVEN a list of `DetalleLine` objects in the ViewModel
- WHEN the user submits the pedido form
- THEN each `DetalleLine` is mapped to a `CrearDetalleRequest` with identical field values
- AND the resulting list is set on the `CrearPedidoRequest`

#### Scenario: DetalleAdapter renders DetalleLine

- GIVEN a `DetalleAdapter` backed by `DetalleLine` items
- WHEN `submitList()` is called with a list of `DetalleLine`
- THEN each row displays the product name, quantity, and line subtotal
- AND the delete button triggers removal of that line

### Requirement: CrearPedido Validation

The `CrearPedidoViewModel` SHALL validate the request before submission: client name must not be blank, at least one detalle line must exist, and delivery orders (`metodoVentaId == 1`) must have both address and coordinates.

(Previously: Same validation rules applied; validation now operates on `DetalleLine` list before mapping to DTO.)

#### Scenario: Validation passes with valid data

- GIVEN a request with non-blank client name, at least one detail, and valid delivery coords when applicable
- WHEN `validate()` is called
- THEN it returns null (no error)

#### Scenario: Validation fails for empty details

- GIVEN a request with no detalle lines
- WHEN `validate()` is called
- THEN it returns "Agregá al menos un producto"
- AND the API is never called

#### Scenario: Validation fails for delivery without coords

- GIVEN a request with `metodoVentaId == 1` but missing `latitudDestino` or `longitudDestino`
- WHEN `validate()` is called
- THEN it returns "Dirección y coordenadas requeridas para Delivery"
- AND the API is never called
