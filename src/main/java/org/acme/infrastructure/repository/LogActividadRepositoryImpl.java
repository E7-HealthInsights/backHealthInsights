package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.acme.domain.models.EntidadTipo;
import org.acme.domain.models.LogActividad;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.infrastructure.entities.LogActividadEntity;
import org.acme.infrastructure.mapper.LogActividadMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class LogActividadRepositoryImpl implements LogActividadRepository, PanacheRepositoryBase<LogActividadEntity, UUID> {

    @Inject
    EntityManager em;

    @Override
    public List<LogActividad> findAllLogs() {
        return em.createQuery(
                "SELECT new org.acme.domain.models.LogActividad(" +
                "   l.id, l.accion, l.detalle, l.entidadTipo, l.entidadId, l.fecha, " +
                "   CONCAT(CONCAT(u.name, ' '), u.lastName)" +
                ") " +
                "FROM LogActividadEntity l " +
                "LEFT JOIN l.usuario u " +
                "ORDER BY l.fecha DESC",
                LogActividad.class)
                .getResultList();
    }

    @Override
    public Optional<LogActividad> findLatestByEntidadId(String entidadId) {
        return em.createQuery(
                "SELECT l FROM LogActividadEntity l " +
                "WHERE l.entidadId = :entidadId " +
                "ORDER BY l.fecha DESC",
                LogActividadEntity.class)
                .setParameter("entidadId", entidadId)
                .setMaxResults(1)
                .getResultStream()
                .map(LogActividadMapper::toDomain)
                .findFirst();
    }

    @Override
    @Transactional
    public void updateDetalle(UUID logId, String detalle) {
        em.createQuery(
                "UPDATE LogActividadEntity l " +
                "SET l.detalle = :detalle " +
                "WHERE l.id = :id")
                .setParameter("detalle", detalle)
                .setParameter("id", logId)
                .executeUpdate();
    }
}
