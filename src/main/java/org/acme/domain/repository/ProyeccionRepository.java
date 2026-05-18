package org.acme.domain.repository;

import org.acme.domain.models.Proyeccion;
import java.util.List;
import java.util.UUID;

public interface ProyeccionRepository {
    Proyeccion save(Proyeccion proyeccion);
    List<Proyeccion> findByUsuarioId(UUID usuarioId);
    void delete(UUID id);
}