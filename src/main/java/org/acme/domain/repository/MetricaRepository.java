package org.acme.domain.repository;

import org.acme.domain.models.Metrica;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetricaRepository {
    List<Metrica> findByDatasetId(UUID datasetId);
    void saveAll(List<Metrica> metricas);
    Optional<Metrica> findByColumnaCsvAndDatasetId(String columnaCsv, UUID datasetId);
}