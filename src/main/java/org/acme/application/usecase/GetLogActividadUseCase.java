package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.models.LogActividad;
import org.acme.domain.repository.LogActividadRepository;

import java.util.List;

@ApplicationScoped
public class GetLogActividadUseCase {

    private final LogActividadRepository logActividadRepository;

    @Inject
    public GetLogActividadUseCase(LogActividadRepository logActividadRepository) {
        this.logActividadRepository = logActividadRepository;
    }

    public List<LogActividad> execute() {
        return logActividadRepository.findAllLogs();
    }
}
