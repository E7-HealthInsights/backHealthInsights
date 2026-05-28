package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.ProyeccionResponseDto;
import org.acme.domain.repository.ProyeccionRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.List;

@ApplicationScoped
public class GetProyeccionesUseCase {

    @Inject ProyeccionRepository proyeccionRepository;
    @Inject AuthContext authContext;

    public List<ProyeccionResponseDto> execute() {
        return proyeccionRepository
                .findByUsuarioId(authContext.getUser().getId())
                .stream()
                .map(p -> {
                    ProyeccionResponseDto dto = new ProyeccionResponseDto();
                    dto.setId(p.getId());
                    dto.setTitulo(p.getTitulo());
                    dto.setDescripcion(p.getDescripcion());
                    dto.setResultado(p.getParametros());
                    dto.setFechaCreacion(p.getFechaCreacion());
                    return dto;
                })
                .toList();
    }
}