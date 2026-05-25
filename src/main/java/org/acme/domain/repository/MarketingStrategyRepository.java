package org.acme.domain.repository;

import org.acme.domain.models.MarketingStrategy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketingStrategyRepository {
    MarketingStrategy create(MarketingStrategy strategy);
    MarketingStrategy update(MarketingStrategy strategy);
    List<MarketingStrategy> findByUsuarioId(UUID usuarioId);
    Optional<MarketingStrategy> findOneById(UUID id);
}
