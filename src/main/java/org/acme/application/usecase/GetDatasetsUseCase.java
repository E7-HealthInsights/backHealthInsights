package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.DatasetResponseDto;
import org.acme.domain.models.Dataset;
import org.acme.domain.repository.DatasetRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetDatasetsUseCase {

    private final DatasetRepository datasetRepository;

    @Inject
    public GetDatasetsUseCase(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    public List<Dataset> execute() {
        return datasetRepository.findAllActive();
    }

}