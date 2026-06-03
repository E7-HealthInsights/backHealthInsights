package org.acme.infrastructure.mapper;

import org.acme.domain.models.*;
import org.acme.infrastructure.entities.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para todos los mappers de infraestructura.
 *
 * Los mappers son clases estáticas puras — no necesitan mocks ni Quarkus.
 * Se verifican:
 * - Round-trip completo: dominio → entidad → dominio (ningún campo se pierde)
 * - Lógica no trivial: defaults aplicados cuando un campo es nulo
 * - Relaciones lazy: se mapean correctamente cuando la entidad relacionada está presente
 */
class MappersTest {

    // ═══════════════════════════════════════════════════════════════════════
    // MarketingStrategyMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void marketingStrategyToDomainShouldMapAllFields() {
        MarketingStrategyEntity entity = new MarketingStrategyEntity();
        UUID id        = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();

        entity.setId(id);
        entity.setUsuarioId(usuarioId);
        entity.setCreadoEn(ahora);
        entity.setContextoExtra("Enfoque rural");
        entity.setPayloadJson("{\"resumen\":\"Test\"}");
        entity.setEstado(MarketingStrategy.ESTADO_EJECUTADA);
        entity.setNotaResultado("Resultados positivos");
        entity.setFechaRevision(ahora.plusDays(7));
        entity.setComentariosJson("[{\"contenido\":\"Bien\"}]");

        MarketingStrategy domain = MarketingStrategyMapper.toDomain(entity);

        assertEquals(id,                             domain.getId());
        assertEquals(usuarioId,                      domain.getUsuarioId());
        assertEquals(ahora,                          domain.getCreadoEn());
        assertEquals("Enfoque rural",                domain.getContextoExtra());
        assertEquals("{\"resumen\":\"Test\"}",       domain.getPayloadJson());
        assertEquals(MarketingStrategy.ESTADO_EJECUTADA, domain.getEstado());
        assertEquals("Resultados positivos",         domain.getNotaResultado());
        assertEquals(ahora.plusDays(7),              domain.getFechaRevision());
        assertEquals("[{\"contenido\":\"Bien\"}]",   domain.getComentariosJson());
    }

    @Test
    void marketingStrategyToDomainShouldDefaultEstadoPropuestaWhenNull() {
        MarketingStrategyEntity entity = new MarketingStrategyEntity();
        entity.setId(UUID.randomUUID());
        entity.setUsuarioId(UUID.randomUUID());
        entity.setCreadoEn(LocalDateTime.now());
        entity.setPayloadJson("{}");
        entity.setEstado(null); // sin estado explícito

        MarketingStrategy domain = MarketingStrategyMapper.toDomain(entity);

        assertEquals(MarketingStrategy.ESTADO_PROPUESTA, domain.getEstado());
    }

    @Test
    void marketingStrategyToEntityShouldMapAllFields() {
        UUID id        = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();

        MarketingStrategy domain = new MarketingStrategy(id, usuarioId, ahora, "Contexto", "{\"data\":1}");
        domain.setEstado(MarketingStrategy.ESTADO_DESCARTADA);
        domain.setNotaResultado("No funcionó");
        domain.setFechaRevision(ahora.plusDays(3));
        domain.setComentariosJson("[{\"id\":\"abc\"}]");

        MarketingStrategyEntity entity = MarketingStrategyMapper.toEntity(domain);

        assertEquals(id,                                  entity.getId());
        assertEquals(usuarioId,                           entity.getUsuarioId());
        assertEquals(ahora,                               entity.getCreadoEn());
        assertEquals("Contexto",                          entity.getContextoExtra());
        assertEquals("{\"data\":1}",                      entity.getPayloadJson());
        assertEquals(MarketingStrategy.ESTADO_DESCARTADA, entity.getEstado());
        assertEquals("No funcionó",                       entity.getNotaResultado());
        assertEquals(ahora.plusDays(3),                   entity.getFechaRevision());
        assertEquals("[{\"id\":\"abc\"}]",                entity.getComentariosJson());
    }

    @Test
    void marketingStrategyToEntityShouldDefaultEstadoPropuestaWhenNull() {
        MarketingStrategy domain = new MarketingStrategy(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), null, "{}");
        domain.setEstado(null);

        MarketingStrategyEntity entity = MarketingStrategyMapper.toEntity(domain);

        assertEquals(MarketingStrategy.ESTADO_PROPUESTA, entity.getEstado());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DatasetMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void datasetToDomainShouldMapAllFields() {
        DatasetEntity entity = new DatasetEntity();
        UUID id = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();

        entity.setId(id);
        entity.setNombre("Diabetes IMSS 2023");
        entity.setNombreTabla("imss_diabetes_2023");
        entity.setDescripcion("Dataset de detección");
        entity.setFuente("IMSS");
        entity.setArchivoCsv("gs://bucket/file.csv");
        entity.setLink("https://datos.gob.mx");
        entity.setEstado(DatasetEstado.READY);
        entity.setErrorMensaje(null);
        entity.setFechaActualizacion(ahora);
        entity.setModifiedBy("admin@test.com");

        Dataset domain = DatasetMapper.toDomain(entity);

        assertEquals(id,                       domain.getId());
        assertEquals("Diabetes IMSS 2023",     domain.getNombre());
        assertEquals("imss_diabetes_2023",     domain.getNombreTabla());
        assertEquals("Dataset de detección",   domain.getDescripcion());
        assertEquals("IMSS",                   domain.getFuente());
        assertEquals("gs://bucket/file.csv",   domain.getArchivoCsv());
        assertEquals("https://datos.gob.mx",   domain.getLink());
        assertEquals(DatasetEstado.READY,      domain.getEstado());
        assertNull(domain.getErrorMensaje());
        assertEquals(ahora,                    domain.getFechaActualizacion());
        assertEquals("admin@test.com",         domain.getModifiedBy());
    }

    @Test
    void datasetToEntityShouldMapAllFields() {
        Dataset domain = new Dataset();
        UUID id = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();

        domain.setId(id);
        domain.setNombre("Obesidad SSA 2022");
        domain.setNombreTabla("ssa_obesidad_2022");
        domain.setDescripcion("Datos de obesidad");
        domain.setFuente("SSA");
        domain.setArchivoCsv("gs://bucket/obesidad.csv");
        domain.setLink(null);
        domain.setEstado(DatasetEstado.PENDING);
        domain.setErrorMensaje("Error de parsing");
        domain.setFechaActualizacion(ahora);
        domain.setModifiedBy("editor@test.com");

        DatasetEntity entity = DatasetMapper.toEntity(domain);

        assertEquals(id,                    entity.getId());
        assertEquals("Obesidad SSA 2022",   entity.getNombre());
        assertEquals("ssa_obesidad_2022",   entity.getNombreTabla());
        assertEquals("Datos de obesidad",   entity.getDescripcion());
        assertEquals("SSA",                 entity.getFuente());
        assertNull(entity.getLink());
        assertEquals(DatasetEstado.PENDING, entity.getEstado());
        assertEquals("Error de parsing",    entity.getErrorMensaje());
        assertEquals(ahora,                 entity.getFechaActualizacion());
        assertEquals("editor@test.com",     entity.getModifiedBy());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UserMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void userToDomainShouldMapAllScalarFields() {
        UserEntity entity = new UserEntity();
        UUID id = UUID.randomUUID();

        entity.setId(id);
        entity.setName("Carlos");
        entity.setLastName("Ruiz");
        entity.setEmail("carlos@test.com");
        entity.setStatus(true);
        entity.setProviderId("firebase-uid-123");
        entity.setModifiedBy("admin-uuid");
        entity.setRole(null); // relación no inicializada

        User domain = UserMapper.toDomain(entity);

        assertEquals(id,                  domain.getId());
        assertEquals("Carlos",            domain.getName());
        assertEquals("Ruiz",              domain.getLastName());
        assertEquals("carlos@test.com",   domain.getEmail());
        assertTrue(domain.isStatus());
        assertEquals("firebase-uid-123",  domain.getProviderId());
        assertEquals("admin-uuid",        domain.getModifiedBy());
        assertNull(domain.getRole()); // lazy no inicializado → null en dominio
    }

    @Test
    void userToEntityShouldMapAllScalarFields() {
        Role role = new Role((byte) 1, "ADMIN");
        User domain = new User(UUID.randomUUID(), "Laura", "Gómez",
                "laura@test.com", role, false, "firebase-laura");
        domain.setModifiedBy("editor-uuid");

        UserEntity entity = UserMapper.toEntity(domain);

        assertEquals(domain.getId(),       entity.getId());
        assertEquals("Laura",              entity.getName());
        assertEquals("Gómez",              entity.getLastName());
        assertEquals("laura@test.com",     entity.getEmail());
        assertFalse(entity.isStatus());
        assertEquals("firebase-laura",     entity.getProviderId());
        assertEquals("editor-uuid",        entity.getModifiedBy());
        assertNotNull(entity.getRole());
        assertEquals("ADMIN",              entity.getRole().getName());
    }

    @Test
    void userToEntityShouldHandleNullRole() {
        User domain = new User(UUID.randomUUID(), "Test", "User",
                "test@test.com", null, true, "uid");

        UserEntity entity = UserMapper.toEntity(domain);

        assertNull(entity.getRole());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ProyeccionMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void proyeccionToEntityShouldMapAllFields() {
        Proyeccion domain = new Proyeccion();
        UUID id = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();

        domain.setId(id);
        domain.setTitulo("Escenario 2040");
        domain.setDescripcion("Inversión alta en prevención");
        domain.setParametros("{\"params\":{},\"puntos\":[]}");
        domain.setFechaCreacion(ahora);
        domain.setFechaActualizacion(ahora.plusDays(1));

        ProyeccionEntity entity = ProyeccionMapper.toEntity(domain);

        assertEquals(id,                              entity.getId());
        assertEquals("Escenario 2040",                entity.getTitulo());
        assertEquals("Inversión alta en prevención",  entity.getDescripcion());
        assertEquals("{\"params\":{},\"puntos\":[]}",entity.getQuery());
        assertEquals(ahora,                           entity.getFechaCreacion());
        assertEquals(ahora.plusDays(1),               entity.getFechaActualizacion());
    }

    @Test
    void proyeccionToDomainShouldMapScalarFieldsWhenUsuarioIsNull() {
        ProyeccionEntity entity = new ProyeccionEntity();
        UUID id = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();

        entity.setId(id);
        entity.setTitulo("Escenario base");
        entity.setDescripcion("Sin cambios");
        entity.setQuery("{\"kpis\":{}}");
        entity.setFechaCreacion(ahora);
        entity.setFechaActualizacion(null);
        entity.setUsuario(null);

        Proyeccion domain = ProyeccionMapper.toDomain(entity);

        assertEquals(id,               domain.getId());
        assertEquals("Escenario base", domain.getTitulo());
        assertEquals("Sin cambios",    domain.getDescripcion());
        assertEquals("{\"kpis\":{}}",  domain.getParametros());
        assertEquals(ahora,            domain.getFechaCreacion());
        assertNull(domain.getFechaActualizacion());
        assertNull(domain.getUsuario());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WidgetMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void widgetToEntityShouldMapAllFields() {
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        Role role = new Role((byte) 3, "DIRECTOR_FINANZAS");
        User user = new User(UUID.randomUUID(), "Test", "User",
                "test@test.com", role, true, "uid");

        Widget domain = new Widget(UUID.randomUUID(), "Widget financiero", user, tipo,
                "{\"tabla\":\"f4_pib\"}", 2, (byte) 3);
        domain.setTipoSemantico("moneda");
        domain.setNivelGeografico("pais");

        WidgetEntity entity = WidgetMapper.toEntity(domain);

        assertEquals(domain.getId(),         entity.getId());
        assertEquals("Widget financiero",    entity.getTitulo());
        assertEquals("{\"tabla\":\"f4_pib\"}", entity.getQuery());
        assertEquals(2,                      entity.getOrden());
        assertEquals((byte) 3,               entity.getRolId());
        assertEquals("moneda",               entity.getTipoSemantico());
        assertEquals("pais",                 entity.getNivelGeografico());
        assertNotNull(entity.getTipo());
        assertNotNull(entity.getUsuario());
    }

    @Test
    void widgetToEntityShouldHandleNullTipoAndUsuario() {
        Widget domain = new Widget(UUID.randomUUID(), "Widget sin tipo", null, null,
                "{}", 1, null);

        WidgetEntity entity = WidgetMapper.toEntity(domain);

        assertNull(entity.getTipo());
        assertNull(entity.getUsuario());
    }

    @Test
    void widgetToDomainShouldMapScalarFieldsWhenRelationsAreNull() {
        WidgetEntity entity = new WidgetEntity();
        UUID id = UUID.randomUUID();

        entity.setId(id);
        entity.setTitulo("Widget de rol");
        entity.setQuery("{\"tabla\":\"test\"}");
        entity.setOrden(5);
        entity.setRolId((byte) 2);
        entity.setTipoSemantico("porcentaje");
        entity.setNivelGeografico("estado");
        entity.setTipo(null);
        entity.setUsuario(null);

        Widget domain = WidgetMapper.toDomain(entity);

        assertEquals(id,                   domain.getId());
        assertEquals("Widget de rol",      domain.getTitulo());
        assertEquals("{\"tabla\":\"test\"}", domain.getQuery());
        assertEquals(5,                    domain.getOrden());
        assertEquals((byte) 2,             domain.getRolId());
        assertEquals("porcentaje",         domain.getTipoSemantico());
        assertEquals("estado",             domain.getNivelGeografico());
        assertNull(domain.getTipo());
        assertNull(domain.getUsuario());
    }
}
