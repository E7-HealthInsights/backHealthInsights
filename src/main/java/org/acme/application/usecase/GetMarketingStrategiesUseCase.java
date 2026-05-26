package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.MarketingStrategyDto;
import org.acme.application.dto.MarketingStrategyDtoFactory;
import org.acme.domain.repository.MarketingStrategyRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.List;

@ApplicationScoped
public class GetMarketingStrategiesUseCase {

    @Inject MarketingStrategyRepository repository;
    @Inject AuthContext authContext;
    @Inject MarketingStrategyDtoFactory dtoFactory;

    public List<MarketingStrategyDto> execute() {
        return repository.findByUsuarioId(authContext.getUser().getId())
                .stream()
                .map(dtoFactory::from)
                .toList();
    }
}
