package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domain.models.Metrica;
import org.acme.domain.repository.MetricaRepository;
import org.acme.infrastructure.entities.DatasetEntity;
import org.acme.infrastructure.entities.MetricaEntity;
import org.acme.infrastructure.mapper.MetricaMapper;

import java.util.List;
import java.util.Optional;
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

    @Override
    public void saveAll(List<Metrica> metricas) {
        metricas.forEach(m -> {
            MetricaEntity entity = new MetricaEntity();
            entity.setId(m.getId());
            entity.setNombre(m.getNombre());
            entity.setColumnaCsv(m.getColumnaCsv());
            entity.setUnidad(m.getUnidad());

            // getReference devuelve el proxy que Hibernate ya tiene en su contexto,
            // evitando el error de "unsaved transient entity"
            DatasetEntity datasetRef = getEntityManager().getReference(DatasetEntity.class, m.getDatasetId());
            entity.setDataset(datasetRef);

            persist(entity);
        });
    }

    @Override
    public Optional<Metrica> findByColumnaCsvAndDatasetId(String columnaCsv, UUID datasetId) {
        return find("columnaCsv = ?1 and dataset.id = ?2", columnaCsv, datasetId)
                .firstResultOptional()
                .map(MetricaMapper::toDomain);
    }
}