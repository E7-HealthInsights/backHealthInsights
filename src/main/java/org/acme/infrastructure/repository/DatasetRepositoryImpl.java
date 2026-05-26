package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.repository.DatasetRepository;
import org.acme.infrastructure.entities.DatasetEntity;
import org.acme.infrastructure.mapper.DatasetMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class DatasetRepositoryImpl implements DatasetRepository, PanacheRepositoryBase<DatasetEntity, UUID> {

    /**
     * Devuelve datasets en estado READY (visibles para el usuario final).
     * Los que están en PENDING, PROCESSING o ERROR no se exponen en el listado.
     */
    @Override
    public List<Dataset> findAllActive() {
        return find("estado", DatasetEstado.READY)
                .list()
                .stream()
                .map(DatasetMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Dataset> findDatasetById(UUID id) {
        return findByIdOptional(id).map(DatasetMapper::toDomain);
    }

    @Override
    public Dataset save(Dataset dataset) {
        DatasetEntity entity = DatasetMapper.toEntity(dataset);
        persist(entity);
        return DatasetMapper.toDomain(entity);
    }

    /**
     * Actualiza un Dataset existente (usado por el consumer para cambiar estado).
     */
    @Override
    public Dataset update(Dataset dataset) {
        DatasetEntity entity = findById(dataset.getId());
        if (entity == null) {
            throw new jakarta.ws.rs.NotFoundException("Dataset no encontrado: " + dataset.getId());
        }
        entity.setEstado(dataset.getEstado());
        entity.setErrorMensaje(dataset.getErrorMensaje());
        entity.setFechaActualizacion(dataset.getFechaActualizacion());
        return DatasetMapper.toDomain(entity);
    }

    @Override
    public boolean existsByNombreTabla(String nombreTabla) {
        return count("nombreTabla", nombreTabla) > 0;
    }

    @Override
    public Optional<Dataset> findByNombreTabla(String nombreTabla) {
        return find("nombreTabla", nombreTabla)
                .firstResultOptional()
                .map(DatasetMapper::toDomain);
    }
}
