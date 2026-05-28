package org.acme.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.application.dto.UploadDatasetDto;
import org.acme.domain.exception.TableAlreadyExistsException;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.models.Metrica;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.MetricaRepository;
import org.acme.infrastructure.messaging.DatasetCsvUploadedEvent;
import org.acme.infrastructure.security.AuthContext;
import org.acme.infrastructure.storage.GcsStorageService;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: registrar un dataset y disparar el ingest asíncrono.
 *
 * Flujo (asíncrono con Kafka):
 *  1. Validar que la tabla no exista ya.
 *  2. Decodificar el CSV base64 y subirlo a GCS.
 *  3. Persistir el Dataset en estado PENDING.
 *  4. Persistir las Métricas.
 *  5. Publicar el evento DatasetCsvUploadedEvent al topic Kafka.
 *  6. Responder 202 Accepted — el ingest ocurre en el consumer.
 */
@ApplicationScoped
public class UploadDatasetUseCase {

    private static final Logger LOG = Logger.getLogger(UploadDatasetUseCase.class);

    private final DatasetRepository datasetRepository;
    private final MetricaRepository metricaRepository;
    private final GcsStorageService gcsStorageService;
    private final AuthContext authContext;
    private final EntityManager em;
    private final Emitter<String> emitter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public UploadDatasetUseCase(
            DatasetRepository datasetRepository,
            MetricaRepository metricaRepository,
            GcsStorageService gcsStorageService,
            AuthContext authContext,
            EntityManager em,
            @Channel("dataset-csv-uploaded-out") Emitter<String> emitter
    ) {
        this.datasetRepository = datasetRepository;
        this.metricaRepository = metricaRepository;
        this.gcsStorageService = gcsStorageService;
        this.authContext       = authContext;
        this.em                = em;
        this.emitter           = emitter;
    }

    @Transactional
    public Dataset execute(UploadDatasetDto dto) {

        // 1 — Slug y validación de unicidad
        String nombreTabla = slugify(dto.getArchivoNombre());
        if (datasetRepository.existsByNombreTabla(nombreTabla)) {
            throw new TableAlreadyExistsException(nombreTabla);
        }

        // 2 — Decodificar CSV y subir a GCS
        byte[] csvBytes;
        try {
            csvBytes = Base64.getDecoder().decode(dto.getArchivoCsvBase64());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El archivo CSV tiene codificación base64 inválida.", e);
        }

        UUID datasetId    = UUID.randomUUID();
        String objectName = "datasets/" + datasetId + "/" + dto.getArchivoNombre();
        gcsStorageService.upload(objectName, csvBytes);
        LOG.infof("CSV subido a GCS — objectName=%s", objectName);

        // 3 — Persistir Dataset en estado PENDING
        Dataset dataset = new Dataset();
        dataset.setId(datasetId);
        dataset.setNombre(dto.getNombre());
        dataset.setNombreTabla(nombreTabla);
        dataset.setDescripcion(dto.getDescripcion());
        dataset.setFuente(dto.getFuente());
        dataset.setArchivoCsv(dto.getArchivoNombre());
        dataset.setEstado(DatasetEstado.PENDING);
        dataset.setFechaActualizacion(LocalDateTime.now());
        dataset.setModifiedBy(authContext.getUser().getId().toString());

        Dataset savedDataset = datasetRepository.save(dataset);
        LOG.infof("Dataset '%s' persistido — id=%s, estado=PENDING", savedDataset.getNombre(), savedDataset.getId());

        // 4 — Persistir Métricas
        List<Metrica> metricas = dto.getColumnas().stream().map(col -> {
            Metrica m = new Metrica();
            m.setId(UUID.randomUUID());
            m.setNombre(col.getDisplayName());
            m.setColumnaCsv(col.getOriginalName());
            m.setUnidad(col.getUnidad());
            m.setDatasetId(savedDataset.getId());
            return m;
        }).toList();

        metricaRepository.saveAll(metricas);
        LOG.infof("%d métricas persistidas para dataset id=%s", metricas.size(), savedDataset.getId());

        // 5 — Publicar evento Kafka
        DatasetCsvUploadedEvent event = new DatasetCsvUploadedEvent(
                savedDataset.getId(),
                nombreTabla,
                objectName,
                dto.getColumnas()
        );

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            emitter.send(eventJson);
            LOG.infof("Evento Kafka publicado — datasetId=%s, objectName=%s",
                    savedDataset.getId(), objectName);
        } catch (Exception e) {
            LOG.errorf("Error al publicar evento Kafka para datasetId=%s: %s",
                    savedDataset.getId(), e.getMessage());
            throw new RuntimeException("Error al encolar el procesamiento del dataset.", e);
        }

        // 6 — Flush para que el trigger de LogActividad dispare dentro de la transacción
        em.flush();

        if (dto.getJustification() != null && !dto.getJustification().isBlank()) {
            em.createNativeQuery(
                "UPDATE LogActividad " +
                "SET detalle = :detalle " +
                "WHERE entidad_id = :entidadId " +
                "ORDER BY fecha DESC LIMIT 1"
            )
            .setParameter("detalle", dto.getJustification())
            .setParameter("entidadId", savedDataset.getId().toString())
            .executeUpdate();
        }

        return savedDataset;
    }

    private String slugify(String fileName) {
        String name = fileName.replaceAll("(?i)\\.csv$", "");
        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
