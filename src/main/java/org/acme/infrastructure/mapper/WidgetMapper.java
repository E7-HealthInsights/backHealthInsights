package org.acme.infrastructure.mapper;

import org.acme.domain.models.Widget;
import org.acme.infrastructure.entities.WidgetEntity;
import org.hibernate.Hibernate;

public class WidgetMapper {

    public static Widget toDomain(WidgetEntity entity){
        Widget widget = new Widget();
        widget.setId(entity.getId());
        widget.setTitulo(entity.getTitulo());
        widget.setQuery(entity.getQuery());
        widget.setOrden(entity.getOrden());

        if(entity.getTipo() != null && Hibernate.isInitialized(entity.getTipo())){
            widget.setTipo(TipoWidgetMapper.toDomain(entity.getTipo()));
        }

        if(entity.getUsuario() != null && Hibernate.isInitialized(entity.getUsuario())){
            widget.setUsuario(UserMapper.toDomain(entity.getUsuario()));
        }
        return widget;
    }

    public static WidgetEntity toEntity(Widget widget) {
        WidgetEntity entity = new WidgetEntity();
        entity.setId(widget.getId());
        entity.setTitulo(widget.getTitulo());
        entity.setQuery(widget.getQuery());
        entity.setOrden(widget.getOrden());

        if (widget.getUsuario() != null) {
            entity.setUsuario(UserMapper.toEntity(widget.getUsuario()));
        }

        if (widget.getTipo() != null) {
            entity.setTipo(TipoWidgetMapper.toEntity(widget.getTipo()));
        }

        return entity;
    }


}
