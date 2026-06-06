package org.acme.application.usecase;

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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetMarketingStrategiesUseCaseTest {

    private MarketingStrategyRepository repository;
    private AuthContext authContext;
    private MarketingStrategyDtoFactory dtoFactory;
    private GetMarketingStrategiesUseCase useCase;

    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        repository  = mock(MarketingStrategyRepository.class);
        authContext = mock(AuthContext.class);
        dtoFactory  = mock(MarketingStrategyDtoFactory.class);

        Role role = new Role((byte) 4, "DIRECTOR_MERCADOTECNIA");
        User user = new User(USER_ID, "Ana", "López", "ana@test.com", role, true, "firebase-uid");
        when(authContext.getUser()).thenReturn(user);

        useCase = new GetMarketingStrategiesUseCase();
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

    private MarketingStrategy buildStrategy(UUID id) {
        MarketingStrategy s = new MarketingStrategy(
                id, USER_ID, LocalDateTime.now(), null, "{\"resumen_ejecutivo\":\"Test\"}");
        return s;
    }

    private MarketingStrategyDto buildDto(UUID id) {
        MarketingStrategyDto dto = new MarketingStrategyDto();
        dto.setId(id);
        dto.setUsuarioId(USER_ID);
        dto.setEstado(MarketingStrategy.ESTADO_PROPUESTA);
        return dto;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void executeShouldReturnEmptyListWhenNoStrategiesExist() {
        when(repository.findByUsuarioId(USER_ID)).thenReturn(List.of());

        List<MarketingStrategyDto> result = useCase.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void executeShouldReturnMappedDtosForCurrentUser() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        MarketingStrategy s1 = buildStrategy(id1);
        MarketingStrategy s2 = buildStrategy(id2);
        MarketingStrategyDto dto1 = buildDto(id1);
        MarketingStrategyDto dto2 = buildDto(id2);

        when(repository.findByUsuarioId(USER_ID)).thenReturn(List.of(s1, s2));
        when(dtoFactory.from(s1)).thenReturn(dto1);
        when(dtoFactory.from(s2)).thenReturn(dto2);

        List<MarketingStrategyDto> result = useCase.execute();

        assertEquals(2, result.size());
        assertEquals(id1, result.get(0).getId());
        assertEquals(id2, result.get(1).getId());
    }

    @Test
    void executeShouldQueryUsingAuthenticatedUserId() {
        when(repository.findByUsuarioId(USER_ID)).thenReturn(List.of());

        useCase.execute();

        verify(repository, times(1)).findByUsuarioId(USER_ID);
    }

    @Test
    void executeShouldMapEachStrategyThroughDtoFactory() {
        MarketingStrategy s = buildStrategy(UUID.randomUUID());
        MarketingStrategyDto dto = buildDto(s.getId());

        when(repository.findByUsuarioId(USER_ID)).thenReturn(List.of(s));
        when(dtoFactory.from(s)).thenReturn(dto);

        useCase.execute();

        verify(dtoFactory, times(1)).from(s);
    }
}
