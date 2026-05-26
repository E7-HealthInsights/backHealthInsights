package org.acme.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.application.dto.ColumnDefinitionDto;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.repository.DatasetRepository;
import org.acme.infrastructure.csv.CsvIngestService;
import org.acme.infrastructure.storage.GcsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatasetIngestionConsumerTest {

    private DatasetRepository datasetRepository;
    private CsvIngestService  csvIngestService;
    private GcsStorageService gcsStorageService;
    private DatasetIngestionConsumer consumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        datasetRepository = mock(DatasetRepository.class);
        csvIngestService  = mock(CsvIngestService.class);
        gcsStorageService = mock(GcsStorageService.class);

        consumer = new DatasetIngestionConsumer();
        consumer.datasetRepository = datasetRepository;
        consumer.csvIngestService  = csvIngestService;
        consumer.gcsStorageService = gcsStorageService;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DatasetCsvUploadedEvent buildEvent() {
        ColumnDefinitionDto col = new ColumnDefinitionDto();
        col.setOriginalName("valor");
        col.setDisplayName("Valor");
        col.setSqlType("INT");

        return new DatasetCsvUploadedEvent(
                UUID.randomUUID(),
                "mi_tabla",
                "datasets/uuid/archivo.csv",
                List.of(col)
        );
    }

    private Dataset buildDataset(UUID id) {
        Dataset d = new Dataset();
        d.setId(id);
        d.setEstado(DatasetEstado.PENDING);
        return d;
    }

    private String toJson(DatasetCsvUploadedEvent event) throws Exception {
        return objectMapper.writeValueAsString(event);
    }

    // ── Tests: flujo feliz ────────────────────────────────────────────────────

    @Test
    void consumeShouldMarkProcessingThenReady() throws Exception {
        DatasetCsvUploadedEvent event = buildEvent();
        Dataset dataset = buildDataset(event.getDatasetId());

        when(datasetRepository.findDatasetById(event.getDatasetId()))
                .thenReturn(Optional.of(dataset));
        when(datasetRepository.update(any())).thenReturn(dataset);
        when(gcsStorageService.download(anyString()))
                .thenReturn(new ByteArrayInputStream("col\nval".getBytes()));

        consumer.consume(toJson(event));

        verify(datasetRepository, times(2)).update(any(Dataset.class));
        verify(gcsStorageService).delete(anyString());
    }

    @Test
    void consumeShouldCallCsvIngestWithCorrectTableName() throws Exception {
        DatasetCsvUploadedEvent event = buildEvent();
        Dataset dataset = buildDataset(event.getDatasetId());

        when(datasetRepository.findDatasetById(event.getDatasetId()))
                .thenReturn(Optional.of(dataset));
        when(datasetRepository.update(any())).thenReturn(dataset);
        when(gcsStorageService.download(anyString()))
                .thenReturn(new ByteArrayInputStream("col\nval".getBytes()));

        consumer.consume(toJson(event));

        verify(csvIngestService).crearTablaEInsertarDatos(
                eq("mi_tabla"),
                anyList(),
                any()
        );
    }

    @Test
    void consumeShouldDeleteGcsFileAfterSuccessfulIngest() throws Exception {
        DatasetCsvUploadedEvent event = buildEvent();
        Dataset dataset = buildDataset(event.getDatasetId());

        when(datasetRepository.findDatasetById(event.getDatasetId()))
                .thenReturn(Optional.of(dataset));
        when(datasetRepository.update(any())).thenReturn(dataset);
        when(gcsStorageService.download(anyString()))
                .thenReturn(new ByteArrayInputStream("col\nval".getBytes()));

        consumer.consume(toJson(event));

        verify(gcsStorageService).delete("datasets/uuid/archivo.csv");
    }

    // ── Tests: manejo de errores ──────────────────────────────────────────────

    @Test
    void consumeShouldMarkErrorWhenIngestFails() throws Exception {
        DatasetCsvUploadedEvent event = buildEvent();
        Dataset dataset = buildDataset(event.getDatasetId());

        when(datasetRepository.findDatasetById(event.getDatasetId()))
                .thenReturn(Optional.of(dataset));
        when(datasetRepository.update(any())).thenReturn(dataset);
        when(gcsStorageService.download(anyString()))
                .thenReturn(new ByteArrayInputStream("col\nval".getBytes()));
        doThrow(new RuntimeException("Error de BD al crear tabla"))
                .when(csvIngestService).crearTablaEInsertarDatos(any(), any(), any());

        consumer.consume(toJson(event));

        verify(datasetRepository, atLeastOnce()).update(argThat(d ->
                d.getEstado() == DatasetEstado.ERROR &&
                        d.getErrorMensaje() != null &&
                        d.getErrorMensaje().contains("Error de BD")
        ));
    }

    @Test
    void consumeShouldDeleteGcsFileEvenWhenIngestFails() throws Exception {
        DatasetCsvUploadedEvent event = buildEvent();
        Dataset dataset = buildDataset(event.getDatasetId());

        when(datasetRepository.findDatasetById(event.getDatasetId()))
                .thenReturn(Optional.of(dataset));
        when(datasetRepository.update(any())).thenReturn(dataset);
        when(gcsStorageService.download(anyString()))
                .thenReturn(new ByteArrayInputStream("col\nval".getBytes()));
        doThrow(new RuntimeException("fallo"))
                .when(csvIngestService).crearTablaEInsertarDatos(any(), any(), any());

        consumer.consume(toJson(event));

        verify(gcsStorageService).delete(anyString());
    }

    @Test
    void consumeShouldIgnoreMessageWhenJsonIsInvalid() {
        consumer.consume("esto no es json valido {{{");

        verifyNoInteractions(datasetRepository);
        verifyNoInteractions(csvIngestService);
        verifyNoInteractions(gcsStorageService);
    }

    @Test
    void consumeShouldContinueWhenGcsDeletionFails() throws Exception {
        DatasetCsvUploadedEvent event = buildEvent();
        Dataset dataset = buildDataset(event.getDatasetId());

        when(datasetRepository.findDatasetById(event.getDatasetId()))
                .thenReturn(Optional.of(dataset));
        when(datasetRepository.update(any())).thenReturn(dataset);
        when(gcsStorageService.download(anyString()))
                .thenReturn(new ByteArrayInputStream("col\nval".getBytes()));
        doThrow(new RuntimeException("GCS no disponible"))
                .when(gcsStorageService).delete(anyString());

        consumer.consume(toJson(event));

        verify(datasetRepository, atLeastOnce()).update(argThat(d ->
                d.getEstado() == DatasetEstado.READY));
    }
}
