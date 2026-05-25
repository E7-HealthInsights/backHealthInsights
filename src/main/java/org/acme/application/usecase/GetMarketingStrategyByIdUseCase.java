package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.acme.application.dto.MarketingStrategyDto;
import org.acme.application.dto.MarketingStrategyDtoFactory;
import org.acme.domain.models.MarketingStrategy;
import org.acme.domain.repository.MarketingStrategyRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.UUID;

@ApplicationScoped
public class GetMarketingStrategyByIdUseCase {

    @Inject MarketingStrategyRepository repository;
    @Inject AuthContext authContext;
    @Inject MarketingStrategyDtoFactory dtoFactory;

    public MarketingStrategyDto execute(UUID id) {
        MarketingStrategy strategy = repository.findOneById(id)
                .orElseThrow(() -> new NotFoundException("Estrategia no encontrada."));

        UUID currentUser = authContext.getUser().getId();
        if (!strategy.getUsuarioId().equals(currentUser)) {
            throw new ForbiddenException("Esta estrategia pertenece a otro usuario.");
        }

        return dtoFactory.from(strategy);
    }
}
