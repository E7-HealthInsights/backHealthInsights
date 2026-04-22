package org.acme.infrastructure.mapper;

import org.acme.domain.models.Metrica;
import org.acme.infrastructure.entities.MetricaEntity;

public class MetricaMapper {

    public static Metrica toDomain(MetricaEntity entity) {
        Metrica metrica = new Metrica();
        metrica.setId(entity.getId());
        metrica.setNombre(entity.getNombre());
        metrica.setColumnaCsv(entity.getColumnaCsv());
        metrica.setUnidad(entity.getUnidad());
        metrica.setDatasetId(entity.getDataset().getId());
        return metrica;
    }
}