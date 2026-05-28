package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.ReactivateDatasetDto;
import org.acme.domain.exception.DatasetNotFoundException;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.UUID;

@ApplicationScoped
public class ReactivateDatasetUseCase {

    private final DatasetRepository datasetRepository;
    private final AuthContext authContext;
    private final LogActividadRepository logActividadRepository;

    @Inject
    public ReactivateDatasetUseCase(DatasetRepository datasetRepository,
                                    AuthContext authContext,
                                    LogActividadRepository logActividadRepository) {
        this.datasetRepository = datasetRepository;
        this.authContext = authContext;
        this.logActividadRepository = logActividadRepository;
    }

    public void execute(UUID datasetId, ReactivateDatasetDto dto) {
        datasetRepository.findDatasetById(datasetId)
                .orElseThrow(() -> new DatasetNotFoundException(datasetId));

        datasetRepository.reactivate(datasetId, authContext.getUser().getId().toString());

        if (dto != null) {
            logActividadRepository
                    .findLatestByEntidadId(datasetId.toString())
                    .ifPresent(log -> {
                        if (dto.getJustification() != null
                                && !dto.getJustification().isBlank()) {
                            logActividadRepository.updateDetalle(
                                    log.getId(),
                                    dto.getJustification()
                            );
                        }
                    });
        }
    }
}
