# API REST PetHaven — Contrato

## Convenciones generales

| Tema | Regla |
|---|---|
| Base URL | `http://localhost:8080/api` |
| Formato | JSON, nombres `camelCase` |
| Entidades | **Nunca** se serializan: toda entrada/salida pasa por DTOs `record` (`dto/request`, `dto/response`) y mappers explícitos |
| IDs | `Long` numéricos |
| Especie | `"DOG"` \| `"CAT"` |
| Booleanos | nombre final del JSON: `active` (nunca `isActive`) |
| Fechas | ISO-8601. Datos de negocio con `LocalDateTime` (`2026-08-01T09:00:00`); errores con `Instant` UTC |
| Contraseñas | nunca en respuestas; opcionales en actualización |
| Creación | `201 Created` + cabecera `Location` |
| Eliminación | `204 No Content` |
| CORS | `http://localhost:4200` sobre `/api/**` (`config/CorsConfig.java`), sin credenciales por ahora |
| Advice REST | `RestExceptionHandler` aplica solo a `@RestController`; convive con el handler MVC de Thymeleaf |

## Formato de error

Todas las excepciones REST usan `ApiErrorResponse`:

```json
{
  "timestamp": "2026-10-09T14:03:21.551Z",
  "status": 404,
  "error": "Not Found",
  "message": "El recurso 'Pet' con id: '99' no fue encontrado",
  "path": "/api/pets/99",
  "errors": {}
}
```

- `message`: legible para el usuario, en español.
- `errors`: mapa `campo → mensaje` en errores de validación (400); `{}` en el resto.

| Código | Uso |
|---|---|
| `200 OK` | consulta o actualización exitosa |
| `201 Created` | creación exitosa (con `Location`) |
| `204 No Content` | eliminación exitosa |
| `400 Bad Request` | body o parámetros inválidos (incluye validación) |
| `401 Unauthorized` | credenciales inválidas |
| `403 Forbidden` | cuenta desactivada |
| `404 Not Found` | recurso inexistente |
| `409 Conflict` | regla de negocio o restricción de unicidad |
| `500 Internal Server Error` | error inesperado (se registra en logs) |

## Fase 1 — Dueños

Controller: `OwnerRestController` → `@RequestMapping("/api/owners")`

| DTO | Campos |
|---|---|
| `OwnerRequest` | `document`, `name`, `email`, `password`, `phone` (todos obligatorios) |
| `OwnerUpdateRequest` | igual que el anterior, con `password` opcional |
| `OwnerResponse` | `id`, `document`, `name`, `email`, `phone` |

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/api/owners` | `200` `[OwnerResponse]` |
| `GET` | `/api/owners/{id}` | `200` `OwnerResponse` |
| `GET` | `/api/owners/{id}/pets` | `200` `[PetResponse]` (disponible al cerrar Fase 2) |
| `POST` | `/api/owners` | `201` `OwnerResponse` + `Location` |
| `PUT` | `/api/owners/{id}` | `200` `OwnerResponse` |
| `DELETE` | `/api/owners/{id}` | `204`; `409` si alguna mascota tiene tratamientos |

## Fase 2 — Mascotas

Controller: `PetRestController` (se reescribe) → `@RequestMapping("/api/pets")`

| DTO | Campos |
|---|---|
| `PetRequest` | `name`, `ownerId`, `species`, `breed`, `age`, `weight`, `disease`, `photoUrl`, `active` |
| `PetResponse` | `id`, `name`, `species`, `breed`, `age`, `weight`, `disease`, `photoUrl`, `active`, `ownerId`, `ownerName` |

```json
{
  "id": 1,
  "name": "Buddy",
  "species": "DOG",
  "breed": "Golden Retriever",
  "age": 3,
  "weight": 15.0,
  "disease": null,
  "photoUrl": "https://...",
  "active": true,
  "ownerId": 31,
  "ownerName": "Diego Aguilar"
}
```

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/api/pets` | `200` `[PetResponse]` |
| `GET` | `/api/pets/{id}` | `200` `PetResponse` |
| `POST` | `/api/pets` | `201` `PetResponse` + `Location` |
| `PUT` | `/api/pets/{id}` | `200` `PetResponse` (puede reasignar dueño) |
| `PUT` | `/api/pets/{id}/active?active=true\|false` | `200` `PetResponse` |
| — | *(sin DELETE)* | la mascota solo se desactiva; se elimina en cascada con su dueño |

> `GET /api/pets` actual devuelve entidades y está roto por el ciclo de serialización
> `Pet ↔ Owner`; no consumir hasta que cierre la Fase 2.

## Fase 3 — Veterinarios

Controller: `VeterinarianRestController` → `@RequestMapping("/api/veterinarians")`

| DTO | Campos |
|---|---|
| `VeterinarianRequest` | `document`, `name`, `email`, `password`, `specialty`, `photoUrl` |
| `VeterinarianUpdateRequest` | igual, con `password` opcional |
| `VeterinarianResponse` | `id`, `document`, `name`, `email`, `specialty`, `photoUrl`, `attentions`, `active` |

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/api/veterinarians` | `200` `[VeterinarianResponse]` |
| `GET` | `/api/veterinarians/{id}` | `200` `VeterinarianResponse` |
| `GET` | `/api/veterinarians/{id}/treatments` | `200` `[TreatmentResponse]` (Fase 5) |
| `POST` | `/api/veterinarians` | `201` + `Location` |
| `PUT` | `/api/veterinarians/{id}` | `200` |
| `PUT` | `/api/veterinarians/{id}/active?active=true\|false` | `200` |
| — | *(sin DELETE)* | solo se desactiva (historial de tratamientos) |

## Fase 4 — Drogas

Controller: `DrugRestController` → `@RequestMapping("/api/drugs")`

| DTO | Campos |
|---|---|
| `DrugRequest` | `name`, `purchasePrice`, `salePrice`, `unitsAvailable` (`unitsSold` lo maneja el servidor) |
| `DrugResponse` | `id`, `name`, `purchasePrice`, `salePrice`, `unitsAvailable`, `unitsSold` |

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/api/drugs` | `200` `[DrugResponse]` |
| `GET` | `/api/drugs/{id}` | `200` `DrugResponse` |
| `POST` | `/api/drugs` | `201` + `Location` |
| `PUT` | `/api/drugs/{id}` | `200` `DrugResponse` |
| `DELETE` | `/api/drugs/{id}` | `204`; `409` si está asociada a tratamientos |

## Fase 5 — Tratamientos y drogas administradas

Controllers: `TreatmentRestController` y `TreatmentDrugRestController`

| DTO | Campos |
|---|---|
| `TreatmentRequest` | `petId`, `veterinarianId`, `date` (opcional; por defecto, ahora) |
| `TreatmentResponse` | `id`, `date`, `petId`, `petName`, `veterinarianId`, `veterinarianName`, `drugs` |
| `TreatmentDrugRequest` | `drugId`, `units` |
| `TreatmentDrugResponse` | `id`, `drugId`, `drugName`, `units`, `unitPurchasePrice`, `unitSalePrice` |

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/api/treatments` | `200` `[TreatmentResponse]` |
| `GET` | `/api/treatments/{id}` | `200` `TreatmentResponse` |
| `POST` | `/api/treatments` | `201` + `Location`; incrementa atenciones del veterinario |
| `GET` | `/api/treatments/{id}/drugs` | `200` `[TreatmentDrugResponse]` |
| `POST` | `/api/treatments/{id}/drugs` | `201`; descuenta inventario; `409` sin stock o droga repetida |
| `GET` | `/api/pets/{id}/treatments` | `200` `[TreatmentResponse]` (historial) |
| `GET` | `/api/veterinarians/{id}/treatments` | `200` `[TreatmentResponse]` (mascotas atendidas) |

## Fase 6 — Autenticación (provisional, sin Spring Security)

Controller: `AuthRestController` → `@RequestMapping("/api/auth")`

| DTO | Campos |
|---|---|
| `LoginRequest` | `email`, `password` |
| `AuthResponse` | `id`, `name`, `email`, `role` (`"OWNER"` \| `"VETERINARIAN"`); nunca incluye contraseña |

| Método | Ruta | Respuesta |
|---|---|---|
| `POST` | `/api/auth/owners/login` | `200` `AuthResponse`; `401` credenciales inválidas |
| `POST` | `/api/auth/veterinarians/login` | `200` `AuthResponse`; `401` credenciales; `403` cuenta desactivada |
