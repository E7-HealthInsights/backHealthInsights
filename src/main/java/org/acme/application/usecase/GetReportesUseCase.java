package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.models.Reporte;
import org.acme.domain.repository.ReporteRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.List;

@ApplicationScoped
public class GetReportesUseCase {

    private final ReporteRepository reporteRepository;
    private final AuthContext authContext;

    @Inject
    public GetReportesUseCase(ReporteRepository reporteRepository, AuthContext authContext) {
        this.reporteRepository = reporteRepository;
        this.authContext = authContext;
    }

    public List<Reporte> execute() {
        return reporteRepository.findByUsuarioId(authContext.getUser().getId());
    }
}
