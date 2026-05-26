package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Reporte;
import org.acme.domain.repository.ReporteRepository;
import org.acme.infrastructure.entities.ReporteEntity;
import org.acme.infrastructure.mapper.ReporteMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ReporteRepositoryImpl implements ReporteRepository, PanacheRepositoryBase<ReporteEntity, UUID> {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public Reporte save(Reporte reporte) {
        ReporteEntity entity = ReporteMapper.toEntity(reporte);
        persist(entity);
        return ReporteMapper.toDomain(entity);
    }

    @Override
    public List<Reporte> findByUsuarioId(UUID usuarioId) {
        return em.createQuery(
                "SELECT r FROM ReporteEntity r WHERE r.usuarioId = :uid ORDER BY r.fechaCreacion DESC",
                ReporteEntity.class)
                .setParameter("uid", usuarioId.toString())
                .getResultList()
                .stream()
                .map(ReporteMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Reporte> findReporteById(UUID id) {
        return findByIdOptional(id).map(ReporteMapper::toDomain);
    }

    @Override
    @Transactional
    public void eliminarReporte(UUID id) {
        deleteById(id);
    }
}
