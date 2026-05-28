package org.acme.domain.repository;

import org.acme.domain.models.Proyeccion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyeccionRepository {
    Proyeccion save(Proyeccion proyeccion);
    List<Proyeccion> findByUsuarioId(UUID usuarioId);
    Optional<Proyeccion> findProyeccionById(UUID id);  
    Proyeccion update(Proyeccion proyeccion);
    void delete(UUID id);
}