package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domain.models.Dataset;
import org.acme.domain.repository.DatasetRepository;
import org.acme.infrastructure.entities.DatasetEntity;
import org.acme.infrastructure.mapper.DatasetMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class DatasetRepositoryImpl implements DatasetRepository, PanacheRepositoryBase<DatasetEntity, UUID> {

    @Override
    public List<Dataset> findAllActive() {
        return find("estado", true)
                .list()
                .stream()
                .map(DatasetMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Dataset> findDatasetById(UUID id) {
        return findByIdOptional(id).map(DatasetMapper::toDomain);
    }
}