package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.DeactivateDatasetDto;
import org.acme.domain.exception.DatasetNotFoundException;
import org.acme.domain.repository.DatasetRepository;

import java.util.UUID;

@ApplicationScoped
public class DeactivateDatasetUseCase {

    private final DatasetRepository datasetRepository;

    @Inject
    public DeactivateDatasetUseCase(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    public void execute(UUID datasetId, DeactivateDatasetDto dto) {
        datasetRepository.findDatasetById(datasetId)
                .orElseThrow(() -> new DatasetNotFoundException(datasetId));

        datasetRepository.deactivate(datasetId);
    }
}
