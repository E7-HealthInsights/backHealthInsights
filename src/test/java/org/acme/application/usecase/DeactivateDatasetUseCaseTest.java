package org.acme.application.usecase;

import org.acme.application.dto.DeactivateDatasetDto;
import org.acme.domain.exception.DatasetNotFoundException;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.repository.DatasetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeactivateDatasetUseCaseTest {

    private DatasetRepository datasetRepository;
    private DeactivateDatasetUseCase useCase;

    private final UUID DATASET_ID = UUID.randomUUID();
    private Dataset existingDataset;

    @BeforeEach
    void setUp() {
        datasetRepository = mock(DatasetRepository.class);
        useCase = new DeactivateDatasetUseCase(datasetRepository);

        existingDataset = new Dataset(
                DATASET_ID,
                "Diabetes México 2023",
                "diabetes_mexico_2023",
                "Casos de diabetes por entidad",
                "SINAVE",
                null,
                null,
                DatasetEstado.READY,
                LocalDateTime.now()
        );

        when(datasetRepository.findDatasetById(DATASET_ID)).thenReturn(Optional.of(existingDataset));
        doNothing().when(datasetRepository).deactivate(DATASET_ID);
    }

    @Test
    void executeShouldCallDeactivateOnRepository() {
        useCase.execute(DATASET_ID, null);

        verify(datasetRepository, times(1)).deactivate(DATASET_ID);
    }

    @Test
    void executeShouldThrowDatasetNotFoundWhenDatasetDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(datasetRepository.findDatasetById(unknownId)).thenReturn(Optional.empty());

        assertThrows(DatasetNotFoundException.class, () -> useCase.execute(unknownId, null));
        verify(datasetRepository, never()).deactivate(any());
    }

    @Test
    void executeShouldNotCallDeactivateWhenDatasetNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(datasetRepository.findDatasetById(unknownId)).thenReturn(Optional.empty());

        assertThrows(DatasetNotFoundException.class, () -> useCase.execute(unknownId, null));

        verify(datasetRepository, never()).deactivate(unknownId);
    }

    @Test
    void executeShouldWorkWithNullDto() {
        assertDoesNotThrow(() -> useCase.execute(DATASET_ID, null));

        verify(datasetRepository, times(1)).deactivate(DATASET_ID);
    }

    @Test
    void executeShouldWorkWithEmptyDto() {
        DeactivateDatasetDto dto = new DeactivateDatasetDto();

        assertDoesNotThrow(() -> useCase.execute(DATASET_ID, dto));

        verify(datasetRepository, times(1)).deactivate(DATASET_ID);
    }

    @Test
    void executeShouldLookUpDatasetBeforeDeactivating() {
        useCase.execute(DATASET_ID, null);

        verify(datasetRepository, times(1)).findDatasetById(DATASET_ID);
        verify(datasetRepository, times(1)).deactivate(DATASET_ID);
    }
}
