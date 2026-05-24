package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exception.ProyeccionNotFoundException;
import org.acme.domain.exception.UnauthorizedException;
import org.acme.domain.models.Proyeccion;
import org.acme.domain.repository.ProyeccionRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.UUID;

@ApplicationScoped
public class EliminarProyeccionUseCase {

    @Inject ProyeccionRepository proyeccionRepository;
    @Inject AuthContext authContext;

    public void execute(UUID id) {
        // 1 — Busca la proyección
        Proyeccion proyeccion = proyeccionRepository.findProyeccionById(id)
                .orElseThrow(() -> new ProyeccionNotFoundException(id));

        // 2 — Verifica que el usuario autenticado es el dueño
        if (!proyeccion.getUsuario().getId()
                .equals(authContext.getUser().getId())) {
            throw new UnauthorizedException("No tienes permiso para eliminar esta proyección");
        }

        // 3 — Elimina
        proyeccionRepository.delete(id);
    }
}