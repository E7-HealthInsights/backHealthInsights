package org.acme.infrastructure.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Reporte;
import org.acme.domain.models.ReporteTipo;
import org.acme.domain.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ReporteRepositoryImplTest {

    @Inject ReporteRepository reporteRepository;
    @Inject EntityManager em;

    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    @Transactional
    void setUp() {
        em.createQuery("DELETE FROM ReporteEntity").executeUpdate();
        em.createNativeQuery("MERGE INTO Rol (id, nombre) KEY(id) VALUES (1, 'ADMIN')").executeUpdate();
        em.createNativeQuery(
                "MERGE INTO Usuario (id, nombre, apellido, correo, rol_id, estatus, proveedor_id) KEY(id) VALUES " +
                        "('00000000-0000-0000-0000-000000000001', 'Test', 'Admin', 'test@test.com', 1, true, 'test-firebase-uid')"
        ).executeUpdate();
    }

    private Reporte buildReporte(String titulo, ReporteTipo tipo) {
        Reporte r = new Reporte();
        r.setId(UUID.randomUUID());
        r.setUsuarioId(USER_ID);
        r.setTitulo(titulo);
        r.setTipo(tipo);
        r.setReferenciaId(UUID.randomUUID().toString());
        r.setFechaCreacion(LocalDateTime.now());
        return r;
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    void saveShouldPersistAndReturnReporte() {
        Reporte saved = reporteRepository.save(buildReporte("Reporte Q1", ReporteTipo.DASHBOARD));

        assertNotNull(saved);
        assertEquals("Reporte Q1", saved.getTitulo());
        assertEquals(USER_ID, saved.getUsuarioId());
    }

    // ── findByUsuarioId ───────────────────────────────────────────────────────

    @Test
    void findByUsuarioIdShouldReturnReportesOfUser() {
        reporteRepository.save(buildReporte("Reporte A", ReporteTipo.DASHBOARD));
        reporteRepository.save(buildReporte("Reporte B", ReporteTipo.PROYECCION));

        List<Reporte> result = reporteRepository.findByUsuarioId(USER_ID);

        assertEquals(2, result.size());
    }

    @Test
    void findByUsuarioIdShouldReturnEmptyForOtherUser() {
        reporteRepository.save(buildReporte("Reporte X", ReporteTipo.DASHBOARD));

        List<Reporte> result = reporteRepository.findByUsuarioId(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    // ── findReporteById ───────────────────────────────────────────────────────

    @Test
    void findReporteByIdShouldReturnReporteWhenExists() {
        Reporte saved = reporteRepository.save(buildReporte("Reporte findById", ReporteTipo.ACTIVIDAD));

        Optional<Reporte> found = reporteRepository.findReporteById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Reporte findById", found.get().getTitulo());
    }

    @Test
    void findReporteByIdShouldReturnEmptyForNonExistentId() {
        Optional<Reporte> found = reporteRepository.findReporteById(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    // ── eliminarReporte ───────────────────────────────────────────────────────

    @Test
    void eliminarReporteShouldDeleteIt() {
        Reporte saved = reporteRepository.save(buildReporte("Reporte a eliminar", ReporteTipo.DASHBOARD));

        reporteRepository.eliminarReporte(saved.getId());

        Optional<Reporte> found = reporteRepository.findReporteById(saved.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void eliminarReporteShouldOnlyDeleteTargetReporte() {
        Reporte toKeep   = reporteRepository.save(buildReporte("Queda", ReporteTipo.DASHBOARD));
        Reporte toDelete = reporteRepository.save(buildReporte("Eliminar", ReporteTipo.DASHBOARD));

        reporteRepository.eliminarReporte(toDelete.getId());

        List<Reporte> remaining = reporteRepository.findByUsuarioId(USER_ID);
        assertEquals(1, remaining.size());
        assertEquals(toKeep.getId(), remaining.get(0).getId());
    }
}