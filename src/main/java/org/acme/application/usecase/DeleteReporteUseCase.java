package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exception.ReporteNotFoundException;
import org.acme.domain.exception.UnauthorizedException;
import org.acme.domain.models.Reporte;
import org.acme.domain.repository.ReporteRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.UUID;

@ApplicationScoped
public class DeleteReporteUseCase {

    private final ReporteRepository reporteRepository;
    private final AuthContext authContext;

    @Inject
    public DeleteReporteUseCase(ReporteRepository reporteRepository, AuthContext authContext) {
        this.reporteRepository = reporteRepository;
        this.authContext = authContext;
    }

    public void execute(UUID id) {
        Reporte reporte = reporteRepository.findReporteById(id)
                .orElseThrow(() -> new ReporteNotFoundException(id));

        if (!reporte.getUsuarioId().equals(authContext.getUser().getId())) {
            throw new UnauthorizedException("No tienes permiso para eliminar este reporte");
        }

        reporteRepository.eliminarReporte(id);
    }
}
