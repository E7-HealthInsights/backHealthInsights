package org.acme.infrastructure.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.EntidadTipo;
import org.acme.domain.models.LogActividad;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.infrastructure.entities.LogActividadEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LogActividadRepositoryImplTest {

    // LogActividadRepository no expone save() en su interfaz de dominio:
    // los logs se crean desde los use cases vía EntityManager directamente.
    // En los tests insertamos usando em.persist(LogActividadEntity) dentro de
    // métodos @Transactional para preparar el estado sin acoplar el test a
    // métodos inexistentes en el repositorio.

    @Inject LogActividadRepository logRepository;
    @Inject EntityManager em;

    @BeforeEach
    @Transactional
    void setUp() {
        em.createNativeQuery("DELETE FROM LogActividad").executeUpdate();
        em.createNativeQuery("MERGE INTO Role (id, name) KEY(id) VALUES (1, 'ADMIN')").executeUpdate();
        em.createNativeQuery(
                "MERGE INTO Users (id, name, last_name, email, role_id, status, provider_id) KEY(id) VALUES " +
                        "('00000000-0000-0000-0000-000000000001', 'Test', 'Admin', 'test@test.com', 1, true, 'test-firebase-uid')"
        ).executeUpdate();
    }

    @Transactional
    void persistLog(String accion, String detalle, EntidadTipo entidad, String entidadId) {
        LogActividadEntity entity = new LogActividadEntity();
        entity.setId(UUID.randomUUID());
        entity.setAccion(accion);
        entity.setDetalle(detalle);
        entity.setEntidadTipo(entidad);
        entity.setEntidadId(entidadId);
        entity.setFecha(LocalDateTime.now());
        em.persist(entity);
        em.flush();
    }

    // ── findAllLogs ───────────────────────────────────────────────────────────

    @Test
    void findAllLogsShouldReturnAllPersistedLogs() {
        persistLog("CREAR_USUARIO", "Detalle A", EntidadTipo.USUARIO, UUID.randomUUID().toString());
        persistLog("CREAR_DATASET", "Detalle B", EntidadTipo.DATASET, UUID.randomUUID().toString());

        List<LogActividad> logs = logRepository.findAllLogs();

        assertEquals(2, logs.size());
    }

    @Test
    void findAllLogsShouldReturnEmptyWhenNoLogs() {
        List<LogActividad> logs = logRepository.findAllLogs();
        assertTrue(logs.isEmpty());
    }

    // ── findLatestByEntidadId ─────────────────────────────────────────────────

    @Test
    void findLatestByEntidadIdShouldReturnMostRecentLog() throws InterruptedException {
        String entidadId = UUID.randomUUID().toString();
        persistLog("PRIMER_ACCION", "Primero", EntidadTipo.USUARIO, entidadId);
        Thread.sleep(10);
        persistLog("ULTIMA_ACCION", "Último",  EntidadTipo.USUARIO, entidadId);

        Optional<LogActividad> latest = logRepository.findLatestByEntidadId(entidadId);

        assertTrue(latest.isPresent());
        assertEquals("ULTIMA_ACCION", latest.get().getAccion());
    }

    @Test
    void findLatestByEntidadIdShouldReturnEmptyForNonExistentEntidad() {
        Optional<LogActividad> latest = logRepository.findLatestByEntidadId("id-inexistente");
        assertTrue(latest.isEmpty());
    }

    // ── updateDetalle ─────────────────────────────────────────────────────────

    @Test
    @Transactional
    void updateDetalleShouldModifyDetalleField() {
        UUID logId = UUID.randomUUID();
        LogActividadEntity entity = new LogActividadEntity();
        entity.setId(logId);
        entity.setAccion("ACTUALIZAR");
        entity.setDetalle("Detalle original");
        entity.setEntidadTipo(EntidadTipo.USUARIO);
        entity.setEntidadId(UUID.randomUUID().toString());
        entity.setFecha(LocalDateTime.now());
        em.persist(entity);
        em.flush();

        logRepository.updateDetalle(logId, "Nuevo detalle actualizado");

        List<LogActividad> all = logRepository.findAllLogs();
        LogActividad found = all.stream()
                .filter(l -> l.getId().equals(logId))
                .findFirst().orElseThrow();

        assertEquals("Nuevo detalle actualizado", found.getDetalle());
    }

    // ── findPaginated (base-1: page=1 es la primera página) ──────────────────

    @Test
    void findPaginatedShouldReturnFirstPage() {
        for (int i = 0; i < 5; i++) {
            persistLog("ACCION_" + i, "Detalle " + i, EntidadTipo.USUARIO, UUID.randomUUID().toString());
        }

        List<LogActividad> page = logRepository.findPaginated(1, 3, null);

        assertEquals(3, page.size());
    }

    @Test
    void findPaginatedShouldFilterBySearchTerm() {
        persistLog("CREAR_DATASET", "Dataset ingresado",  EntidadTipo.DATASET, UUID.randomUUID().toString());
        persistLog("CREAR_USUARIO", "Usuario registrado", EntidadTipo.USUARIO, UUID.randomUUID().toString());

        List<LogActividad> result = logRepository.findPaginated(1, 10, "DATASET");

        assertEquals(1, result.size());
        assertEquals("CREAR_DATASET", result.get(0).getAccion());
    }

    @Test
    void findPaginatedShouldReturnEmptyForNonMatchingSearch() {
        persistLog("CREAR_USUARIO", "Detalle", EntidadTipo.USUARIO, UUID.randomUUID().toString());

        List<LogActividad> result = logRepository.findPaginated(1, 10, "termino-no-existente");

        assertTrue(result.isEmpty());
    }

    // ── countAll ──────────────────────────────────────────────────────────────

    @Test
    void countAllShouldReturnTotalLogs() {
        persistLog("A1", "Detalle 1", EntidadTipo.USUARIO, UUID.randomUUID().toString());
        persistLog("A2", "Detalle 2", EntidadTipo.DATASET, UUID.randomUUID().toString());

        long count = logRepository.countAll(null);

        assertEquals(2, count);
    }

    @Test
    void countAllShouldFilterBySearchTerm() {
        persistLog("CREAR_DATASET", "Detalle dataset", EntidadTipo.DATASET, UUID.randomUUID().toString());
        persistLog("CREAR_USUARIO", "Detalle usuario", EntidadTipo.USUARIO, UUID.randomUUID().toString());

        long count = logRepository.countAll("USUARIO");

        assertEquals(1, count);
    }
}