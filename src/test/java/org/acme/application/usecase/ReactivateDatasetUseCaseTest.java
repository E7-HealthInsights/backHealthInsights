package org.acme.application.usecase;

import org.acme.application.dto.ReactivateDatasetDto;
import org.acme.domain.exception.DatasetNotFoundException;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.models.LogActividad;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReactivateDatasetUseCaseTest {

    private DatasetRepository datasetRepository;
    private AuthContext authContext;
    private LogActividadRepository logActividadRepository;
    private ReactivateDatasetUseCase useCase;

    private final UUID DATASET_ID = UUID.randomUUID();
    private final UUID ADMIN_ID   = UUID.randomUUID();
    private final UUID LOG_ID     = UUID.randomUUID();
    private Dataset existingDataset;
    private LogActividad logEntry;

    @BeforeEach
    void setUp() {
        datasetRepository      = mock(DatasetRepository.class);
        authContext             = mock(AuthContext.class);
        logActividadRepository  = mock(LogActividadRepository.class);
        useCase = new ReactivateDatasetUseCase(datasetRepository, authContext, logActividadRepository);

        User adminUser = new User(ADMIN_ID, "Admin", "Test", "admin@test.com",
                new Role((byte) 1, "ADMIN"), true, "firebase-admin");
        when(authContext.getUser()).thenReturn(adminUser);

        existingDataset = new Dataset(
                DATASET_ID, "Diabetes México 2023", "diabetes_mexico_2023",
                "Casos de diabetes por entidad", "SINAVE",
                null, null, DatasetEstado.INACTIVE, LocalDateTime.now()
        );

        logEntry = mock(LogActividad.class);
        when(logEntry.getId()).thenReturn(LOG_ID);

        when(datasetRepository.findDatasetById(DATASET_ID)).thenReturn(Optional.of(existingDataset));
        doNothing().when(datasetRepository).reactivate(any(UUID.class), any(String.class));
        when(logActividadRepository.findLatestByEntidadId(DATASET_ID.toString()))
                .thenReturn(Optional.of(logEntry));
    }

    @Test
    void executeShouldCallReactivateOnRepository() {
        useCase.execute(DATASET_ID, null);

        verify(datasetRepository, times(1)).reactivate(eq(DATASET_ID), any(String.class));
    }

    @Test
    void executeShouldThrowDatasetNotFoundWhenDatasetDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(datasetRepository.findDatasetById(unknownId)).thenReturn(Optional.empty());

        assertThrows(DatasetNotFoundException.class, () -> useCase.execute(unknownId, null));
        verify(datasetRepository, never()).reactivate(any(UUID.class), any(String.class));
    }

    @Test
    void executeShouldNotCallReactivateWhenDatasetNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(datasetRepository.findDatasetById(unknownId)).thenReturn(Optional.empty());

        assertThrows(DatasetNotFoundException.class, () -> useCase.execute(unknownId, null));

        verify(datasetRepository, never()).reactivate(eq(unknownId), any(String.class));
    }

    @Test
    void executeShouldPassAdminIdAsModifiedBy() {
        useCase.execute(DATASET_ID, null);

        verify(datasetRepository, times(1)).reactivate(DATASET_ID, ADMIN_ID.toString());
    }

    @Test
    void executeShouldUpdateDetalleWhenJustificationProvided() {
        ReactivateDatasetDto dto = new ReactivateDatasetDto();
        dto.setJustification("Dataset revisado y aprobado");

        useCase.execute(DATASET_ID, dto);

        verify(logActividadRepository, times(1)).updateDetalle(LOG_ID, "Dataset revisado y aprobado");
    }

    @Test
    void executeShouldNotUpdateDetalleWhenJustificationIsNull() {
        ReactivateDatasetDto dto = new ReactivateDatasetDto();

        useCase.execute(DATASET_ID, dto);

        verify(logActividadRepository, never()).updateDetalle(any(), any());
    }

    @Test
    void executeShouldNotUpdateDetalleWhenDtoIsNull() {
        useCase.execute(DATASET_ID, null);

        verify(logActividadRepository, never()).updateDetalle(any(), any());
    }

    @Test
    void executeShouldLookUpDatasetBeforeReactivating() {
        useCase.execute(DATASET_ID, null);

        verify(datasetRepository, times(1)).findDatasetById(DATASET_ID);
        verify(datasetRepository, times(1)).reactivate(eq(DATASET_ID), any(String.class));
    }
}
