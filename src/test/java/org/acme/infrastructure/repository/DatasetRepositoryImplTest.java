package org.acme.infrastructure.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.repository.DatasetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DatasetRepositoryImplTest {

    @Inject DatasetRepository datasetRepository;
    @Inject EntityManager em;

    @BeforeEach
    @Transactional
    void setUp() {
        em.createNativeQuery("DELETE FROM Dataset").executeUpdate();
    }

    private Dataset buildDataset(String nombre, String nombreTabla) {
        Dataset d = new Dataset();
        d.setId(UUID.randomUUID());
        d.setNombre(nombre);
        d.setNombreTabla(nombreTabla);
        d.setDescripcion("Descripción de " + nombre);
        d.setFuente("IMSS");
        d.setEstado(DatasetEstado.PENDING);
        d.setFechaActualizacion(LocalDateTime.now());
        return d;
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void saveShouldPersistDatasetAndReturnIt() {
        Dataset saved = datasetRepository.save(buildDataset("Diabetes 2023", "imss_diabetes_2023"));

        assertNotNull(saved);
        assertEquals("Diabetes 2023", saved.getNombre());
        assertNotNull(saved.getId());
    }

    // ── findAllDatasets ───────────────────────────────────────────────────────

    @Test
    @Transactional
    void findAllDatasetsShouldReturnAllPersisted() {
        datasetRepository.save(buildDataset("Dataset A", "tabla_a"));
        datasetRepository.save(buildDataset("Dataset B", "tabla_b"));

        List<Dataset> all = datasetRepository.findAllDatasets();

        assertEquals(2, all.size());
    }

    @Test
    void findAllDatasetsShouldReturnEmptyWhenNoneExist() {
        List<Dataset> all = datasetRepository.findAllDatasets();
        assertTrue(all.isEmpty());
    }

    // ── findDatasetById ───────────────────────────────────────────────────────

    @Test
    @Transactional
    void findDatasetByIdShouldReturnDatasetWhenExists() {
        Dataset saved = datasetRepository.save(buildDataset("Obesidad 2022", "ssa_obesidad_2022"));

        Optional<Dataset> found = datasetRepository.findDatasetById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Obesidad 2022", found.get().getNombre());
    }

    @Test
    void findDatasetByIdShouldReturnEmptyForNonExistentId() {
        Optional<Dataset> found = datasetRepository.findDatasetById(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    // ── existsByNombreTabla ───────────────────────────────────────────────────

    @Test
    @Transactional
    void existsByNombreTablaShouldReturnTrueWhenExists() {
        datasetRepository.save(buildDataset("Dataset X", "tabla_unica_x"));

        assertTrue(datasetRepository.existsByNombreTabla("tabla_unica_x"));
    }

    @Test
    void existsByNombreTablaShouldReturnFalseWhenNotFound() {
        assertFalse(datasetRepository.existsByNombreTabla("tabla_inexistente"));
    }

    // ── findByNombreTabla ─────────────────────────────────────────────────────

    @Test
    @Transactional
    void findByNombreTablaShouldReturnDatasetWhenExists() {
        datasetRepository.save(buildDataset("Dataset Y", "tabla_y_unica"));

        Optional<Dataset> found = datasetRepository.findByNombreTabla("tabla_y_unica");

        assertTrue(found.isPresent());
        assertEquals("Dataset Y", found.get().getNombre());
    }

    @Test
    void findByNombreTablaShouldReturnEmptyWhenNotFound() {
        Optional<Dataset> found = datasetRepository.findByNombreTabla("no_existe");
        assertTrue(found.isEmpty());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void updateShouldChangeEstadoAndErrorMensaje() {
        Dataset saved = datasetRepository.save(buildDataset("Dataset Update", "tabla_update"));

        saved.setEstado(DatasetEstado.READY);
        saved.setErrorMensaje(null);
        saved.setFechaActualizacion(LocalDateTime.now());
        Dataset updated = datasetRepository.update(saved);

        assertEquals(DatasetEstado.READY, updated.getEstado());
        assertNull(updated.getErrorMensaje());
    }

    @Test
    @Transactional
    void updateShouldSetErrorMensajeWhenFails() {
        Dataset saved = datasetRepository.save(buildDataset("Dataset Error", "tabla_error"));

        saved.setEstado(DatasetEstado.ERROR);
        saved.setErrorMensaje("Error al parsear CSV");
        Dataset updated = datasetRepository.update(saved);

        assertEquals(DatasetEstado.ERROR, updated.getEstado());
        assertEquals("Error al parsear CSV", updated.getErrorMensaje());
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void deactivateShouldSetEstadoInactive() {
        Dataset saved = datasetRepository.save(buildDataset("Dataset Deactivate", "tabla_deact"));
        saved.setEstado(DatasetEstado.READY);
        datasetRepository.update(saved);

        datasetRepository.deactivate(saved.getId(), "admin@test.com");

        Optional<Dataset> found = datasetRepository.findDatasetById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(DatasetEstado.INACTIVE, found.get().getEstado());
    }

    @Test
    void deactivateShouldThrowNotFoundForNonExistentId() {
        assertThrows(jakarta.ws.rs.NotFoundException.class,
                () -> datasetRepository.deactivate(UUID.randomUUID(), "admin@test.com"));
    }

    // ── reactivate ────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void reactivateShouldSetEstadoReady() {
        Dataset saved = datasetRepository.save(buildDataset("Dataset Reactivate", "tabla_react"));
        datasetRepository.deactivate(saved.getId(), "admin@test.com");

        datasetRepository.reactivate(saved.getId(), "admin@test.com");

        Optional<Dataset> found = datasetRepository.findDatasetById(saved.getId());
        assertEquals(DatasetEstado.READY, found.get().getEstado());
    }
}