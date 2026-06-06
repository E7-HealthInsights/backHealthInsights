package org.acme.application.usecase;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.acme.application.dto.MarketingStrategyDto;
import org.acme.application.dto.MarketingStrategyDtoFactory;
import org.acme.domain.models.MarketingStrategy;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.MarketingStrategyRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetMarketingStrategyByIdUseCaseTest {

    private MarketingStrategyRepository repository;
    private AuthContext authContext;
    private MarketingStrategyDtoFactory dtoFactory;
    private GetMarketingStrategyByIdUseCase useCase;

    private final UUID USER_ID     = UUID.randomUUID();
    private final UUID OTHER_ID    = UUID.randomUUID();
    private final UUID STRATEGY_ID = UUID.randomUUID();

    private MarketingStrategy strategy;

    @BeforeEach
    void setUp() throws Exception {
        repository  = mock(MarketingStrategyRepository.class);
        authContext = mock(AuthContext.class);
        dtoFactory  = mock(MarketingStrategyDtoFactory.class);

        Role role = new Role((byte) 4, "DIRECTOR_MERCADOTECNIA");
        User user = new User(USER_ID, "Ana", "López", "ana@test.com", role, true, "firebase-uid");
        when(authContext.getUser()).thenReturn(user);

        strategy = new MarketingStrategy(
                STRATEGY_ID, USER_ID, LocalDateTime.now(), null, "{\"resumen_ejecutivo\":\"Test\"}");

        when(repository.findOneById(STRATEGY_ID)).thenReturn(Optional.of(strategy));

        useCase = new GetMarketingStrategyByIdUseCase();
        setField(useCase, "repository",  repository);
        setField(useCase, "authContext",  authContext);
        setField(useCase, "dtoFactory",   dtoFactory);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void executeShouldReturnDtoWhenStrategyBelongsToCurrentUser() {
        MarketingStrategyDto expectedDto = new MarketingStrategyDto();
        expectedDto.setId(STRATEGY_ID);
        when(dtoFactory.from(strategy)).thenReturn(expectedDto);

        MarketingStrategyDto result = useCase.execute(STRATEGY_ID);

        assertNotNull(result);
        assertEquals(STRATEGY_ID, result.getId());
    }

    @Test
    void executeShouldThrowNotFoundWhenStrategyDoesNotExist() {
        UUID unknown = UUID.randomUUID();
        when(repository.findOneById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(unknown));
    }

    @Test
    void executeShouldThrowForbiddenWhenStrategyBelongsToAnotherUser() {
        strategy.setUsuarioId(OTHER_ID);

        assertThrows(ForbiddenException.class, () -> useCase.execute(STRATEGY_ID));
    }

    @Test
    void executeShouldNotCallDtoFactoryWhenStrategyNotFound() {
        UUID unknown = UUID.randomUUID();
        when(repository.findOneById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(unknown));
        verify(dtoFactory, never()).from(any());
    }

    @Test
    void executeShouldNotCallDtoFactoryWhenForbidden() {
        strategy.setUsuarioId(OTHER_ID);

        assertThrows(ForbiddenException.class, () -> useCase.execute(STRATEGY_ID));
        verify(dtoFactory, never()).from(any());
    }

    @Test
    void executeShouldDelegateMappingToDtoFactory() {
        MarketingStrategyDto dto = new MarketingStrategyDto();
        when(dtoFactory.from(strategy)).thenReturn(dto);

        useCase.execute(STRATEGY_ID);

        verify(dtoFactory, times(1)).from(strategy);
    }
}
