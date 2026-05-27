package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Widget;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.entities.WidgetEntity;
import org.acme.infrastructure.mapper.WidgetMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class WidgetRepositoryImpl implements WidgetRepository, PanacheRepositoryBase<WidgetEntity, UUID> {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public Widget create(Widget widget){
        WidgetEntity entity = WidgetMapper.toEntity(widget);
        persist(entity);
        return WidgetMapper.toDomain(entity);
    }

    @Override
    public List<Widget> findByUserId(UUID userId){
        List<WidgetEntity> entities = em.createQuery(
                        "SELECT w FROM WidgetEntity w WHERE w.usuario.id = :userId",
                        WidgetEntity.class
                )
                .setParameter("userId", userId)
                .setHint("jakarta.persistence.fetchgraph", em.getEntityGraph("Widget.withTipo"))
                .getResultList();
        return entities.stream().map(WidgetMapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Widget> findDefaultsByRolId(Byte rolId) {
        List<WidgetEntity> entities = em.createQuery(
                        "SELECT w FROM WidgetEntity w WHERE w.rolId = :rolId",
                        WidgetEntity.class
                )
                .setParameter("rolId", rolId)
                .setHint("jakarta.persistence.fetchgraph", em.getEntityGraph("Widget.withTipo"))
                .getResultList();
        return entities.stream().map(WidgetMapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void updateOrden(UUID widgetId, int orden) {
        em.createQuery("UPDATE WidgetEntity w SET w.orden = :orden WHERE w.id = :id")
                .setParameter("orden", orden)
                .setParameter("id", widgetId)
                .executeUpdate();
    }

}
