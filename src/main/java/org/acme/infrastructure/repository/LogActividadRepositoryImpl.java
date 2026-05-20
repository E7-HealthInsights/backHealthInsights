package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.acme.domain.models.LogActividad;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.infrastructure.entities.LogActividadEntity;
import org.acme.infrastructure.mapper.LogActividadMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class LogActividadRepositoryImpl implements LogActividadRepository, PanacheRepositoryBase<LogActividadEntity, UUID> {

    @Inject
    EntityManager em;

    @Override
    public List<LogActividad> findAllLogs() {
        return em.createQuery("SELECT l FROM LogActividadEntity l ORDER BY l.fecha DESC", LogActividadEntity.class)
                .getResultList()
                .stream()
                .map(LogActividadMapper::toDomain)
                .collect(Collectors.toList());
    }
}
