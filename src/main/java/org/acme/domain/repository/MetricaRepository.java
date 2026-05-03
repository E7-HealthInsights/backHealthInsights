package org.acme.domain.repository;

import org.acme.domain.models.Metrica;

import java.util.List;
import java.util.UUID;

public interface MetricaRepository {
    List<Metrica> findByDatasetId(UUID datasetId);
    void saveAll(List<Metrica> metricas);
}