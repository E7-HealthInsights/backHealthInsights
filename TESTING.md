# Guía de Tests — backHealthInsights Backend

## Correr los tests

```bash
# Todos los tests
./mvnw test

# Una clase específica
./mvnw test -Dtest=GetMarketingStrategiesUseCaseTest

# Un método específico
./mvnw test -Dtest=GetMarketingStrategiesUseCaseTest#executeShouldReturnEmptyListWhenNoStrategiesExist

# Solo tests unitarios (sin @QuarkusTest, mucho más rápido)
./mvnw test -Dgroups="unit"

# Solo tests de integración
./mvnw test -Dgroups="integration"
```

> Desde IntelliJ: click derecho sobre el paquete `src/test` → **Run Tests**.

---

## Estrategia general

| Capa | Tipo | Herramienta | Qué valida |
|------|------|-------------|------------|
| Use Cases | Unitario | JUnit 5 + Mockito | Lógica de negocio, validaciones, autorización |
| REST Resources | Integración | `@QuarkusTest` + RestAssured | Seguridad por rol, códigos HTTP |
| Repository Impls | Integración | `@QuarkusTest` + H2 | Persistencia real contra BD en memoria |
| Infraestructura pura | Unitario | JUnit 5 (sin mocks) | Lógica de string/data sin dependencias externas |

---

## Tests unitarios — Use Cases

**29 clases** en `src/test/java/org/acme/application/usecase/`

### Patrón de construcción

Los use cases usan inyección de campo (`@Inject` sin constructor). Se instancian directamente y se inyectan los mocks vía reflection en el `@BeforeEach`:

```java
@BeforeEach
void setUp() throws Exception {
    repository  = mock(MarketingStrategyRepository.class);
    authContext = mock(AuthContext.class);

    Role role = new Role((byte) 4, "DIRECTOR_MERCADOTECNIA");
    User user = new User(USER_ID, "Ana", "López", "ana@test.com", role, true, "firebase-uid");
    when(authContext.getUser()).thenReturn(user);

    useCase = new GetMarketingStrategiesUseCase();
    setField(useCase, "repository",  repository);
    setField(useCase, "authContext", authContext);
}

private static void setField(Object target, String fieldName, Object value) throws Exception {
    var field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
}
```

### Casos que siempre se cubren

Para cualquier use case nuevo, estos escenarios son obligatorios:

**Validación de input:**
- DTO nulo o con campos obligatorios en blanco → `BadRequestException`

**Autorización por ownership:**
- Recurso no encontrado → `NotFoundException`
- Recurso de otro usuario → `ForbiddenException`
- No llamar al repositorio de escritura cuando falla la autorización

**Caso feliz:**
- El método correcto del repositorio se llama con los parámetros esperados
- El resultado se mapea y devuelve correctamente

**Ejemplo completo — use case con ownership check:**

```java
@Test
void executeShouldThrowNotFoundWhenStrategyDoesNotExist() {
    UUID unknown = UUID.randomUUID();
    when(repository.findOneById(unknown)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> useCase.execute(unknown));
}

@Test
void executeShouldThrowForbiddenWhenStrategyBelongsToAnotherUser() {
    strategy.setUsuarioId(OTHER_ID); // otro usuario

    assertThrows(ForbiddenException.class, () -> useCase.execute(STRATEGY_ID));
}

@Test
void executeShouldNotPersistWhenForbidden() {
    strategy.setUsuarioId(OTHER_ID);

    assertThrows(ForbiddenException.class, () -> useCase.execute(STRATEGY_ID));
    verify(repository, never()).update(any()); // no escribe
}
```

### Inventario actual

| Archivo | Use Case |
|---------|----------|
| `ActualizarProyeccionUseCaseTest` | Actualizar proyección existente |
| `AddMarketingStrategyCommentUseCaseTest` | Agregar comentario a estrategia |
| `CreateReporteUseCaseTest` | Crear reporte |
| `CreateUserUseCaseTest` | Crear usuario |
| `CreateWidgetUseCaseTest` | Crear widget personal |
| `DeactivateDatasetUseCaseTest` | Desactivar dataset |
| `DeactivateUserUseCaseTest` | Desactivar usuario |
| `DeleteReporteUseCaseTest` | Eliminar reporte |
| `DeleteWidgetUseCaseTest` | Eliminar widget (403 vs 404 por origen) |
| `EliminarProyeccionUseCaseTest` | Eliminar proyección |
| `GenerateMarketingStrategyUseCaseTest` | Generar estrategia con OpenAI |
| `GetDatasetsUseCaseTest` | Listar datasets |
| `GetLogActividadUseCaseTest` | Listar logs paginados |
| `GetMarketingStrategiesUseCaseTest` | Listar estrategias del usuario |
| `GetMarketingStrategyByIdUseCaseTest` | Obtener estrategia por ID |
| `GetMetricasByDatasetUseCaseTest` | Obtener métricas de un dataset |
| `GetProyeccionesUseCaseTest` | Listar proyecciones del usuario |
| `GetReportesUseCaseTest` | Listar reportes del usuario |
| `GetUserWidgetsUseCaseTest` | Obtener widgets del usuario |
| `GetUsersUseCaseTest` | Listar usuarios (admin) |
| `GetValoresDistintosUseCaseTest` | Obtener valores únicos de columna |
| `GuardarProyeccionUseCaseTest` | Guardar proyección nueva |
| `ReactivateDatasetUseCaseTest` | Reactivar dataset |
| `SimularProyeccionFinanzasUseCaseTest` | Simular proyección financiera |
| `SimularProyeccionGeneralUseCaseTest` | Simular proyección general |
| `UpdateMarketingStrategyStateUseCaseTest` | Cambiar estado de estrategia |
| `UpdateUserUseCaseTest` | Actualizar datos de usuario |
| `UpdateWidgetOrdenUseCaseTest` | Reordenar widgets en batch |
| `UploadDatasetUseCaseTest` | Subir dataset CSV |

---

## Tests de integración — REST Resources

**9 clases** en `src/test/java/org/acme/interfaces/rest/`

### Patrón de construcción

Todos los tests de recursos usan `@QuarkusTest`. Quarkus levanta la aplicación completa con H2 en memoria. El `TestFirebaseAuthFilter` inyecta automáticamente un usuario con rol `ADMIN` en todas las peticiones que incluyen `Authorization: Bearer <token>`.

```java
@QuarkusTest
class MarketingStrategyResourceTest {

    @InjectMock
    OpenAIClient openAIClient; // mockear dependencias externas con @InjectMock

    @Test
    void postStrategyShouldReturn403WhenUserIsAdmin() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("{}")
                .when().post("/marketing/strategies")
                .then()
                .statusCode(403);
    }
}
```

### Casos que siempre se cubren

Para cada endpoint de un recurso nuevo:

- **403** con token de un rol sin acceso (el `TestFirebaseAuthFilter` inyecta ADMIN)
- **403** sin token (sin header `Authorization`)
- Para endpoints públicos (sin `@RolesAllowed`): **200** con y sin token, y validar campos del body

### Dependencias externas

Mockear siempre con `@InjectMock` cualquier cliente externo que el resource use, para evitar llamadas reales durante los tests:

```java
@InjectMock
OpenAIClient openAIClient;
```

### Inventario actual

| Archivo | Endpoints cubiertos |
|---------|---------------------|
| `AuthResourceTest` | `/auth/*` |
| `DatasetResourceTest` | `/datasets/*` |
| `LogActividadResourceTest` | `/logs/*` |
| `MarketingStrategyResourceTest` | `/marketing/strategies/*` (5 endpoints) |
| `ProyeccionResourceTest` | `/proyecciones/*` |
| `ReporteResourceTest` | `/reportes/*` |
| `StatusResourceTest` | `GET /status` (endpoint público) |
| `UserResourceTest` | `/users/*` |
| `WidgetResourceTest` | `/widgets/*` |

---

## Tests de integración — Repository Impls

**9 clases** en `src/test/java/org/acme/infrastructure/repository/`

### Patrón de construcción

```java
@QuarkusTest
class DatasetRepositoryImplTest {

    @Inject DatasetRepository datasetRepository;
    @Inject EntityManager em;

    @BeforeEach
    @Transactional
    void setUp() {
        // 1. Limpiar la tabla para aislar el test
        em.createNativeQuery("DELETE FROM Dataset").executeUpdate();

        // 2. Garantizar datos de lookup (Role, Users) con MERGE INTO
        //    —no depender del import.sql que puede fallar al arrancar—
        em.createNativeQuery("MERGE INTO Role (id, name) KEY(id) VALUES (1, 'ADMIN')").executeUpdate();
        em.createNativeQuery(
            "MERGE INTO Users (id, name, last_name, email, role_id, status, provider_id) KEY(id) VALUES " +
            "('00000000-0000-0000-0000-000000000001', 'Test', 'Admin', 'test@test.com', 1, true, 'test-firebase-uid')"
        ).executeUpdate();
    }

    // ⚠️ CRÍTICO: @Transactional en cada @Test que escribe en BD
    // El @Transactional del @BeforeEach NO se propaga a los @Test en Quarkus
    @Test
    @Transactional
    void saveShouldPersistDatasetAndReturnIt() {
        Dataset saved = datasetRepository.save(buildDataset("Diabetes 2023", "imss_diabetes_2023"));
        assertNotNull(saved);
    }

    // Los tests de solo lectura NO necesitan @Transactional
    @Test
    void findDatasetByIdShouldReturnEmptyForNonExistentId() {
        Optional<Dataset> found = datasetRepository.findDatasetById(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }
}
```

### Reglas importantes

- **`@Transactional` en el `@Test`** cuando el método del repositorio llama a `persist()` o `merge()` internamente. Sin esto Panache lanza `TransactionRequiredException`.
- **Los tests de lectura no necesitan `@Transactional`** — solo los de escritura.
- **Paginación base-1**: `findPaginated` y `countAll` de `LogActividadRepository` y `UserRepository` usan `page=1` como primera página. Pasar `page=0` produce offset negativo y Hibernate lanza excepción.
- **`status` obligatorio en `UserRepository`**: `findPaginated` y `countUsers` tienen `WHERE u.status = :status` fijo. Pasar `null` devuelve 0 resultados. Usar `true` o `false` explícitamente.
- **`LogActividadRepository` no expone `save()`**: el repositorio solo expone métodos de lectura y `updateDetalle`. Para insertar datos en tests, usar `em.persist(LogActividadEntity)` directamente en un método `@Transactional` auxiliar.

### Inventario actual

| Archivo | Métodos cubiertos |
|---------|-------------------|
| `DatasetRepositoryImplTest` | `save`, `findAllDatasets`, `findDatasetById`, `existsByNombreTabla`, `findByNombreTabla`, `update`, `deactivate`, `reactivate` |
| `LogActividadRepositoryImplTest` | `findAllLogs`, `findLatestByEntidadId`, `updateDetalle`, `findPaginated`, `countAll` |
| `MarketingStrategyRepositoryImplTest` | `create`, `findByUsuarioId`, `findOneById`, `update` |
| `MetricaRepositoryImplTest` | `saveAll`, `findByDatasetId`, `findByColumnaCsvAndDatasetId` |
| `ProyeccionRepositoryImplTest` | `save`, `findByUsuarioId`, `findById`, `update`, `delete` |
| `ReporteRepositoryImplTest` | `save`, `findByUsuarioId`, `findReporteById`, `eliminarReporte` |
| `RoleRepositoryImplTest` | `findRoleById` |
| `UserRepositoryImplTest` | `create`, `findByFirebaseUuid`, `existsByEmail`, `findAllUsers`, `findUserById`, `update`, `findPaginated`, `countUsers` |
| `WidgetRepositoryImplTest` | `create`, `findByUserId`, `findDefaultsByRolId`, `updateOrden`, `removeById` |

---

## Tests unitarios — Infraestructura pura

**6 clases** en `src/test/java/org/acme/infrastructure/` (fuera de `repository/`)

Sin `@QuarkusTest` — instanciación directa, sin DI ni BD.

| Archivo | Qué valida |
|---------|------------|
| `CsvIngestServiceTest` | `sanitizeColumnName` vía reflection: lowercase, trim, espacios→`_`, eliminación de caracteres especiales y acentos, prefijo `_` para nombres que empiezan con dígito, fallback `col_sin_nombre` |
| `DashboardSummarizerTest` | Compresión de widgets por tipo: STAT, LINE/BAR/PIE (con `delta_pct` y `tendencia`), MULTISERIES/MULTIBAR, TABLE (ordenamiento, límites de filas), tipo desconocido, filtrado de widgets con error |
| `DatasetIngestionConsumerTest` | Consumidor de mensajes Kafka para ingesta de CSV |
| `DistinctValuesExecutorTest` | Validación de `SAFE_IDENTIFIER` sin BD: nombres válidos vs. SQL injection, backticks, slashes |
| `FirebaseAuthFilterTest` | Filtro de autenticación Firebase |
| `MappersTest` | Round-trip de los 4 mappers: `MarketingStrategyMapper` (default `ESTADO_PROPUESTA`), `DatasetMapper`, `UserMapper`, `WidgetMapper`, `ProyeccionMapper` |

### Truco para testear métodos privados

```java
// Acceso a sanitizeColumnName (método private en CsvIngestService)
private Method sanitizeMethod;

@BeforeEach
void setUp() throws Exception {
    service = new CsvIngestService();
    sanitizeMethod = CsvIngestService.class.getDeclaredMethod("sanitizeColumnName", String.class);
    sanitizeMethod.setAccessible(true);
}

private String sanitize(String input) throws Exception {
    return (String) sanitizeMethod.invoke(service, input);
}
```

### Truco para testear validaciones sin BD (`DistinctValuesExecutor`)

El `fetchDistinct` valida los identificadores antes de abrir la conexión. Con `DataSource=null`:
- Si la validación **pasa** → `NullPointerException` (intenta abrir la conexión)
- Si la validación **falla** → `IllegalArgumentException` (lanzado antes de tocar el `DataSource`)

```java
@BeforeEach
void setUp() throws Exception {
    executor = new DistinctValuesExecutor();
    var field = DistinctValuesExecutor.class.getDeclaredField("dataSource");
    field.setAccessible(true);
    field.set(executor, null); // sin BD real
}

@Test
void shouldAcceptValidTableName() {
    // NPE significa que pasó la validación → correcto
    assertThrows(NullPointerException.class,
            () -> executor.fetchDistinct("tabla_valida", "columna", 10));
}

@Test
void shouldRejectSqlInjection() {
    assertThrows(IllegalArgumentException.class,
            () -> executor.fetchDistinct("tabla'; DROP TABLE users;--", "col", 10));
}
```

---

## Decisiones de diseño a mantener

**`@Transactional` solo donde se necesita:** No anotar todos los `@Test` con `@Transactional` por defecto. Solo los que llaman a métodos que internamente hacen `persist()` o `merge()`. Los tests de lectura corren sin transacción activa.

**`MERGE INTO` en lugar de `INSERT`:** Usar siempre `MERGE INTO ... KEY(id) VALUES (...)` para datos de lookup (Role, Users, Tipo_de_Grafica) en el `@BeforeEach`. Evita `DuplicateKeyException` si el dato ya existe de un test anterior o del `import.sql`.

**No depender del `import.sql` para datos de test:** El `import.sql` de test falla silenciosamente al arrancar porque Hibernate lo ejecuta antes de que las tablas de referencia existan (el `WARN` de FK en los logs). El `@BeforeEach` garantiza los datos necesarios dentro de su propia transacción.

**Un `setUp()` limpio por clase:** El `@BeforeEach` siempre limpia la tabla principal con `DELETE FROM` antes de insertar. Esto garantiza el aislamiento entre tests sin depender del orden de ejecución.

**Mocks del `TestFirebaseAuthFilter`:** En los tests de `@QuarkusTest`, el filtro inyecta siempre un usuario ADMIN. No se puede cambiar el rol del usuario inyectado sin modificar el filtro. Los tests de recursos verifican el rechazo por rol incorrecto, no el acceso correcto (eso lo cubren los tests unitarios de los use cases).