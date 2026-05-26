package org.acme.infrastructure.mapper;

import org.acme.domain.models.Reporte;
import org.acme.infrastructure.entities.ReporteEntity;

import java.util.UUID;

public class ReporteMapper {

    public static Reporte toDomain(ReporteEntity entity) {
        Reporte r = new Reporte();
        r.setId(entity.getId());
        r.setUsuarioId(UUID.fromString(entity.getUsuarioId()));
        r.setTitulo(entity.getTitulo());
        r.setTipo(entity.getTipo());
        r.setReferenciaId(entity.getReferenciaId());
        r.setFechaCreacion(entity.getFechaCreacion());
        return r;
    }

    public static ReporteEntity toEntity(Reporte reporte) {
        ReporteEntity e = new ReporteEntity();
        e.setId(reporte.getId());
        e.setUsuarioId(reporte.getUsuarioId().toString());
        e.setTitulo(reporte.getTitulo());
        e.setTipo(reporte.getTipo());
        e.setReferenciaId(reporte.getReferenciaId());
        e.setFechaCreacion(reporte.getFechaCreacion());
        return e;
    }
}
