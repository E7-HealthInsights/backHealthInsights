package org.acme.domain.repository;

import org.acme.domain.models.Reporte;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReporteRepository {
    Reporte save(Reporte reporte);
    List<Reporte> findByUsuarioId(UUID usuarioId);
    Optional<Reporte> findReporteById(UUID id);
    void eliminarReporte(UUID id);
}
