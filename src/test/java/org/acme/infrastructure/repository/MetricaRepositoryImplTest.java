package org.acme.infrastructure.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.models.Metrica;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.MetricaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MetricaRepositoryImplTest {

    @Inject MetricaRepository metricaRepository;
    @Inject DatasetRepository datasetRepository;
    @Inject EntityManager em;

    private UUID datasetId;

    @BeforeEach
    @Transactional
    void setUp() {
        em.createQuery("DELETE FROM MetricaEntity").executeUpdate();
        em.createNativeQuery("DELETE FROM Dataset").executeUpdate();

        datasetId = UUID.randomUUID();
        Dataset dataset = new Dataset();
        dataset.setId(datasetId);
        dataset.setNombre("Dataset Métricas Test");
        dataset.setNombreTabla("dataset_metricas_test");
        dataset.setDescripcion("Para tests de métricas");
        dataset.setFuente("Test");
        dataset.setEstado(DatasetEstado.READY);
        dataset.setFechaActualizacion(LocalDateTime.now());
        datasetRepository.save(dataset);
    }

    private Metrica buildMetrica(String nombre, String columna) {
        Metrica m = new Metrica();
        m.setId(UUID.randomUUID());
        m.setNombre(nombre);
        m.setColumnaCsv(columna);
        m.setUnidad("unidad_test");
        m.setDatasetId(datasetId);
        return m;
    }

    // ── saveAll ───────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void saveAllShouldPersistAllMetricas() {
        metricaRepository.saveAll(List.of(
                buildMetrica("Tasa de Detección", "tasa_deteccion"),
                buildMetrica("Total Casos", "total_casos")
        ));

        List<Metrica> saved = metricaRepository.findByDatasetId(datasetId);
        assertEquals(2, saved.size());
    }

    @Test
    void saveAllShouldHandleEmptyList() {
        assertDoesNotThrow(() -> metricaRepository.saveAll(List.of()));

        List<Metrica> result = metricaRepository.findByDatasetId(datasetId);
        assertTrue(result.isEmpty());
    }

    // ── findByDatasetId ───────────────────────────────────────────────────────

    @Test
    @Transactional
    void findByDatasetIdShouldReturnMetricasOfDataset() {
        metricaRepository.saveAll(List.of(
                buildMetrica("Metrica A", "col_a"),
                buildMetrica("Metrica B", "col_b")
        ));

        List<Metrica> result = metricaRepository.findByDatasetId(datasetId);

        assertEquals(2, result.size());
    }

    @Test
    @Transactional
    void findByDatasetIdShouldReturnEmptyForOtherDataset() {
        metricaRepository.saveAll(List.of(buildMetrica("Metrica X", "col_x")));

        List<Metrica> result = metricaRepository.findByDatasetId(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    // ── findByColumnaCsvAndDatasetId ──────────────────────────────────────────

    @Test
    @Transactional
    void findByColumnaCsvAndDatasetIdShouldReturnMetricaWhenExists() {
        metricaRepository.saveAll(List.of(buildMetrica("Estado", "estado")));

        Optional<Metrica> found = metricaRepository.findByColumnaCsvAndDatasetId("estado", datasetId);

        assertTrue(found.isPresent());
        assertEquals("Estado", found.get().getNombre());
    }

    @Test
    void findByColumnaCsvAndDatasetIdShouldReturnEmptyWhenNotFound() {
        Optional<Metrica> found = metricaRepository.findByColumnaCsvAndDatasetId("no_existe", datasetId);
        assertTrue(found.isEmpty());
    }

    @Test
    @Transactional
    void findByColumnaCsvAndDatasetIdShouldReturnEmptyForWrongDataset() {
        metricaRepository.saveAll(List.of(buildMetrica("Estado", "estado")));

        Optional<Metrica> found = metricaRepository.findByColumnaCsvAndDatasetId("estado", UUID.randomUUID());

        assertTrue(found.isEmpty());
    }
}