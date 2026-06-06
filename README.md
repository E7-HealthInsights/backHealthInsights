# HealthInsights — Backend API

> Plataforma de inteligencia de datos epidemiológicos y financieros sobre diabetes en México.
> Desarrollada como proyecto académico por el equipo 7 **NAGANIOM**.

---

## Tabla de contenidos

- [Descripción del proyecto](#descripción-del-proyecto)
- [Equipo](#equipo)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Requisitos previos](#requisitos-previos)
- [Instalación y configuración local](#instalación-y-configuración-local)
- [Variables de entorno](#variables-de-entorno)
- [Base de datos](#base-de-datos)
- [Ejecutar en desarrollo](#ejecutar-en-desarrollo)
- [Ejecutar pruebas](#ejecutar-pruebas)
- [Documentación de la API](#documentación-de-la-api)
- [Despliegue en producción](#despliegue-en-producción)
- [Módulos implementados](#módulos-implementados)
- [Créditos](#créditos)

---

## Descripción del proyecto

**HealthInsights** es una plataforma web de análisis de datos orientada a la toma de decisiones en salud pública. Integra datos epidemiológicos y financieros provenientes de fuentes oficiales (INEGI, ENSANUT, PAHO, IDF, World Bank, OECD) para ofrecer dashboards personalizados por rol, simulaciones de política pública y reportes ejecutivos.

El sistema está dirigido a tres perfiles directivos:

- **Director General** — visión macro de prevalencia, mortalidad y carga de enfermedad
- **Director de Finanzas** — análisis de gasto en salud y simulación de impacto presupuestal en prevención
- **Director de Mercadotecnia** — segmentación poblacional, detecciones y factores de riesgo

La plataforma permite cargar datasets epidemiológicos en CSV, visualizarlos mediante widgets dinámicos configurables, y proyectar escenarios de intervención a 5, 10, 15 y 25 años basados en literatura científica validada.

---

## Equipo 7

**NAGANIOM** — Equipo de desarrollo, Instituto Tecnológico y de Estudios Superiores de Monterrey

| Nombre | Contacto |
|--------|-----|
| Santiago Ramírez Niño (Líder) | a01665906@tec.mx |
| Omar Llano Tostado | a01666730@tec.mx |
| Gabriel Gutiérrez Guerra | a01660505@tec.mx |
| Alejandro Vargas | a01659714@tec.mx |

---

## Stack tecnológico

### Backend
| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Quarkus | 3.x | Framework backend |
| Hibernate ORM + Panache | — | ORM y acceso a datos |
| MySQL | 8.x | Base de datos producción |
| H2 | — | Base de datos en pruebas |
| Firebase Admin SDK | — | Autenticación JWT |
| Google Cloud Storage | — | Almacenamiento de archivos CSV |
| Apache Kafka | — | Procesamiento asíncrono de datasets |
| SmallRye OpenAPI | — | Documentación de la API |
| JUnit 5 + Mockito | — | Pruebas unitarias e integración |

### Infraestructura GCP
| Servicio | Uso |
|---|---|
| Cloud Run | Despliegue del backend en contenedor |
| Cloud SQL (MySQL) | Base de datos en producción |
| Cloud Build | CI/CD — build y deploy automático |
| Artifact Registry | Registro de imágenes Docker |
| Cloud Storage | Almacenamiento de CSVs subidos |
| Secret Manager | Gestión segura de credenciales |

---

## Arquitectura

El proyecto sigue **Clean Architecture** con cuatro capas claramente separadas:

```
src/main/java/org/acme/
├── domain/
│   ├── models/          # Entidades de dominio (User, Dataset, Widget, Proyeccion...)
│   ├── repository/      # Interfaces de repositorio
│   └── exception/       # Excepciones de dominio
├── application/
│   ├── usecase/         # Casos de uso (lógica de negocio)
│   └── dto/             # Data Transfer Objects
├── infrastructure/
│   ├── entities/        # Entidades JPA con anotaciones Hibernate
│   ├── mapper/          # Conversión domain ↔ entity
│   ├── repository/      # Implementaciones Panache de los repositorios
│   ├── query/           # QueryExecutor — ejecución de Stored Procedures
│   └── security/        # FirebaseAuthFilter, AuthContext
└── interfaces/
    └── rest/            # Resources JAX-RS (endpoints HTTP)
```

---

## Requisitos previos

Antes de instalar, asegúrate de tener:

- **JDK 21** — [Descargar](https://adoptium.net/)
- **Maven 3.9+** — incluido via `./mvnw`
- **MySQL 8.x** corriendo localmente en el puerto `3306`
- **Docker** (opcional, para Kafka local)
- **Firebase** — cuenta de servicio JSON del proyecto
- Una base de datos `health_insights` creada en MySQL

---

## Instalación y configuración local

### 1. Clona el repositorio

```bash
git clone https://github.com/TU_ORG/healthinsights-backend.git
cd healthinsights-backend
```

### 2. Crea la base de datos

```sql
CREATE DATABASE health_insights CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Ejecuta el schema inicial y los datos de catálogo:

```bash
mysql -u root -p health_insights < src/main/resources/db/schema.sql
mysql -u root -p health_insights < src/main/resources/db/stored_procedures.sql
mysql -u root -p health_insights < src/main/resources/db/stored_functions.sql
```

### 3. Agrega las credenciales de Firebase

Descarga el JSON de Firebase Admin SDK desde la consola de Firebase y colócalo en:

```
src/main/resources/healthinsights-firebase-adminsdk.json
```

> ⚠️ Este archivo está en `.gitignore` — nunca se sube al repositorio.

### 4. Crea el archivo `.env`

Crea un archivo `.env` en la raíz del proyecto:

```bash
# Base de datos
DB_KIND=mysql
DB_USERNAME=root
DB_PASSWORD=tu_password_local
DB_JDBC_URL=jdbc:mysql://localhost:3306/health_insights
DB_SCHEMA_STRATEGY=validate

# Firebase
FIREBASE_SERVICE_ACCOUNT_LOCATION=src/main/resources/healthinsights-firebase-adminsdk.json

# CORS
CORS_ORIGINS=http://localhost:5173

# Google Cloud Storage
GCS_BUCKET_NAME=health-insights-csv-uploads-dev

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP=health-insights-ingestor

# OpenAI
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxx
OPENAI_MODEL=gpt-4o-mini
OPENAI_ENDPOINT=https://api.openai.com/v1/chat/completions
OPENAI_TIMEOUT=60
```

---

## Variables de entorno

| Variable | Descripción | Requerida |
|---|---|---|
| `DB_USERNAME` | Usuario de MySQL | ✅ |
| `DB_PASSWORD` | Contraseña de MySQL | ✅ |
| `DB_JDBC_URL` | URL JDBC de la base de datos | ✅ |
| `FIREBASE_SERVICE_ACCOUNT_LOCATION` | Ruta al JSON de Firebase Admin SDK | ✅ |
| `CORS_ORIGINS` | Origen permitido por CORS (frontend URL) | ✅ |
| `GCS_BUCKET_NAME` | Nombre del bucket de GCS para CSVs | ✅ |
| `KAFKA_BOOTSTRAP_SERVERS` | Dirección del broker Kafka | ✅ |
| `OPENAI_API_KEY` | API Key de OpenAI para detección de columnas | ✅ |
| `DB_SCHEMA_STRATEGY` | Estrategia Hibernate (`validate` en prod) | ⚙️ default: `validate` |
| `OPENAI_MODEL` | Modelo de OpenAI | ⚙️ default: `gpt-4o-mini` |

En producción, todas las variables sensibles se gestionan en **GCP Secret Manager** e inyectadas como variables de entorno en Cloud Run.

---

## Base de datos

### Tablas principales

| Tabla | Descripción |
|---|---|
| `Role` | Roles del sistema (ADMIN, DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA) |
| `Users` | Usuarios de la plataforma |
| `Dataset` | Datasets epidemiológicos cargados |
| `Metrica` | Columnas/métricas de cada dataset |
| `Widget` | Widgets del dashboard por rol o personales |
| `Tipo_de_Grafica` | Catálogo de tipos de widget |
| `Proyeccion` | Escenarios de simulación guardados |
| `Reporte` | Metadata de reportes PDF generados |
| `LogActividad` | Bitácora de acciones del administrador |

### Stored Procedures

| Nombre | Descripción |
|---|---|
| `sp_widget_stat` | Ejecuta consultas de tipo STAT (valor único) |
| `sp_widget_series` | Ejecuta consultas de series temporales (LINE, BAR) |
| `sp_widget_pie` | Ejecuta consultas de distribución (PIE) |
| `sp_widget_multiseries` | Ejecuta consultas multiserie |

### Stored Functions

| Nombre | Descripción |
|---|---|
| `fn_total_usuarios(rol_id, status)` | Cuenta usuarios por rol y estatus |
| `fn_total_datasets(estado)` | Cuenta datasets por estado del enum |

### Triggers

| Nombre | Descripción |
|---|---|
| `trg_usuario_after_insert` | Registra creación de usuario en LogActividad |
| `trg_usuario_after_update` | Registra edición/baja de usuario en LogActividad |
| `trg_dataset_after_insert` | Registra carga de dataset en LogActividad |
| `trg_dataset_after_update` | Registra activación/desactivación de dataset en LogActividad |

---

## Ejecutar en desarrollo

```bash
./mvnw quarkus:dev
```

El servidor arranca en `http://localhost:8080`.

El Dev UI de Quarkus está disponible en `http://localhost:8080/q/dev/`.

---

## Ejecutar pruebas

```bash
# Todas las pruebas
./mvnw test

# Solo pruebas de una clase
./mvnw test -Dtest=ProyeccionRepositoryImplTest

# Con reporte de cobertura
./mvnw test jacoco:report
```

Las pruebas usan **H2 en memoria** con el perfil `%test` — no requieren MySQL ni Firebase corriendo.

> Las pruebas de integración usan `TestFirebaseAuthFilter` que inyecta automáticamente un usuario ADMIN sin llamar a Firebase.

---

## Documentación de la API

Con el servidor corriendo, accede a:

| URL | Descripción |
|---|---|
| `http://localhost:8080/q/swagger-ui` | Swagger UI interactivo |
| `http://localhost:8080/q/openapi` | Especificación OpenAPI en JSON |

### Endpoints principales

| Método | Endpoint | Rol requerido | Descripción |
|---|---|---|---|
| `POST` | `/auth/me` | — | Valida token Firebase y retorna usuario |
| `GET` | `/users` | ADMIN | Lista usuarios paginados |
| `POST` | `/users` | ADMIN | Crea usuario |
| `PUT` | `/users/{id}` | ADMIN | Actualiza usuario (incluye cambio de contraseña) |
| `PATCH` | `/users/{id}` | ADMIN | Desactiva usuario |
| `GET` | `/datasets` | ADMIN | Lista datasets |
| `POST` | `/datasets/upload` | ADMIN | Sube dataset CSV |
| `GET` | `/widgets` | DIRECTORES | Obtiene widgets del dashboard |
| `POST` | `/widgets` | DIRECTORES | Crea widget personal |
| `GET` | `/proyecciones` | DIRECTORES | Lista escenarios guardados |
| `POST` | `/proyecciones` | DIRECTORES | Guarda escenario |
| `PATCH` | `/proyecciones/{id}` | DIRECTORES | Edita escenario |
| `DELETE` | `/proyecciones/{id}` | DIRECTORES | Elimina escenario |
| `GET` | `/proyecciones/simular/finanzas` | DIRECTORES | Simula proyección Finanzas |
| `GET` | `/proyecciones/simular/general` | DIRECTORES | Simula proyección General |
| `GET` | `/actividad` | ADMIN | Bitácora de actividad paginada |
| `GET` | `/admin/stats` | ADMIN | Stats de usuarios y datasets |

### Autenticación

Todos los endpoints (excepto `/auth/me`) requieren un token JWT de Firebase en el header:

```
Authorization: Bearer <firebase-id-token>
```

---

## Despliegue en producción

El despliegue es automático vía **Cloud Build** al hacer merge a `main`.

```
merge a main
    → Cloud Build trigger
    → mvn package -DskipTests
    → docker build
    → push a Artifact Registry
    → deploy a Cloud Run
```

Para despliegue manual:

```bash
gcloud run deploy healthinsights-backend \
  --image REGION-docker.pkg.dev/PROJECT_ID/REPO/healthinsights-backend:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

Las variables de entorno y secretos se configuran en la consola de Cloud Run, vinculadas a **Secret Manager**.

---

## Módulos implementados

- **Autenticación** — Firebase Auth con filtro JWT, roles por base de datos
- **Gestión de usuarios** — CRUD completo, cambio de contraseña Firebase, paginación server-side
- **Gestión de datasets** — Upload CSV asíncrono via Kafka + GCS, detección de columnas con OpenAI
- **Widgets dinámicos** — 7 tipos de gráficas ejecutadas via Stored Procedures con filtros dinámicos
- **Proyecciones** — Simulación de escenarios epidemiológicos con modelos matemáticos basados en literatura científica (DPP Study NEJM 2002, IDF 2024, WHO 2013-2020)
- **Actividad reciente** — Bitácora automática via triggers MySQL, paginación y búsqueda server-side
- **Dashboard Admin** — Stats en tiempo real via Stored Functions
- **Reportes PDF** — Generación client-side con metadata persistida en backend

---

## Créditos

Desarrollado por el equipo 7 **NAGANIOM** como proyecto semestral de la materia **Desarrollo e Implantación de Sistemas de Software** en el **Instituto Tecnológico y de Estudios Superiores de Monterrey**, durante 6° semestre (Febrero-Junio 2026) de la carrera **Ingeniería en Tecnologías Computacionales**.

Datos utilizados bajo licencia abierta:
- [IDF Diabetes Atlas 2024](https://diabetesatlas.org/)
- [PAHO Core Indicators](https://www.paho.org/en/data-and-statistics)
- [INEGI](https://www.inegi.org.mx/)
- [ENSANUT 2022](https://ensanut.insp.mx/)
- [World Bank Health Data](https://data.worldbank.org/topic/health)
- [OECD Health Statistics](https://www.oecd.org/health/health-data.htm)

Modelo matemático de simulación basado en:
- Knowler et al. *Reduction in the Incidence of Type 2 Diabetes with Lifestyle Intervention or Metformin.* NEJM 2002.
- WHO Global Action Plan for the Prevention and Control of NCDs 2013–2020.

**NAGANIOM** — Equipo de desarrollo, Instituto Tecnológico y de Estudios Superiores de Monterrey

| Nombre | Contacto |
|--------|-----|
| Santiago Ramírez Niño (Líder) | a01665906@tec.mx |
| Omar Llano Tostado | a01666730@tec.mx |
| Gabriel Gutiérrez Guerra | a01660505@tec.mx |
| Alejandro Vargas | a01659714@tec.mx |

**Profesores**

- Alejandra Flores Mosri.
- Andrés Fernando Torres Morán.
- Olga Patricia Escamilla Escalante.
- Cesar David Betancourt Adame.
- Diogo Miguel Burnay Rojas.

**Socio Formador**: Data2.

---

<div align="center">
  <sub>HealthInsights © 2025 — NAGANIOM · ITESM</sub>
</div>