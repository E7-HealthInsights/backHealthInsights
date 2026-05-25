package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.acme.application.dto.PaginadoResponseDto;
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

    public PaginadoResponseDto<LogActividad> execute(int page, int size, String search) {
        List<LogActividad> data = logActividadRepository.findPaginated(page, size, search);
        long total = logActividadRepository.countAll(search);
        return new PaginadoResponseDto<>(data, total, page, size);
    }
}
