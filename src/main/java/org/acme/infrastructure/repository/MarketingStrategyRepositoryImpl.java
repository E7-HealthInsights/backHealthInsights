package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.MarketingStrategy;
import org.acme.domain.repository.MarketingStrategyRepository;
import org.acme.infrastructure.entities.MarketingStrategyEntity;
import org.acme.infrastructure.mapper.MarketingStrategyMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MarketingStrategyRepositoryImpl
        implements MarketingStrategyRepository,
                   PanacheRepositoryBase<MarketingStrategyEntity, UUID> {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public MarketingStrategy create(MarketingStrategy strategy) {
        MarketingStrategyEntity entity = MarketingStrategyMapper.toEntity(strategy);
        persist(entity);
        return MarketingStrategyMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public MarketingStrategy update(MarketingStrategy strategy) {
        MarketingStrategyEntity entity = findById(strategy.getId());
        if (entity == null) {
            throw new IllegalStateException("Estrategia no encontrada para actualizar: " + strategy.getId());
        }
        entity.setEstado(strategy.getEstado());
        entity.setNotaResultado(strategy.getNotaResultado());
        entity.setFechaRevision(strategy.getFechaRevision());
        entity.setComentariosJson(strategy.getComentariosJson());
        // payloadJson, contextoExtra, usuarioId y creadoEn no se modifican
        return MarketingStrategyMapper.toDomain(entity);
    }

    @Override
    public List<MarketingStrategy> findByUsuarioId(UUID usuarioId) {
        List<MarketingStrategyEntity> entities = em.createQuery(
                        "SELECT s FROM MarketingStrategyEntity s " +
                        "WHERE s.usuarioId = :usuarioId " +
                        "ORDER BY s.creadoEn DESC",
                        MarketingStrategyEntity.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
        return entities.stream().map(MarketingStrategyMapper::toDomain).toList();
    }

    @Override
    public Optional<MarketingStrategy> findOneById(UUID id) {
        return findByIdOptional(id).map(MarketingStrategyMapper::toDomain);
    }
}
