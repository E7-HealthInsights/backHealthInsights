package org.acme.domain.repository;

import org.acme.domain.models.LogActividad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LogActividadRepository {
    List<LogActividad> findAllLogs();
    Optional<LogActividad> findLatestByEntidadId(String entidadId);
    void updateDetalle(UUID logid, String detalle);
}
