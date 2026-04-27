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
        return find("usuario.id", userId)
                .withHint("jakarta.persistence.fetchgraph", em.getEntityGraph("Widget.withTipo"))
                .list().stream().map(WidgetMapper::toDomain).collect(Collectors.toList());
    }

}
