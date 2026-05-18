package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Proyeccion;
import org.acme.domain.repository.ProyeccionRepository;
import org.acme.infrastructure.entities.ProyeccionEntity;
import org.acme.infrastructure.entities.UserEntity;
import org.acme.infrastructure.mapper.ProyeccionMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProyeccionRepositoryImpl
        implements ProyeccionRepository, PanacheRepositoryBase<ProyeccionEntity, UUID> {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public Proyeccion save(Proyeccion proyeccion) {
        ProyeccionEntity entity = ProyeccionMapper.toEntity(proyeccion);
        entity.setUsuario(em.getReference(UserEntity.class, proyeccion.getUsuario().getId()));
        persist(entity);
        return ProyeccionMapper.toDomain(entity);
    }

    @Override
    public List<Proyeccion> findByUsuarioId(UUID usuarioId) {
        return em.createQuery(
                "SELECT p FROM ProyeccionEntity p WHERE p.usuario.id = :uid " +
                "ORDER BY p.fechaCreacion DESC",
                ProyeccionEntity.class)
                .setParameter("uid", usuarioId)
                .getResultList()
                .stream()
                .map(ProyeccionMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        deleteById(id);
    }
}