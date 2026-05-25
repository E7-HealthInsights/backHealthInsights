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

    @Override
    public List<LogActividad> findPaginated(int page, int size, String search) {
        int offset = (page - 1) * size;
        String like = search != null && !search.isBlank()
            ? "%" + search.toLowerCase() + "%"
            : null;

        var query = em.createNativeQuery(
                "SELECT l.id, l.accion, l.detalle, l.entidad_tipo, " +
                "l.entidad_id, l.fecha, " +
                "CONCAT(u.name, ' ', u.last_name) AS admin_nombre " +
                "FROM LogActividad l " +
                "LEFT JOIN Users u ON u.id = l.usuario_id " +
                (like != null ? "WHERE LOWER(l.accion) LIKE :like OR LOWER(l.detalle) LIKE :like OR LOWER(CONCAT(u.name, ' ', u.last_name)) LIKE :like " : "") +
                "ORDER BY l.fecha DESC " +
                "LIMIT :size OFFSET :offset"
        )
        .setParameter("size", size)
        .setParameter("offset", offset);

        if (like != null) {
            query.setParameter("like", like);
        }

        List<Object[]> rows = query.getResultList();

        return rows.stream().map(row -> {
            LogActividad log = new LogActividad();
            log.setId(UUID.fromString((String) row[0]));
            log.setAccion((String) row[1]);
            log.setDetalle((String) row[2]);
            log.setEntidadTipo(EntidadTipo.valueOf((String) row[3]));
            log.setEntidadId((String) row[4]);
            Object fechaRaw = row[5];
            log.setFecha(fechaRaw instanceof java.sql.Timestamp ts
                    ? ts.toLocalDateTime()
                    : (LocalDateTime) fechaRaw);
            log.setAdminNombre((String) row[6]);
            return log;
        }).toList();
    }

    @Override
    public long countAll(String search) {
        String like = search != null && !search.isBlank()
                ? "%" + search.toLowerCase() + "%"
                : null;

        String sql =
            "SELECT COUNT(*) FROM LogActividad l " +
            "LEFT JOIN Users u ON u.id = l.usuario_id " +
            (like != null ?
            "WHERE LOWER(l.accion) LIKE :like " +
            "OR LOWER(l.detalle) LIKE :like " +
            "OR LOWER(CONCAT(u.name, ' ', u.last_name)) LIKE :like " : "");

        var query = em.createNativeQuery(sql);
        if (like != null) query.setParameter("like", like);

        return ((Number) query.getSingleResult()).longValue();
    }
}
