package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.application.dto.ColumnDefinitionDto;
import org.acme.application.dto.UploadDatasetDto;
import org.acme.domain.exception.TableAlreadyExistsException;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.Metrica;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.MetricaRepository;
import org.acme.infrastructure.csv.CsvIngestService;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UploadDatasetUseCase {

    private static final Logger LOG = Logger.getLogger(UploadDatasetUseCase.class);

    private final DatasetRepository datasetRepository;
    private final MetricaRepository metricaRepository;
    private final CsvIngestService csvIngestService;

    @Inject
    public UploadDatasetUseCase(
            DatasetRepository datasetRepository,
            MetricaRepository metricaRepository,
            CsvIngestService csvIngestService
    ) {
        this.datasetRepository = datasetRepository;
        this.metricaRepository = metricaRepository;
        this.csvIngestService  = csvIngestService;
    }

    @Transactional
    public Dataset execute(UploadDatasetDto dto) {

        List<ColumnDefinitionDto> columnas = dto.getColumnas();

        // 1 — Generar nombre de tabla a partir del nombre del archivo CSV
        String nombreTabla = slugify(dto.getArchivoNombre());

        // 2 — Validar que no exista ya esa tabla
        if (datasetRepository.existsByNombreTabla(nombreTabla)) {
            throw new TableAlreadyExistsException(nombreTabla);
        }

        // 3 — Construir y persistir el Dataset
        Dataset dataset = new Dataset();
        dataset.setId(UUID.randomUUID());
        dataset.setNombre(dto.getNombre());
        dataset.setNombreTabla(nombreTabla);
        dataset.setDescripcion(dto.getDescripcion());
        dataset.setFuente(dto.getFuente());
        dataset.setArchivoCsv(dto.getArchivoNombre());
        dataset.setEstado(true);
        dataset.setFechaActualizacion(LocalDateTime.now());
        dataset.setModifiedBy(dto.getModifiedBy());

        Dataset savedDataset = datasetRepository.save(dataset);
        LOG.infof("Dataset '%s' persistido con id=%s, tabla='%s'",
                savedDataset.getNombre(), savedDataset.getId(), nombreTabla);

        // 4 — Construir y persistir las Metricas
        List<Metrica> metricas = columnas.stream().map(col -> {
            Metrica m = new Metrica();
            m.setId(UUID.randomUUID());
            m.setNombre(col.getDisplayName());
            m.setColumnaCsv(col.getOriginalName());
            m.setUnidad(col.getUnidad());
            m.setDatasetId(savedDataset.getId());
            return m;
        }).toList();

        metricaRepository.saveAll(metricas);
        LOG.infof("%d metricas persistidas para dataset id=%s", metricas.size(), savedDataset.getId());

        // 5 — Decodificar base64 y cargar datos del CSV
        byte[] csvBytes;
        try {
            csvBytes = Base64.getDecoder().decode(dto.getArchivoCsvBase64());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El archivo CSV tiene codificacion base64 invalida.", e);
        }

        try (InputStream csvStream = new ByteArrayInputStream(csvBytes)) {
            csvIngestService.crearTablaEInsertarDatos(nombreTabla, columnas, csvStream);
        } catch (SQLException | IOException e) {
            LOG.errorf("Error al cargar datos en tabla '%s': %s", nombreTabla, e.getMessage());
            throw new RuntimeException(
                    "Error al procesar el archivo CSV. Por favor intenta de nuevo.", e);
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