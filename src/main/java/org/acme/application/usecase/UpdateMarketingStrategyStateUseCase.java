package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.acme.application.dto.MarketingStrategyDto;
import org.acme.application.dto.MarketingStrategyDtoFactory;
import org.acme.application.dto.UpdateStrategyStateDto;
import org.acme.domain.models.MarketingStrategy;
import org.acme.domain.repository.MarketingStrategyRepository;
import org.acme.infrastructure.security.AuthContext;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class UpdateMarketingStrategyStateUseCase {

    @Inject MarketingStrategyRepository repository;
    @Inject AuthContext authContext;
    @Inject MarketingStrategyDtoFactory dtoFactory;

    public MarketingStrategyDto execute(UUID strategyId, UpdateStrategyStateDto request) {
        if (request == null || request.getEstado() == null) {
            throw new BadRequestException("estado es obligatorio.");
        }
        if (!MarketingStrategy.isEstadoValido(request.getEstado())) {
            throw new BadRequestException("Estado inválido: " + request.getEstado());
        }

        MarketingStrategy strategy = repository.findOneById(strategyId)
                .orElseThrow(() -> new NotFoundException("Estrategia no encontrada."));

        UUID currentUser = authContext.getUser().getId();
        if (!strategy.getUsuarioId().equals(currentUser)) {
            throw new ForbiddenException("Esta estrategia pertenece a otro usuario.");
        }

        strategy.setEstado(request.getEstado());
        strategy.setNotaResultado(emptyToNull(request.getNotaResultado()));
        strategy.setFechaRevision(LocalDateTime.now());

        MarketingStrategy updated = repository.update(strategy);
        return dtoFactory.from(updated);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
