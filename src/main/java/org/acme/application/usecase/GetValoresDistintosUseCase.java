package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.repository.DatasetRepository;
import org.acme.infrastructure.query.DistinctValuesExecutor;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GetValoresDistintosUseCase {

    private final DatasetRepository datasetRepository;
    private final DistinctValuesExecutor distinctValuesExecutor;

    @Inject
    public GetValoresDistintosUseCase(DatasetRepository datasetRepository,
                                      DistinctValuesExecutor distinctValuesExecutor) {
        this.datasetRepository = datasetRepository;
        this.distinctValuesExecutor = distinctValuesExecutor;
    }

    /**
     * Devuelve hasta 50 valores distintos de una columna para una tabla dada.
     * Si hay más de 50 valores únicos, se devuelven igual los 50 primeros
     * para que el front pueda usarlos como ejemplo en el placeholder.
     *
     * @throws NotFoundException si el dataset no existe o no está en estado READY
     */
    public List<String> execute(UUID datasetId, String columna) {
        String nombreTabla = datasetRepository.findDatasetById(datasetId)
                .filter(d -> d.getEstado() == DatasetEstado.READY)
                .orElseThrow(() -> new NotFoundException("Dataset no encontrado: " + datasetId))
                .getNombreTabla();

        return distinctValuesExecutor.fetchDistinct(nombreTabla, columna, 50);
    }
}
