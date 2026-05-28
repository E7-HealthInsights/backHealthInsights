package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.MetricaResponseDto;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.models.Metrica;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.MetricaRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetMetricasByDatasetUseCase {

    private final DatasetRepository datasetRepository;
    private final MetricaRepository metricaRepository;

    @Inject
    public GetMetricasByDatasetUseCase(DatasetRepository datasetRepository,
                                       MetricaRepository metricaRepository) {
        this.datasetRepository = datasetRepository;
        this.metricaRepository = metricaRepository;
    }

    /**
     * @throws jakarta.ws.rs.NotFoundException si el dataset no existe o está inactivo
     */
    public List<Metrica> execute(UUID datasetId) {
        // Valida que el dataset exista y esté activo antes de devolver métricas
        datasetRepository.findDatasetById(datasetId)
                .filter(d -> d.getEstado() == DatasetEstado.READY)
                .orElseThrow(() -> new jakarta.ws.rs.NotFoundException(
                        "Dataset no encontrado: " + datasetId));

        return metricaRepository.findByDatasetId(datasetId);
    }

}