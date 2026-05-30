package org.acme.application.usecase;

import jakarta.ws.rs.NotFoundException;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.repository.DatasetRepository;
import org.acme.infrastructure.query.DistinctValuesExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetValoresDistintosUseCaseTest {

    private DatasetRepository datasetRepository;
    private DistinctValuesExecutor distinctValuesExecutor;
    private GetValoresDistintosUseCase useCase;

    private final UUID DATASET_ID   = UUID.randomUUID();
    private final String TABLA      = "imss_diabetes_2023";
    private final String COLUMNA    = "estado";

    private Dataset readyDataset;

    @BeforeEach
    void setUp() {
        datasetRepository      = mock(DatasetRepository.class);
        distinctValuesExecutor = mock(DistinctValuesExecutor.class);

        readyDataset = new Dataset();
        readyDataset.setId(DATASET_ID);
        readyDataset.setNombre("Diabetes IMSS 2023");
        readyDataset.setNombreTabla(TABLA);
        readyDataset.setEstado(DatasetEstado.READY);

        when(datasetRepository.findDatasetById(DATASET_ID)).thenReturn(Optional.of(readyDataset));
        when(distinctValuesExecutor.fetchDistinct(TABLA, COLUMNA, 50))
                .thenReturn(List.of("Jalisco", "CDMX", "Nuevo León"));

        useCase = new GetValoresDistintosUseCase(datasetRepository, distinctValuesExecutor);
    }

    // ── Tests: caso feliz ─────────────────────────────────────────────────────

    @Test
    void executeShouldReturnDistinctValuesForReadyDataset() {
        List<String> result = useCase.execute(DATASET_ID, COLUMNA);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("Jalisco"));
        assertTrue(result.contains("CDMX"));
        assertTrue(result.contains("Nuevo León"));
    }

    @Test
    void executeShouldDelegateToDistinctValuesExecutorWithLimit50() {
        useCase.execute(DATASET_ID, COLUMNA);

        verify(distinctValuesExecutor, times(1)).fetchDistinct(TABLA, COLUMNA, 50);
    }

    @Test
    void executeShouldUseNombreTablaDerivedFromDataset() {
        useCase.execute(DATASET_ID, COLUMNA);

        // Verifica que se usa el nombreTabla del dataset, no el ID
        verify(distinctValuesExecutor, times(1)).fetchDistinct(eq(TABLA), anyString(), anyInt());
    }

    // ── Tests: dataset no encontrado → 404 ───────────────────────────────────

    @Test
    void executeShouldThrowNotFoundWhenDatasetDoesNotExist() {
        UUID unknown = UUID.randomUUID();
        when(datasetRepository.findDatasetById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(unknown, COLUMNA));
    }

    @Test
    void executeShouldNotCallExecutorWhenDatasetNotFound() {
        UUID unknown = UUID.randomUUID();
        when(datasetRepository.findDatasetById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(unknown, COLUMNA));
        verify(distinctValuesExecutor, never()).fetchDistinct(anyString(), anyString(), anyInt());
    }

    // ── Tests: dataset no READY → 404 ────────────────────────────────────────

    @Test
    void executeShouldThrowNotFoundWhenDatasetIsPending() {
        readyDataset.setEstado(DatasetEstado.PENDING);

        assertThrows(NotFoundException.class, () -> useCase.execute(DATASET_ID, COLUMNA));
    }

    @Test
    void executeShouldThrowNotFoundWhenDatasetIsProcessing() {
        readyDataset.setEstado(DatasetEstado.PROCESSING);

        assertThrows(NotFoundException.class, () -> useCase.execute(DATASET_ID, COLUMNA));
    }

    @Test
    void executeShouldThrowNotFoundWhenDatasetIsInError() {
        readyDataset.setEstado(DatasetEstado.ERROR);

        assertThrows(NotFoundException.class, () -> useCase.execute(DATASET_ID, COLUMNA));
    }

    @Test
    void executeShouldThrowNotFoundWhenDatasetIsInactive() {
        readyDataset.setEstado(DatasetEstado.INACTIVE);

        assertThrows(NotFoundException.class, () -> useCase.execute(DATASET_ID, COLUMNA));
    }

    @Test
    void executeShouldNotCallExecutorWhenDatasetIsNotReady() {
        readyDataset.setEstado(DatasetEstado.PENDING);

        assertThrows(NotFoundException.class, () -> useCase.execute(DATASET_ID, COLUMNA));
        verify(distinctValuesExecutor, never()).fetchDistinct(anyString(), anyString(), anyInt());
    }

    // ── Tests: resultado vacío ────────────────────────────────────────────────

    @Test
    void executeShouldReturnEmptyListWhenNoDistinctValuesExist() {
        when(distinctValuesExecutor.fetchDistinct(TABLA, COLUMNA, 50)).thenReturn(List.of());

        List<String> result = useCase.execute(DATASET_ID, COLUMNA);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
