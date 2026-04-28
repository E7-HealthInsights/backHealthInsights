package org.acme.infrastructure.mapper;

import org.acme.domain.models.TipoWidget;
import org.acme.infrastructure.entities.TipoWidgetEntity;

public class TipoWidgetMapper {

    public static TipoWidget toDomain(TipoWidgetEntity entity){
        TipoWidget tipo = new TipoWidget();
        tipo.setId(entity.getId());
        tipo.setNombre(entity.getNombre());
        return tipo;
    }

    public static TipoWidgetEntity toEntity(TipoWidget tipoWidget){
        TipoWidgetEntity entity = new TipoWidgetEntity();
        entity.setId(tipoWidget.getId());
        entity.setNombre(tipoWidget.getNombre());
        return entity;
    }
}
