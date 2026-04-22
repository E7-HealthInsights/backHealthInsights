package org.acme.domain.repository;

import org.acme.domain.models.Dataset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepository {
    List<Dataset> findAllActive();
    Optional<Dataset> findDatasetById(UUID id);
}