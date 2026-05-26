package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.CreateReporteDto;
import org.acme.domain.models.Reporte;
import org.acme.domain.repository.ReporteRepository;
import org.acme.infrastructure.security.AuthContext;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class CreateReporteUseCase {

    private final ReporteRepository reporteRepository;
    private final AuthContext authContext;

    @Inject
    public CreateReporteUseCase(ReporteRepository reporteRepository, AuthContext authContext) {
        this.reporteRepository = reporteRepository;
        this.authContext = authContext;
    }

    public Reporte execute(CreateReporteDto dto) {
        Reporte reporte = new Reporte();
        reporte.setId(UUID.randomUUID());
        reporte.setUsuarioId(authContext.getUser().getId());
        reporte.setTitulo(dto.getTitulo());
        reporte.setTipo(dto.getTipo());
        reporte.setReferenciaId(dto.getReferenciaId());
        reporte.setFechaCreacion(LocalDateTime.now());
        return reporteRepository.save(reporte);
    }
}
