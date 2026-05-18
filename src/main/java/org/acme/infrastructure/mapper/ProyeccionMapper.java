package org.acme.infrastructure.mapper;

import org.acme.domain.models.Proyeccion;
import org.acme.infrastructure.entities.ProyeccionEntity;
import org.hibernate.Hibernate;

public class ProyeccionMapper {

    public static Proyeccion toDomain(ProyeccionEntity entity) {
        Proyeccion p = new Proyeccion();
        p.setId(entity.getId());
        p.setTitulo(entity.getTitulo());
        p.setDescripcion(entity.getDescripcion());
        p.setParametros(entity.getQuery());
        p.setFechaCreacion(entity.getFechaCreacion());
        p.setFechaActualizacion(entity.getFechaActualizacion());

        if (entity.getUsuario() != null && Hibernate.isInitialized(entity.getUsuario())) {
            p.setUsuario(UserMapper.toDomain(entity.getUsuario()));
        }
        return p;
    }

    public static ProyeccionEntity toEntity(Proyeccion proyeccion) {
        ProyeccionEntity entity = new ProyeccionEntity();
        entity.setId(proyeccion.getId());
        entity.setTitulo(proyeccion.getTitulo());
        entity.setDescripcion(proyeccion.getDescripcion());
        entity.setQuery(proyeccion.getParametros());
        entity.setFechaCreacion(proyeccion.getFechaCreacion());
        entity.setFechaActualizacion(proyeccion.getFechaActualizacion());
        return entity;
    }
}