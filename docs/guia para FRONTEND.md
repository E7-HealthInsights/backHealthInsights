# Backend API — Guía para Frontend (Health Insights)

Documento de referencia **autocontenido** para que el frontend consuma el backend sin necesidad de abrir este repo. Incluye stack, autenticación, CORS, todos los endpoints, DTOs exactos, códigos de error, modelos de datos y datos semilla.

> Backend: **Quarkus 3** (Java 21) + MySQL + Firebase Admin (Auth).
> Arquitectura: Clean Architecture (domain / application / infrastructure / interfaces).

---

## 1. Datos de conexión

| Item | Valor |
|---|---|
| Base URL (dev) | `http://localhost:8080` |
| Content-Type | `application/json` |
| Auth | Firebase ID Token en header `Authorization: Bearer <idToken>` |
| CORS `Access-Control-Allow-Origin` | `http://localhost:5173` (Vite dev) |
| CORS métodos | `GET, POST, PUT, DELETE, OPTIONS` |
| CORS headers | `Authorization, Content-Type, Accept` |
| Credenciales | `Access-Control-Allow-Credentials: true` |
| Dev UI de Quarkus | `http://localhost:8080/q/dev/` (solo dev) |

> El frontend debe correr en `http://localhost:5173`. Si cambia el puerto, hay que actualizar `CorsFilter` en el backend.

---

## 2. Autenticación

### 2.1 Flujo general
1. El usuario se autentica con **Firebase Auth** desde el frontend (SDK web). Ese usuario debe existir previamente en la BD del backend (creado vía `POST /users` por un ADMIN).
2. Tras el login, el frontend obtiene un **ID Token** de Firebase (`getIdToken()`).
3. El frontend envía ese token en **cada request** al backend:
   ```
   Authorization: Bearer <firebase_id_token>
   ```
4. El backend (`FirebaseAuthFilter`) valida el token con Firebase Admin SDK, busca el usuario en BD por `provider_id == firebase uid`, y lo pone en el `AuthContext`.

### 2.2 Rutas públicas (no requieren token)
- Cualquier ruta bajo `/q/*` (Dev UI de Quarkus).
- Requests con método `OPTIONS` (preflight CORS).

**Todo lo demás requiere `Authorization: Bearer <token>` válido**, incluido `POST /users` (la creación de usuarios exige estar autenticado; en la práctica la hace un ADMIN).

### 2.3 Respuestas de auth
| Situación | Status | Body |
|---|---|---|
| Sin header `Authorization` | `401` | `"No autorizado"` (texto plano) |
| Header no empieza con `Bearer ` | `401` | `"No autorizado"` |
| Token inválido / expirado / revocado | `401` | `"No autorizado"` |
| Token válido pero el usuario no existe en BD | `401` | `"No autorizado"` |

> Nota: estos cuerpos son `text/plain`, no JSON. Los errores de negocio (400/404/409/500) sí devuelven JSON `ErrorResponseDto`.

---

## 3. Formato común de errores

Para errores manejados de negocio, el backend devuelve JSON con este shape:

```ts
type ErrorResponseDto = {
  message: string;
};
```

Ejemplo:
```json
{ "message": "El email juan@example.com ya está registrado" }
```

---

## 4. Endpoints

Resumen:

| Método | Path | Auth | Descripción |
|---|---|---|---|
| `GET` | `/auth/me` | ✅ | Datos del usuario autenticado |
| `POST` | `/users` | ✅ | Crear usuario (rol ADMIN en la práctica) |
| `GET` | `/datasets` | ✅ | Listar datasets activos |
| `GET` | `/datasets/{id}/metricas` | ✅ | Listar métricas (columnas) del dataset |

---

### 4.1 `GET /auth/me`

Devuelve el perfil del usuario dueño del token.

**Response 200 — `UserResponseDto`**
```ts
type UserResponseDto = {
  id: string;        // UUID
  name: string;
  lastName: string;
  email: string;
  role: "ADMIN" | "DIRECTOR_GENERAL" | "DIRECTOR_FINANZAS" | "DIRECTOR_MERCADOTECNIA";
  status: boolean;
};
```

Ejemplo:
```json
{
  "id": "08631269-3f4c-4299-a1e7-23f5684e1091",
  "name": "Santiago",
  "lastName": "Niño",
  "email": "santiago.nino@example.com",
  "role": "ADMIN",
  "status": true
}
```

Errores: `401` si el token es inválido o el usuario no existe en BD.

---

### 4.2 `POST /users`

Crea un usuario en Firebase y en la BD. Pensado para uso por un ADMIN.

**Request body — `CreateUserDto`**
```ts
type CreateUserDto = {
  name: string;      // 2..100, requerido
  lastName: string;  // 2..100, requerido
  email: string;     // email válido, <=150, requerido
  password: string;  // 8..64, al menos 1 minúscula, 1 mayúscula, 1 dígito
  roleId: number;    // byte >=1 (ver tabla de roles)
};
```

Regla de password (regex): `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$`.

**Response 201** — devuelve el `User` de dominio creado (incluye `role` como objeto anidado y `providerId` de Firebase):
```json
{
  "id": "uuid-generado",
  "name": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com",
  "role": { "id": 3, "name": "DIRECTOR_FINANZAS" },
  "status": true,
  "providerId": "firebase-uid"
}
```

**Respuestas de error**

| Status | Cuándo | Body (`ErrorResponseDto`) |
|---|---|---|
| `400` | `roleId` no existe en tabla Role | `{ "message": "El rol con id X no existe" }` |
| `400` | Validaciones de Bean Validation fallan (`@NotBlank`, `@Size`, `@Email`, `@Pattern`…) | Formato estándar de Quarkus (ver nota abajo) |
| `409` | Email ya registrado (en BD o en Firebase) | `{ "message": "El email X ya está registrado" }` |
| `500` | Error de Firebase u otro no contemplado | `{ "message": "Error interno del servidor" }` |

> Nota: los 400 por validación los genera Quarkus/RestEasy automáticamente; el shape exacto incluye la lista de violaciones (campo + mensaje localizado). Los mensajes en español que devuelve el backend están definidos en `CreateUserDto` (p. ej. "El nombre es obligatorio", "La contraseña debe contener al menos una minúscula, una mayúscula y un número").

---

### 4.3 `GET /datasets`

Lista los datasets **activos** (`estado = true`).

**Response 200 — `DatasetResponseDto[]`**
```ts
type DatasetResponseDto = {
  id: string;                   // UUID
  nombre: string;
  descripcion: string;
  fuente: string;
  link: string | null;
  fechaActualizacion: string;   // ISO-8601 LocalDateTime, sin zona: "2024-01-15T00:00:00"
};
```

> `nombreTabla`, `archivoCsv` y `estado` del dominio **no se exponen**.

Ejemplo:
```json
[
  {
    "id": "e1000000-0000-0000-0000-000000000001",
    "nombre": "Diabetes México 2023",
    "descripcion": "Casos y defunciones por diabetes en México, desglosados por estado y sexo.",
    "fuente": "SINAVE / Secretaría de Salud",
    "link": null,
    "fechaActualizacion": "2024-01-15T00:00:00"
  }
]
```

---

### 4.4 `GET /datasets/{id}/metricas`

Devuelve las métricas (columnas disponibles del CSV) del dataset. Valida que el dataset exista y esté activo.

**Path param:** `id` — UUID del dataset.

**Response 200 — `MetricaResponseDto[]`**
```ts
type MetricaResponseDto = {
  id: string;          // UUID
  nombre: string;      // nombre visible, ej "Edad promedio"
  columnaCsv: string;  // nombre real de columna en el CSV, ej "edad_promedio"
  unidad: string | null; // ej "%", "años", "MXN", o null
};
```

**Errores**

| Status | Cuándo | Body |
|---|---|---|
| `404` | Dataset no existe o `estado=false` | `{ "message": "Dataset no encontrado: <uuid>" }` |

Ejemplo:
```json
[
  { "id": "11000000-0000-0000-0000-000000000005", "nombre": "Edad promedio", "columnaCsv": "edad_promedio", "unidad": "años" },
  { "id": "11000000-0000-0000-0000-000000000007", "nombre": "Presupuesto",   "columnaCsv": "presupuesto",   "unidad": "MXN" }
]
```

---

## 5. Modelo de datos (referencia)

### 5.1 Roles (tabla `Role`, PK `TINYINT`)

| id | name |
|---|---|
| 1 | `ADMIN` |
| 2 | `DIRECTOR_GENERAL` |
| 3 | `DIRECTOR_FINANZAS` |
| 4 | `DIRECTOR_MERCADOTECNIA` |

> Los roles se siembran al arrancar (estrategia `drop-and-create` + `import.sql`). Úsalos como literales en el frontend.

### 5.2 Entidades relevantes

```
Users(id UUID, name, last_name, email UNIQUE, role_id → Role.id, status bool, provider_id)
Dataset(id UUID, nombre, nombre_tabla, descripcion, fuente, archivo_csv, link, estado bool, fecha_actualizacion)
Metrica(id UUID, nombre, columna_csv, unidad, dataset_id → Dataset.id)
```

### 5.3 Datos semilla (IDs fijos, útiles para mocks/tests)

Datasets:
- `e1000000-0000-0000-0000-000000000001` — **Diabetes México 2023**
- `e2000000-0000-0000-0000-000000000002` — **Hipertensión 2022**
- `e3000000-0000-0000-0000-000000000003` — **Obesidad Nacional 2021**

Usuario ADMIN de prueba:
- email: `santiago.nino@example.com`
- Firebase UID: `i8AULkutUNTy9xIUyp2lpHczMHi2`

> La BD usa `drop-and-create` en dev: **cada reinicio del backend borra y recrea todo** con `import.sql`. No persiste nada entre corridas.

---

## 6. Convenciones y notas

- **IDs**: todos los recursos usan `UUID` string (36 chars), excepto `roleId` que es `byte` (1..127).
- **Fechas**: `LocalDateTime` serializado como ISO sin zona horaria: `"YYYY-MM-DDTHH:mm:ss"`. No viene con `Z`.
- **Idioma**: mensajes de error en español.
- **Nombres**: los DTOs de dominio (datasets/métricas) usan español (`nombre`, `descripcion`, `fuente`); los de usuarios usan inglés (`name`, `lastName`, `email`).
- **Sin paginación**: `GET /datasets` y `/metricas` devuelven arrays completos.
- **Sin endpoints de listar/editar/borrar usuarios** todavía (solo crear + `me`).
- **Sin endpoints de dashboards / elementos de dashboard / consumo de CSV** todavía — solo catálogo de datasets y sus columnas.

---

## 7. Cheatsheet para fetch (TS)

```ts
const API = "http://localhost:8080";

async function authedFetch(path: string, init: RequestInit = {}) {
  const idToken = await firebase.auth().currentUser!.getIdToken();
  const res = await fetch(`${API}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
      ...(init.headers ?? {}),
    },
  });
  if (!res.ok) {
    // 401 devuelve text/plain; el resto devuelve ErrorResponseDto JSON
    const body = res.headers.get("content-type")?.includes("json")
      ? await res.json()
      : { message: await res.text() };
    throw Object.assign(new Error(body.message ?? res.statusText), { status: res.status, body });
  }
  return res.status === 204 ? null : res.json();
}

// Ejemplos
const me        = await authedFetch("/auth/me");
const datasets  = await authedFetch("/datasets");
const metricas  = await authedFetch(`/datasets/${datasetId}/metricas`);
const created   = await authedFetch("/users", {
  method: "POST",
  body: JSON.stringify({
    name: "Juan", lastName: "Pérez", email: "juan@ex.com",
    password: "Passw0rd!", roleId: 3,
  }),
});
```

---

## 8. Changelog de este doc

- **2026-04-23**: versión inicial, cubre `/auth/me`, `/users` (POST), `/datasets`, `/datasets/{id}/metricas`.

> Si agregas/modificas un endpoint en el backend, actualiza este archivo en el mismo PR.
