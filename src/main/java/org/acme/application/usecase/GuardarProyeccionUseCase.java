package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.GuardarProyeccionDto;
import org.acme.application.dto.ProyeccionResponseDto;
import org.acme.domain.models.Proyeccion;
import org.acme.domain.repository.ProyeccionRepository;
import org.acme.infrastructure.security.AuthContext;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class GuardarProyeccionUseCase {

    @Inject ProyeccionRepository proyeccionRepository;
    @Inject AuthContext authContext;

    public GuardarProyeccionUseCase(ProyeccionRepository proyeccionRepository, AuthContext authContext) {
        this.proyeccionRepository = proyeccionRepository;
        this.authContext = authContext;
    }

    public ProyeccionResponseDto execute(GuardarProyeccionDto dto) {
        Proyeccion proyeccion = new Proyeccion();
        proyeccion.setId(UUID.randomUUID());
        proyeccion.setTitulo(dto.getTitulo());
        proyeccion.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion() : dto.getTitulo());
        proyeccion.setUsuario(authContext.getUser());
        proyeccion.setParametros(dto.getResultado()); // JSON completo
        proyeccion.setFechaCreacion(LocalDateTime.now());
        proyeccion.setFechaActualizacion(LocalDateTime.now());

        Proyeccion saved = proyeccionRepository.save(proyeccion);
        return toDto(saved);
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