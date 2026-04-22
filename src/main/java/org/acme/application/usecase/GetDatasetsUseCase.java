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

    public List<DatasetResponseDto> execute() {
        return datasetRepository.findAllActive()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private DatasetResponseDto toDto(Dataset dataset) {
        DatasetResponseDto dto = new DatasetResponseDto();
        dto.setId(dataset.getId());
        dto.setNombre(dataset.getNombre());
        dto.setDescripcion(dataset.getDescripcion());
        dto.setFuente(dataset.getFuente());
        dto.setLink(dataset.getLink());
        dto.setFechaActualizacion(dataset.getFechaActualizacion());
        return dto;
    }
}