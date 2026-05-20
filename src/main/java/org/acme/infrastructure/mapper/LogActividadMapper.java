package org.acme.infrastructure.mapper;

import org.acme.domain.models.LogActividad;
import org.acme.infrastructure.entities.LogActividadEntity;

public class LogActividadMapper {

    public static LogActividad toDomain(LogActividadEntity entity) {
        LogActividad log = new LogActividad();
        log.setId(entity.getId());
        log.setUsuarioId(entity.getUsuarioId());
        log.setAccion(entity.getAccion());
        log.setDetalle(entity.getDetalle());
        log.setEntidadTipo(entity.getEntidadTipo());
        log.setEntidadId(entity.getEntidadId());
        log.setFecha(entity.getFecha());
        return log;
    }
}
