package org.acme.infrastructure.mapper;

import org.acme.domain.models.MarketingStrategy;
import org.acme.infrastructure.entities.MarketingStrategyEntity;

public class MarketingStrategyMapper {

    public static MarketingStrategy toDomain(MarketingStrategyEntity entity) {
        MarketingStrategy domain = new MarketingStrategy(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getCreadoEn(),
                entity.getContextoExtra(),
                entity.getPayloadJson()
        );
        domain.setEstado(entity.getEstado() != null ? entity.getEstado() : MarketingStrategy.ESTADO_PROPUESTA);
        domain.setNotaResultado(entity.getNotaResultado());
        domain.setFechaRevision(entity.getFechaRevision());
        domain.setComentariosJson(entity.getComentariosJson());
        return domain;
    }

    public static MarketingStrategyEntity toEntity(MarketingStrategy domain) {
        MarketingStrategyEntity entity = new MarketingStrategyEntity();
        entity.setId(domain.getId());
        entity.setUsuarioId(domain.getUsuarioId());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setContextoExtra(domain.getContextoExtra());
        entity.setPayloadJson(domain.getPayloadJson());
        entity.setEstado(domain.getEstado() != null ? domain.getEstado() : MarketingStrategy.ESTADO_PROPUESTA);
        entity.setNotaResultado(domain.getNotaResultado());
        entity.setFechaRevision(domain.getFechaRevision());
        entity.setComentariosJson(domain.getComentariosJson());
        return entity;
    }
}
