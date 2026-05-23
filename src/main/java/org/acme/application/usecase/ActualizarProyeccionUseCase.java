package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.GuardarProyeccionDto;
import org.acme.application.dto.ProyeccionResponseDto;
import org.acme.domain.exception.ProyeccionNotFoundException;
import org.acme.domain.exception.UnauthorizedException;
import org.acme.domain.models.Proyeccion;
import org.acme.domain.repository.ProyeccionRepository;
import org.acme.infrastructure.security.AuthContext;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class ActualizarProyeccionUseCase {

    @Inject ProyeccionRepository proyeccionRepository;
    @Inject AuthContext authContext;

    public ProyeccionResponseDto execute(UUID id, GuardarProyeccionDto dto) {
        // 1 — Busca la proyección
        Proyeccion proyeccion = proyeccionRepository.findProyeccionById(id)
                .orElseThrow(() -> new ProyeccionNotFoundException(id));

        // 2 — Verifica que el usuario autenticado es el dueño
        if (!proyeccion.getUsuario().getId()
                .equals(authContext.getUser().getId())) {
            throw new UnauthorizedException("No tienes permiso para editar esta proyección");
        }

        // 3 — Actualiza campos
        proyeccion.setTitulo(dto.getTitulo());
        proyeccion.setDescripcion(
            dto.getDescripcion() != null ? dto.getDescripcion() : dto.getTitulo()
        );
        proyeccion.setParametros(dto.getResultado());
        proyeccion.setFechaActualizacion(LocalDateTime.now());

        Proyeccion updated = proyeccionRepository.update(proyeccion);
        return toDto(updated);
    }

    private ProyeccionResponseDto toDto(Proyeccion p) {
        ProyeccionResponseDto dto = new ProyeccionResponseDto();
        dto.setId(p.getId());
        dto.setTitulo(p.getTitulo());
        dto.setDescripcion(p.getDescripcion());
        dto.setResultado(p.getParametros());
        dto.setFechaCreacion(p.getFechaCreacion());
        return dto;
    }
}