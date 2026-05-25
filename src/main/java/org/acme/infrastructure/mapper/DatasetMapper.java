package org.acme.infrastructure.mapper;

import org.acme.domain.models.Dataset;
import org.acme.infrastructure.entities.DatasetEntity;

public class DatasetMapper {

    public static Dataset toDomain(DatasetEntity entity) {
        Dataset dataset = new Dataset();
        dataset.setId(entity.getId());
        dataset.setNombre(entity.getNombre());
        dataset.setNombreTabla(entity.getNombreTabla());
        dataset.setDescripcion(entity.getDescripcion());
        dataset.setFuente(entity.getFuente());
        dataset.setArchivoCsv(entity.getArchivoCsv());
        dataset.setLink(entity.getLink());
        dataset.setEstado(entity.getEstado());
        dataset.setErrorMensaje(entity.getErrorMensaje());
        dataset.setFechaActualizacion(entity.getFechaActualizacion());
        return dataset;
    }

    public static DatasetEntity toEntity(Dataset dataset) {
        DatasetEntity entity = new DatasetEntity();
        entity.setId(dataset.getId());
        entity.setNombre(dataset.getNombre());
        entity.setNombreTabla(dataset.getNombreTabla());
        entity.setDescripcion(dataset.getDescripcion());
        entity.setFuente(dataset.getFuente());
        entity.setArchivoCsv(dataset.getArchivoCsv());
        entity.setLink(dataset.getLink());
        entity.setEstado(dataset.getEstado());
        entity.setErrorMensaje(dataset.getErrorMensaje());
        entity.setFechaActualizacion(dataset.getFechaActualizacion());
        return entity;
    }
}
