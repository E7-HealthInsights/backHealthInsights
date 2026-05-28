package org.acme.application.usecase;

import jakarta.ws.rs.NotFoundException;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.Metrica;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.MetricaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetMetricasByDatasetUseCaseTest {

    private DatasetRepository datasetRepository;
    private MetricaRepository metricaRepository;
    private GetMetricasByDatasetUseCase useCase;

    private UUID datasetId;
    private Dataset activeDataset;

    @BeforeEach
    void setUp() {
        datasetRepository  = mock(DatasetRepository.class);
        metricaRepository  = mock(MetricaRepository.class);
        useCase            = new GetMetricasByDatasetUseCase(datasetRepository, metricaRepository);

        datasetId     = UUID.randomUUID();
        activeDataset = new Dataset();
        activeDataset.setId(datasetId);
        activeDataset.setNombre("Diabetes México 2023");
        activeDataset.setEstado(org.acme.domain.models.DatasetEstado.READY);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Metrica buildMetrica(String nombre, String columna, String unidad) {
        Metrica m = new Metrica();
        m.setId(UUID.randomUUID());
        m.setNombre(nombre);
        m.setColumnaCsv(columna);
        m.setUnidad(unidad);
        m.setDatasetId(datasetId);
        return m;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void executeShouldReturnMetricasForActiveDataset() {
        Metrica m1 = buildMetrica("Estado",  "estado",  null);
        Metrica m2 = buildMetrica("Casos",   "casos",   null);
        Metrica m3 = buildMetrica("Presupuesto", "presupuesto", "MXN");

        when(datasetRepository.findDatasetById(datasetId)).thenReturn(Optional.of(activeDataset));
        when(metricaRepository.findByDatasetId(datasetId)).thenReturn(List.of(m1, m2, m3));

        List<Metrica> result = useCase.execute(datasetId);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Estado",      result.get(0).getNombre());
        assertEquals("Casos",       result.get(1).getNombre());
        assertEquals("Presupuesto", result.get(2).getNombre());
        assertEquals("MXN",         result.get(2).getUnidad());
    }

    @Test
    void executeShouldThrowNotFoundWhenDatasetDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(datasetRepository.findDatasetById(unknownId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(unknownId));
        // Si el dataset no existe, nunca se consultan sus métricas
        verify(metricaRepository, never()).findByDatasetId(any());
    }

    @Test
    void executeShouldThrowNotFoundWhenDatasetIsInactive() {
        Dataset inactiveDataset = new Dataset();
        inactiveDataset.setId(datasetId);
        inactiveDataset.setNombre("Dataset desactivado");
        inactiveDataset.setEstado(org.acme.domain.models.DatasetEstado.PENDING);   // inactivo

        when(datasetRepository.findDatasetById(datasetId)).thenReturn(Optional.of(inactiveDataset));

        assertThrows(NotFoundException.class, () -> useCase.execute(datasetId));
        // Dataset inactivo se trata igual que inexistente
        verify(metricaRepository, never()).findByDatasetId(any());
    }

    @Test
    void executeShouldReturnEmptyListWhenDatasetHasNoMetricas() {
        when(datasetRepository.findDatasetById(datasetId)).thenReturn(Optional.of(activeDataset));
        when(metricaRepository.findByDatasetId(datasetId)).thenReturn(List.of());

        List<Metrica> result = useCase.execute(datasetId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(metricaRepository, times(1)).findByDatasetId(datasetId);
    }

    @Test
    void executeShouldOnlyQueryMetricasForTheRequestedDataset() {
        UUID otroDatasetId = UUID.randomUUID();

        when(datasetRepository.findDatasetById(datasetId)).thenReturn(Optional.of(activeDataset));
        when(metricaRepository.findByDatasetId(datasetId)).thenReturn(List.of());

        useCase.execute(datasetId);

        // Nunca consulta métricas de otro dataset
        verify(metricaRepository, never()).findByDatasetId(otroDatasetId);
        verify(metricaRepository, times(1)).findByDatasetId(datasetId);
    }

    @Test
    void executeShouldPreserveMetricaFields() {
        Metrica m = buildMetrica("Edad promedio", "edad_promedio", "años");

        when(datasetRepository.findDatasetById(datasetId)).thenReturn(Optional.of(activeDataset));
        when(metricaRepository.findByDatasetId(datasetId)).thenReturn(List.of(m));

        List<Metrica> result = useCase.execute(datasetId);

        Metrica returned = result.get(0);
        assertEquals(m.getId(),          returned.getId());
        assertEquals(m.getNombre(),      returned.getNombre());
        assertEquals(m.getColumnaCsv(),  returned.getColumnaCsv());
        assertEquals(m.getUnidad(),      returned.getUnidad());
        assertEquals(m.getDatasetId(),   returned.getDatasetId());
    }
}