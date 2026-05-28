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
        widget.setRolId(entity.getRolId());
        widget.setTipoSemantico(entity.getTipoSemantico());
        widget.setNivelGeografico(entity.getNivelGeografico());

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
        entity.setTipo(widget.getTipo() != null ? TipoWidgetMapper.toEntity(widget.getTipo()) : null);
        entity.setUsuario(widget.getUsuario() != null ? UserMapper.toEntity(widget.getUsuario()) : null);
        entity.setRolId(widget.getRolId());
        entity.setTipoSemantico(widget.getTipoSemantico());
        entity.setNivelGeografico(widget.getNivelGeografico());
        return entity;
    }


}
