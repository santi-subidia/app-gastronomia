# API Gastronomía — Referencia de Endpoints y Entidades

> **Stack:** .NET 10 Web API · EF Core 10 · PostgreSQL · Redis · SignalR · JWT Bearer  
> **Docs interactivas (dev):** `/scalar/v1` — incluye todos los endpoints con request/response de ejemplo

---

## 1. Autenticación

Todos los endpoints salvo `/api/auth/login` requieren token JWT.

### Obtener token

**`POST /api/auth/login`** (único endpoint público)

```json
// Request
{
  "usuarioNombre": "cajero1",
  "password": "miPassword123"     // mínimo 6 caracteres
}

// Response 200
{
  "id": 1,
  "usuarioNombre": "cajero1",
  "rolId": 1,
  "rolNombre": "Cajero",
  "token": "eyJhbGciOi...",
  "expiraEn": "2026-06-19T04:00:00Z"
}

// Error 401
{ "mensaje": "Credenciales inválidas o usuario inactivo." }
```

**Headers para requests autenticados:**

```
Authorization: Bearer {token}
```

**Claims del JWT:**
- `sub` → ID del usuario
- `unique_name` → nombre de usuario
- `role` → nombre del rol (`Cajero`, `Cocina`, `Repartidor`)
- `jti` → identificador único del token

**Rate limit:** 10 requests por minuto por IP para el endpoint de login.  
**Expiración:** 480 minutos (8 horas) por defecto.

---

## 2. Roles y Permisos

| Rol         | Permisos                                                                 |
|-------------|--------------------------------------------------------------------------|
| **Cajero**  | CRUD productos, crear pedidos, cambiar estados, abrir/cerrar caja, demoras, configuración |
| **Cocina**  | Ver pedidos, ver productos, unirse al grupo `cocina` vía SignalR         |
| **Repartidor** | Ver pedidos, ver productos, enviar posición GPS, unirse a grupos de repartidor |

---

## 3. Endpoints REST

### 3.1 Productos — `/api/productos`

| Método   | Ruta                  | Auth | Roles      |
|----------|-----------------------|------|------------|
| `GET`    | `/api/productos`      | ✅   | Cualquiera |
| `GET`    | `/api/productos/{id}` | ✅   | Cualquiera |
| `POST`   | `/api/productos`      | ✅   | `Cajero`   |
| `PUT`    | `/api/productos/{id}` | ✅   | `Cajero`   |
| `DELETE` | `/api/productos/{id}` | ✅   | `Cajero`   |

#### `GET /api/productos` — Listar productos activos

```json
// Response 200
[
  {
    "id": 1,
    "nombre": "Milanesa con Papas Fritas",
    "precio": 8500,
    "demora": 25,
    "activo": true
  }
]
```

#### `GET /api/productos/{id}` — Obtener producto por ID

Misma estructura que el ítem del array.  
**404** si no existe: `{ "mensaje": "Producto #99 no encontrado." }`

#### `POST /api/productos` — Crear producto (Cajero)

```json
// Request
{
  "nombre": "Nuevo Producto",
  "precio": 5000,
  "demora": 15                // minutos de preparación
}

// Response 201 → igual que GET, incluye id y activo=true
```

**409** si el nombre ya existe.

#### `PUT /api/productos/{id}` — Actualizar producto (Cajero)

```json
// Request (todos los campos son opcionales — partial update)
{
  "nombre": "Nombre Actualizado",
  "precio": 5500,
  "demora": 20
}
```

#### `DELETE /api/productos/{id}` — Eliminar producto (soft delete)

Setea `activo = false`.  
**404** si no existe.

---

### 3.2 Pedidos — `/api/pedidos`

| Método  | Ruta                              | Auth | Roles      |
|---------|-----------------------------------|------|------------|
| `GET`   | `/api/pedidos`                    | ✅   | Cualquiera |
| `GET`   | `/api/pedidos/{id}`               | ✅   | Cualquiera |
| `GET`   | `/api/pedidos/estado/{estado}`    | ✅   | Cualquiera |
| `POST`  | `/api/pedidos`                    | ✅   | Cualquiera |
| `PATCH` | `/api/pedidos/{id}/estado`        | ✅   | Cualquiera |
| `PATCH` | `/api/pedidos/{id}/repartidor`    | ✅   | Cualquiera |

#### Estados de pedido (`EstadoPedidoEnum`)

| Valor | Nombre            |
|-------|-------------------|
| 1     | `Pendiente`       |
| 2     | `EnPreparacion`   |
| 3     | `ListoParaRetirar`|
| 4     | `EnCamino`        |
| 5     | `Entregado`       |
| 6     | `Retirado`        |
| 7     | `Cancelado`       |
| 8     | `Devuelto`        |

#### `GET /api/pedidos` — Listar todos (resumen)

```json
// Response 200
[
  {
    "id": 1,
    "estado": "Pendiente",
    "clienteNombre": "Juan Pérez",
    "metodoVenta": "Delivery",
    "totalEstimado": 15000,
    "fechaIngreso": "2026-06-18T14:30:00Z"
  }
]
```

#### `GET /api/pedidos/{id}` — Detalle completo de un pedido

```json
// Response 200
{
  "id": 1,
  "estado": "EnPreparacion",
  "clienteNombre": "Juan Pérez",
  "clienteDireccion": "Av. Siempre Viva 742",
  "metodoVenta": "Delivery",
  "metodoPago": "Efectivo",
  "totalEstimado": 15000,
  "demoraAprox": 25,
  "latitudDestino": -34.6037,
  "longitudDestino": -58.3816,
  "fechaIngreso": "2026-06-18T14:30:00Z",
  "fechaEstimadoFin": "2026-06-18T14:55:00Z",
  "fechaAsignado": null,
  "fechaEnCamino": null,
  "fechaFinalizado": null,
  "repartidorNombre": null,
  "cajaId": 1,
  "estadoId": 2,
  "detallePedidos": [
    {
      "productoId": 1,
      "nombre": "Milanesa con Papas Fritas",
      "cantidad": 2,
      "precio": 8500,
      "tiempoMaquina": 25
    }
  ]
}
```

**404** si no existe: `{ "mensaje": "Pedido #99 no encontrado." }`

#### `GET /api/pedidos/estado/{estado}` — Filtrar por estado

El parámetro de ruta `estado` es el nombre del enum: `Pendiente`, `EnCamino`, etc.  
Retorna array de `PedidoResumenDTO`.

#### `POST /api/pedidos` — Crear pedido

```json
// Request
{
  "cajaId": 1,                  // opcional — caja abierta actual
  "metodoPagoId": 1,            // 1=Efectivo, 2=Transferencia, 3=Tarjeta
  "metodoVentaId": 1,           // 1=Delivery, 2=Retiro en local
  "clienteNombre": "Juan Pérez",
  "clienteDireccion": "Av. Siempre Viva 742",
  "latitudDestino": -34.6037,   // opcional — requerido para Delivery
  "longitudDestino": -58.3816,  // opcional
  "totalEstimado": 15000,
  "demoraAprox": 25,            // opcional — minutos estimados totales
  "detalles": [
    {
      "productoId": 1,
      "nombre": "Milanesa con Papas Fritas",
      "precio": 8500,
      "cantidad": 2
    }
  ]
}

// Response 201 → PedidoDetalleDTO (misma forma que GET por ID)
// Además dispara evento SignalR "NuevoPedido" al grupo "cocina"
```

#### `PATCH /api/pedidos/{id}/estado` — Cambiar estado

```json
// Request
{
  "nuevoEstado": "EnPreparacion"   // valor del enum
}

// Response 200 → PedidoDetalleDTO actualizado
// Dispara evento SignalR "EstadoCambiado" al grupo "pedido_{id}"
// y "PedidoActualizado" al grupo "cocina"
```

#### `PATCH /api/pedidos/{id}/repartidor` — Asignar repartidor

```json
// Request
{
  "repartidorId": 5
}

// Response 200 → PedidoDetalleDTO actualizado
// Dispara evento SignalR "RepartidorAsignado" al grupo "pedido_{id}"
```

---

### 3.3 Usuarios — `/api/usuarios`

| Método   | Ruta                  | Auth | Roles              |
|----------|-----------------------|------|--------------------|
| `GET`    | `/api/usuarios`       | ✅   | `Admin`            |
| `GET`    | `/api/usuarios/{id}`  | ✅   | Propio o `Admin`   |
| `POST`   | `/api/usuarios`       | ✅   | `Admin`            |
| `PUT`    | `/api/usuarios/{id}`  | ✅   | Propio o `Admin`   |
| `DELETE` | `/api/usuarios/{id}`  | ✅   | `Admin`            |

> ⚠️ **Nota:** Los roles seedeados son `Cajero`, `Cocina`, `Repartidor`. No existe rol `Admin` en los seeds — se debe crear manualmente o ajustar la política de autorización si se necesita. Actualmente `GET /api/usuarios` requiere rol `Admin`, por lo que **este endpoint no es accesible con los usuarios seedeados**.

#### `POST /api/usuarios` — Crear usuario (Admin)

```json
// Request
{
  "usuarioNombre": "nuevoCajero",
  "password": "clave123",       // mínimo 6 caracteres
  "rolId": 1                     // 1=Cajero, 2=Cocina, 3=Repartidor
}
```

#### `PUT /api/usuarios/{id}` — Actualizar usuario (Admin o propio)

```json
// Request (todos opcionales)
{
  "usuarioNombre": "nuevoNombre",
  "password": "nuevaClave456",
  "rolId": 2,
  "disponible": false
}
```

#### Response de usuario

```json
{
  "id": 1,
  "usuarioNombre": "cajero1",
  "rolId": 1,
  "rolNombre": "Cajero",
  "disponible": true,
  "activo": true
}
```

---

### 3.4 Cajas — `/api/cajas`

| Método | Ruta                     | Auth | Roles      |
|--------|--------------------------|------|------------|
| `GET`  | `/api/cajas`             | ✅   | Cualquiera |
| `GET`  | `/api/cajas/{id}`        | ✅   | Cualquiera |
| `POST` | `/api/cajas/apertura`    | ✅   | Cualquiera |
| `POST` | `/api/cajas/{id}/cierre` | ✅   | Cualquiera |

#### `GET /api/cajas` — Listar cajas

**Query param opcional:** `?estado=abiertas` o `?estado=cerradas`

```json
// Response 200
[
  {
    "id": 1,
    "usuarioAperturaId": 1,
    "usuarioAperturaNombre": "cajero1",
    "usuarioCierreId": null,
    "usuarioCierreNombre": null,
    "fechaApertura": "2026-06-18T08:00:00Z",
    "fechaCierre": null,
    "montoApertura": 5000,
    "montoCierreTeorico": null,
    "montoCierreReal": null,
    "estado": "abierta"
  }
]
```

#### `POST /api/cajas/apertura` — Abrir caja

```json
// Request
{
  "usuarioAperturaId": 1,
  "montoApertura": 5000.00
}

// Response 201 → CajaResponse
```

#### `POST /api/cajas/{id}/cierre` — Cerrar caja

```json
// Request
{
  "usuarioCierreId": 1,
  "montoCierreTeorico": 25000.00,
  "montoCierreReal": 24850.00
}

// Response 200 → CajaResponse con estado "cerrada"
```

---

### 3.5 Demoras — `/api/demoras`

| Método   | Ruta                   | Auth | Roles      |
|----------|------------------------|------|------------|
| `GET`    | `/api/demoras`         | ✅   | Cualquiera |
| `POST`   | `/api/demoras`         | ✅   | `Cajero`   |
| `PUT`    | `/api/demoras/{id}`    | ✅   | `Cajero`   |
| `DELETE` | `/api/demoras/{id}`    | ✅   | `Cajero`   |

#### `GET /api/demoras?pedidoId={id}` — Listar demoras de un pedido

Query param `pedidoId` es **obligatorio**.

```json
// Response 200
[
  {
    "id": 1,
    "pedidoId": 5,
    "usuarioId": 1,
    "demoraMinutos": 15,
    "sector": "cocina",
    "observaciones": "Falta ingrediente principal"
  }
]
```

#### `POST /api/demoras` — Registrar demora

```json
// Request
{
  "pedidoId": 5,
  "demoraMinutos": 15,
  "sector": "cocina",
  "observaciones": "Falta ingrediente principal"
}

// Response 201 → DemoraResponse
// Dispara evento SignalR "DemoraRegistrada" al grupo "pedido_{id}"
```

#### `PUT /api/demoras/{id}` — Actualizar demora

```json
// Request
{
  "demoraMinutos": 20,
  "sector": "repartidor",
  "observaciones": "Tráfico en zona"
}
```

---

### 3.6 Configuración — `/api/configuracion`

| Método | Ruta                  | Auth | Roles      |
|--------|-----------------------|------|------------|
| `GET`  | `/api/configuracion`  | ✅   | Cualquiera |
| `POST` | `/api/configuracion`  | ✅   | `Cajero`   |
| `PUT`  | `/api/configuracion`  | ✅   | `Cajero`   |

> Es un recurso **singleton** — no lleva `{id}` en la ruta.

#### Response y Request

```json
// Response 200 / Request (POST/PUT)
{
  "id": 1,
  "metodoPagoDefaultId": 1,
  "metodoPagoDefaultNombre": "Efectivo",
  "nombreGastronomico": "La Cocina de Juan",
  "latitudPartida": -34.6037,
  "longitudPartida": -58.3816
}
```

**Latitud/Longitud de partida** → coordenadas GPS del local, usadas para calcular distancias en entregas.

---

## 4. SignalR — Tiempo Real (WebSocket)

**Hub URL:** `/hubs/logistica`  
**Autenticación:** JWT vía query string `?access_token={token}`  
**Sin rate limiting** (las conexiones WebSocket están excluidas).

### Conexión desde Android

```kotlin
// Con Microsoft.AspNetCore.SignalR.Client (NuGet o similar)
val connection = HubConnectionBuilder
    .create("https://api-url/hubs/logistica")
    .withUrl("wss://api-url/hubs/logistica") { options ->
        options.accessTokenProvider = { token }
    }
    .build()
```

### Grupos

| Grupo                     | Quién puede unirse        | Qué recibe                                     |
|---------------------------|---------------------------|------------------------------------------------|
| `cocina`                  | Cajero o Cocina           | Nuevos pedidos, cambios de estado              |
| `pedido_{id}`             | Cualquiera autenticado     | Cambios de estado, repartidor asignado, demoras |
| `pedido_repartidor_{id}`  | Solo Repartidor           | Posiciones GPS del repartidor                   |

### Métodos del Hub (cliente → servidor)

| Método                | Parámetros                                | Quién      |
|-----------------------|-------------------------------------------|------------|
| `UnirseAGrupo`        | `grupo: string`                           | Cualquiera |
| `UnirseAPedido`       | `pedidoId: int`                           | Cualquiera |
| `SalirDePedido`       | `pedidoId: int`                           | Cualquiera |
| `EnviarPosicionGPS`   | `repartidorId, latitud, longitud`         | Repartidor |

### Eventos (servidor → cliente)

| Evento                   | Payload (JSON)                                                                | Grupo destino            |
|--------------------------|-------------------------------------------------------------------------------|--------------------------|
| `NuevoPedido`            | `{ "pedidoId": 1, "cliente": "...", "total": 15000, "fecha": "..." }`       | `cocina`                 |
| `EstadoCambiado`         | `{ "pedidoId": 1, "estadoAnterior": "Pendiente", "estadoNuevo": "...", "fecha": "..." }` | `pedido_{id}` |
| `PedidoActualizado`      | `{ "pedidoId": 1, "estado": "...", "fecha": "..." }`                        | `cocina`                 |
| `RepartidorAsignado`     | `{ "pedidoId": 1, "repartidorId": 5, "nombreRepartidor": "...", "fecha": "..." }` | `pedido_{id}`   |
| `DemoraRegistrada`       | `{ "pedidoId": 1, "motivo": "...", "tiempoEstimadoMinutos": 15, "fecha": "..." }` | `pedido_{id}`  |
| `PosicionGPSActualizada` | `{ "repartidorId": 5, "latitud": -34.6, "longitud": -58.4, "fecha": "..." }`| `pedido_repartidor_{id}` |
| `PedidoFinalizado`       | `{ "pedidoId": 1, "estadoFinal": "Entregado", "fecha": "..." }`             | `pedido_{id}`            |

---

## 5. Entidades y Relaciones

### Tablas

```
┌─────────────┐     ┌────────────────┐     ┌───────────────┐
│    roles     │     │   usuarios     │     │   productos   │
├─────────────┤     ├────────────────┤     ├───────────────┤
│ id (PK)     │◄────│ rol_id (FK)    │     │ id (PK)       │
│ nombre (UQ) │     │ id (PK)        │     │ nombre (UQ)   │
└─────────────┘     │ usuario (UQ)   │     │ precio        │
                    │ password_hash  │     │ demora (min)  │
                    │ disponible     │     │ activo        │
                    │ activo         │     └───────┬───────┘
                    └────────┬───────┘             │
                             │                     │
              ┌──────────────┼──────────┐          │
              │              │          │          │
              ▼              ▼          ▼          ▼
    ┌────────────┐  ┌──────────────┐  ┌────────────────────┐
    │   cajas    │  │   pedidos    │  │  detalle_pedidos   │
    ├────────────┤  ├──────────────┤  ├────────────────────┤
    │ id (PK)    │  │ id (PK)      │  │ pedido_id (PK,FK)  │
    │ usuario_   │  │ caja_id (FK) │  │ producto_id(PK,FK) │
    │  apertura  │  │ repartidor_  │  │ nombre (snapshot)  │
    │   _id (FK) │  │   id (FK)    │  │ precio (snapshot)  │
    │ usuario_   │  │ estado_id(FK)│  │ cantidad           │
    │  cierre    │  │ metodo_pago_ │  └────────────────────┘
    │   _id (FK) │  │   id (FK)    │
    │ monto_     │  │ metodo_venta_│
    │  apertura  │  │   id (FK)    │
    │ monto_     │  │ cliente_     │     ┌─────────────────┐
    │  cierre_   │  │   nombre     │     │     demoras      │
    │  teorico   │  │ cliente_     │     ├─────────────────┤
    │ monto_     │  │   direccion  │     │ id (PK)         │
    │  cierre_   │  │ demora_aprox │     │ pedido_id (FK)  │
    │  real      │  │ latitud_     │     │ usuario_id (FK) │
    └────────────┘  │   destino    │     │ demora (min)    │
                    │ longitud_    │     │ sector          │
                    │   destino    │     │ observaciones   │
                    │ total_       │     └─────────────────┘
                    │   estimado   │
                    │ fechas de    │
                    │   tracking   │
                    └──────┬───────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
    ┌──────────────────┐     ┌──────────────────┐
    │  estados_pedidos │     │   metodo_pago    │
    ├──────────────────┤     ├──────────────────┤
    │ id (PK)          │     │ id (PK)          │
    │ nombre (UQ)      │     │ nombre           │
    └──────────────────┘     └──────────────────┘

    ┌──────────────────┐     ┌──────────────────┐
    │  metodo_venta    │     │  configuracion   │
    ├──────────────────┤     ├──────────────────┤
    │ id (PK)          │     │ id (PK)          │
    │ nombre           │     │ metodo_pago_     │
    └──────────────────┘     │   default_id (FK)│
                             │ nombreGastronomico│
                             │ latitud_partida  │
                             │ longitud_partida │
                             └──────────────────┘
```

### Relaciones clave

| Relación                 | Tipo          | FK               | On Delete  |
|--------------------------|---------------|------------------|------------|
| Usuario → Rol            | M:1           | `rol_id`         | Restrict   |
| Pedido → Caja            | M:1 (opcional)| `caja_id`        | SetNull    |
| Pedido → Repartidor      | M:1 (opcional)| `repartidor_id`  | SetNull    |
| Pedido → EstadoPedido    | M:1           | `estado_id`      | Restrict   |
| Pedido → MetodoPago      | M:1           | `metodo_pago_id` | Restrict   |
| Pedido → MetodoVenta     | M:1           | `metodo_venta_id`| Restrict   |
| DetallePedido → Pedido   | M:1           | `pedido_id`      | Cascade    |
| DetallePedido → Producto | M:1           | `producto_id`    | Restrict   |
| Caja → Usuario (apertura)| M:1           | `usuario_apertura_id` | Restrict |
| Caja → Usuario (cierre)  | M:1 (opcional)| `usuario_cierre_id` | SetNull  |
| Demora → Pedido          | M:1           | `pedido_id`      | Cascade    |
| Demora → Usuario         | M:1           | `usuario_id`     | Restrict   |

### Catálogos (datos seedeados)

| Tabla             | Valores                                              |
|-------------------|------------------------------------------------------|
| `roles`           | `Cajero`, `Cocina`, `Repartidor`                     |
| `estados_pedidos` | `Pendiente`…`Devuelto` (8 estados, ver sección 3.2)   |
| `metodo_pago`     | `Efectivo`, `Transferencia`, `Tarjeta`               |
| `metodo_venta`    | `Delivery`, `Retiro en local`                        |
| `productos`       | 25 productos: 15 de cocina + 10 bebidas              |

---

## 6. Rate Limiting

- **Global:** 100 requests/minuto por usuario autenticado (`sub` claim). Si no está autenticado, por IP.
- **Login:** 10 requests/minuto por IP (`LoginPolicy`).
- **SignalR:** excluido completamente.
- **Código 429:** incluye header `Retry-After` con segundos restantes.

---

## 7. Entidades Sugeridas para Android (Kotlin)

Recomiendo modelar estas data classes y repositorios en tu app nativa Android:

### 7.1 Data Classes (Kotlin)

```kotlin
// ── Auth ──
data class LoginRequest(val usuarioNombre: String, val password: String)
data class LoginResponse(
    val id: Int, val usuarioNombre: String, val rolId: Int,
    val rolNombre: String, val token: String, val expiraEn: String
)

// ── Producto ──
data class Producto(
    val id: Int, val nombre: String, val precio: Double,
    val demora: Int, val activo: Boolean
)

// ── Pedido ──
data class PedidoResumen(
    val id: Int, val estado: String, val clienteNombre: String?,
    val metodoVenta: String?, val totalEstimado: Double, val fechaIngreso: String
)

data class PedidoDetalle(
    val id: Int, val estado: String, val clienteNombre: String?,
    val clienteDireccion: String?, val metodoVenta: String?,
    val metodoPago: String?, val totalEstimado: Double, val demoraAprox: Int?,
    val latitudDestino: Double?, val longitudDestino: Double?,
    val fechaIngreso: String, val fechaEstimadoFin: String?,
    val fechaAsignado: String?, val fechaEnCamino: String?,
    val fechaFinalizado: String?, val repartidorNombre: String?,
    val cajaId: Int?, val estadoId: Int,
    val detallePedidos: List<DetallePedido>
)

data class DetallePedido(
    val productoId: Int, val nombre: String, val cantidad: Int,
    val precio: Double, val tiempoMaquina: Int
)

data class CrearPedidoRequest(
    val cajaId: Int?, val metodoPagoId: Int, val metodoVentaId: Int,
    val clienteNombre: String?, val clienteDireccion: String?,
    val latitudDestino: Double?, val longitudDestino: Double?,
    val totalEstimado: Double, val demoraAprox: Int?,
    val detalles: List<CrearDetalleRequest>
)

data class CrearDetalleRequest(
    val productoId: Int, val nombre: String, val precio: Double, val cantidad: Int
)

// ── Caja ──
data class Caja(
    val id: Int, val usuarioAperturaId: Int, val usuarioAperturaNombre: String,
    val usuarioCierreId: Int?, val usuarioCierreNombre: String?,
    val fechaApertura: String, val fechaCierre: String?,
    val montoApertura: BigDecimal, val montoCierreTeorico: BigDecimal?,
    val montoCierreReal: BigDecimal?, val estado: String
)

// ── Demora ──
data class Demora(
    val id: Int, val pedidoId: Int, val usuarioId: Int,
    val demoraMinutos: Int, val sector: String?, val observaciones: String?
)

// ── Configuración ──
data class Configuracion(
    val id: Int, val metodoPagoDefaultId: Int?, val metodoPagoDefaultNombre: String?,
    val nombreGastronomico: String?, val latitudPartida: Double?,
    val longitudPartida: Double?
)
```

### 7.2 Repositorios Sugeridos

```
AuthRepository     → login(), refreshToken()
ProductoRepository → getProductos(), getProducto(id), create(), update(), delete()
PedidoRepository   → getPedidos(), getPedido(id), getByEstado(estado),
                     crearPedido(), cambiarEstado(id, estado), asignarRepartidor(id, repId)
CajaRepository     → getCajas(estado?), getCaja(id), abrirCaja(), cerrarCaja(id)
DemoraRepository   → getDemoras(pedidoId), registrar(), actualizar(), eliminar()
ConfigRepository   → getConfig(), updateConfig()
```

### 7.3 SignalR Service

```kotlin
interface SignalRService {
    suspend fun connect(token: String)
    suspend fun unirseACocina()
    suspend fun unirseAPedido(pedidoId: Int)
    suspend fun salirDePedido(pedidoId: Int)
    suspend fun enviarPosicion(repartidorId: Int, lat: Double, lng: Double)

    // Flow de eventos para observar con StateFlow
    val nuevosPedidos: Flow<NuevoPedidoMessage>
    val cambiosEstado: Flow<EstadoCambiadoMessage>
    val repartidorAsignado: Flow<RepartidorAsignadoMessage>
    val demoraRegistrada: Flow<DemoraRegistradaMessage>
    val posicionGPS: Flow<PosicionGPSActualizadaMessage>
    val pedidoFinalizado: Flow<PedidoFinalizadoMessage>
}
```

### 7.4 Flujo de Datos Recomendado

```
LoginScreen → AuthRepository.login() → guardar token en DataStore/EncryptedSharedPrefs
    ↓
HomeScreen → según rol:
    ├─ Cajero: Pantalla de pedidos activos + crear pedido + abrir/cerrar caja
    ├─ Cocina: Pantalla de pedidos pendientes (SignalR para recibir nuevos)
    └─ Repartidor: Pantalla de entregas asignadas + mapa con GPS en tiempo real

PedidoDetailScreen:
    ├─ Ver detalle del pedido (GET /api/pedidos/{id})
    ├─ Cambiar estado (PATCH /api/pedidos/{id}/estado)
    ├─ Asignar repartidor (PATCH /api/pedidos/{id}/repartidor)
    └─ Registrar demora (POST /api/demoras)

MapaScreen (Repartidor):
    ├─ SignalR → unirse a pedido_repartidor_{id}
    ├─ EnviarPosicionGPS cada N segundos
    └─ Recibir actualizaciones de pedido vía SignalR
```

---

## 8. Observaciones para Desarrollo

1. **Token JWT:** Guardalo en `EncryptedSharedPreferences` (Android). Enviá el header `Authorization: Bearer {token}` en cada request REST y como `access_token` en el query string para SignalR.

2. **Retrofit / Ktor Client:** Armá una interfaz con los endpoints REST. Usá un interceptor que agregue el token y maneje 401 → redirigir al login.

3. **SignalR en Android:** Usá la librería oficial `Microsoft.AspNetCore.SignalR.Client` (disponible vía NuGet o Maven). Alternativa: `signalr-client` de Android si usás Java/Kotlin puro sin .NET. O implementá el protocolo manualmente con OkHttp WebSocket + JSON.

4. **GPS en Repartidor:** El endpoint `/hubs/logistica` recibe `EnviarPosicionGPS(repartidorId, lat, lng)`. Hacelo cada 5-10 segundos mientras el repartidor esté en `EnCamino`. Del lado del cliente (cocina/cliente final), suscribite al evento `PosicionGPSActualizada` del grupo `pedido_repartidor_{id}`.

5. **Manejo de errores:** Toda respuesta de error tiene forma `{ "mensaje": "..." }`. Códigos comunes:
    - `400` → request mal formado
    - `401` → token inválido o expirado
    - `403` → sin permisos para la acción
    - `404` → recurso no encontrado
    - `409` → conflicto (ej: nombre duplicado)
    - `429` → rate limit excedido

6. **Fechas:** Todas las fechas vienen en UTC (ISO 8601). Convertilas a la timezone local del dispositivo para mostrar.

7. **Precios:** Están en centavos? No, son valores directos en pesos argentinos (ej: 8500 = $8500 ARS). Usá `BigDecimal` o `Double` en Kotlin — pero para operaciones monetarias serias, preferí `BigDecimal`.
