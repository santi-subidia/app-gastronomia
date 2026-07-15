# Delta for Caja Operaciones

## MODIFIED Requirements

### Requirement: AbrirCajaRequest DTO

The `AbrirCajaRequest` DTO SHALL contain exactly one field: `montoApertura` (double). The `usuarioAperturaId` field SHALL be removed — the server derives the user from the auth token.

(Previously: DTO required both `usuarioAperturaId` (int) and `montoApertura` (double).)

#### Scenario: Construct AbrirCajaRequest with monto only

- GIVEN a montoApertura value
- WHEN an `AbrirCajaRequest` is constructed
- THEN it stores only `montoApertura` — no `usuarioAperturaId` parameter

#### Scenario: Serialize AbrirCajaRequest

- GIVEN an `AbrirCajaRequest` with `montoApertura = 5000.0`
- WHEN Gson serializes it
- THEN the JSON contains exactly `{"montoApertura": 5000.0}`
- AND no `usuarioAperturaId` key is present

### Requirement: CerrarCajaRequest DTO

The `CerrarCajaRequest` DTO SHALL contain exactly two fields: `montoCierreTeorico` (double) and `montoCierreReal` (double). The `usuarioCierreId` field SHALL be removed — the server derives the user from the auth token.

(Previously: DTO required `usuarioCierreId` (int), `montoCierreTeorico` (double), and `montoCierreReal` (double).)

#### Scenario: Construct CerrarCajaRequest without user ID

- GIVEN montoCierreTeorico and montoCierreReal values
- WHEN a `CerrarCajaRequest` is constructed
- THEN it stores only the two monto fields — no `usuarioCierreId` parameter

#### Scenario: Serialize CerrarCajaRequest

- GIVEN a `CerrarCajaRequest` with `montoCierreTeorico = 10000.0` and `montoCierreReal = 9800.0`
- WHEN Gson serializes it
- THEN the JSON contains exactly `{"montoCierreTeorico": 10000.0, "montoCierreReal": 9800.0}`
- AND no `usuarioCierreId` key is present

### Requirement: CajaDto Response

The `CajaDto` SHALL retain `usuarioAperturaNombre` (String) for display purposes but SHALL no longer require `usuarioAperturaId` as a primitive. The `usuarioAperturaId` field SHALL be removed from the DTO. The `usuarioCierreId` field (already nullable Integer) SHALL also be removed.

(Previously: DTO included `usuarioAperturaId` (int) and `usuarioCierreId` (Integer) alongside their name counterparts.)

#### Scenario: Deserialize CajaDto without user IDs

- GIVEN a server response JSON without `usuarioAperturaId` or `usuarioCierreId`
- WHEN Gson deserializes it into `CajaDto`
- THEN all remaining fields are populated correctly
- AND the DTO has no `usuarioAperturaId` or `usuarioCierreId` fields
