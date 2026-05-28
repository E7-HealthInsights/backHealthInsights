package org.acme.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.repository.DatasetRepository;
import org.acme.infrastructure.csv.CsvIngestService;
import org.acme.infrastructure.storage.GcsStorageService;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.time.LocalDateTime;

@ApplicationScoped
public class DatasetIngestionConsumer {

    private static final Logger LOG = Logger.getLogger(DatasetIngestionConsumer.class);

    @Inject DatasetRepository datasetRepository;
    @Inject CsvIngestService csvIngestService;
    @Inject GcsStorageService gcsStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Incoming("dataset-csv-uploaded-in")
    @Blocking
    public void consume(String payload) {
        DatasetCsvUploadedEvent event;
        try {
            event = objectMapper.readValue(payload, DatasetCsvUploadedEvent.class);
        } catch (Exception e) {
            LOG.errorf("Error al deserializar evento Kafka: %s", e.getMessage());
            return;
        }

        LOG.infof("Mensaje Kafka recibido — datasetId=%s, tabla=%s",
                event.getDatasetId(), event.getNombreTabla());

        markEstado(event, DatasetEstado.PROCESSING, null);

        try {
            ingest(event);
            markEstado(event, DatasetEstado.READY, null);
            LOG.infof("Ingest completado — datasetId=%s → READY", event.getDatasetId());
        } catch (Exception e) {
            LOG.errorf("Error en el ingest de datasetId=%s: %s", event.getDatasetId(), e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            markEstado(event, DatasetEstado.ERROR, truncate(msg, 500));
        } finally {
            try {
                gcsStorageService.delete(event.getGsObjectName());
            } catch (Exception ex) {
                LOG.warnf("No se pudo borrar archivo GCS '%s': %s",
                        event.getGsObjectName(), ex.getMessage());
            }
        }
    }

    private void ingest(DatasetCsvUploadedEvent event) throws Exception {
        try (InputStream csvStream = gcsStorageService.download(event.getGsObjectName())) {
            csvIngestService.crearTablaEInsertarDatos(
                    event.getNombreTabla(),
                    event.getColumnas(),
                    csvStream
            );
        }
    }

    @Transactional
    void markEstado(DatasetCsvUploadedEvent event, DatasetEstado nuevoEstado, String errorMensaje) {
        Dataset dataset = datasetRepository
                .findDatasetById(event.getDatasetId())
                .orElseThrow(() -> new IllegalStateException(
                        "Dataset no encontrado: " + event.getDatasetId()));
        dataset.setEstado(nuevoEstado);
        dataset.setErrorMensaje(errorMensaje);
        dataset.setFechaActualizacion(LocalDateTime.now());
        datasetRepository.update(dataset);
        LOG.infof("Dataset %s → %s", event.getDatasetId(), nuevoEstado);
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
