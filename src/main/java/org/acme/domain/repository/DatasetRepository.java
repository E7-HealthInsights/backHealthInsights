package org.acme.domain.repository;

import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepository {
    List<Dataset> findAllDatasets();
    Optional<Dataset> findDatasetById(UUID id);
    Dataset save(Dataset dataset);
    Dataset update(Dataset dataset);
    void deactivate(UUID id, String modifiedBy);
    void reactivate(UUID id, String modifiedBy);
    boolean existsByNombreTabla(String nombreTabla);
    Optional<Dataset> findByNombreTabla(String nombreTabla);
}
