package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domain.models.Metrica;
import org.acme.domain.repository.MetricaRepository;
import org.acme.infrastructure.entities.MetricaEntity;
import org.acme.infrastructure.mapper.MetricaMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class MetricaRepositoryImpl implements MetricaRepository, PanacheRepositoryBase<MetricaEntity, UUID> {

    @Override
    public List<Metrica> findByDatasetId(UUID datasetId) {
        return find("dataset.id", datasetId)
                .list()
                .stream()
                .map(MetricaMapper::toDomain)
                .collect(Collectors.toList());
    }
}