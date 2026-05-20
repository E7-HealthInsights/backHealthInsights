package org.acme.domain.repository;

import org.acme.domain.models.LogActividad;

import java.util.List;

public interface LogActividadRepository {
    List<LogActividad> findAllLogs();
}
