package org.acme.application.usecase;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.acme.application.dto.MarketingStrategyDto;
import org.acme.application.dto.MarketingStrategyDtoFactory;
import org.acme.application.dto.UpdateStrategyStateDto;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateMarketingStrategyStateUseCaseTest {

    private MarketingStrategyRepository repository;
    private AuthContext authContext;
    private MarketingStrategyDtoFactory dtoFactory;
    private UpdateMarketingStrategyStateUseCase useCase;

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
        when(repository.update(any(MarketingStrategy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dtoFactory.from(any(MarketingStrategy.class))).thenReturn(new MarketingStrategyDto());

        useCase = new UpdateMarketingStrategyStateUseCase();
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

    private UpdateStrategyStateDto stateDto(String estado, String nota) {
        UpdateStrategyStateDto dto = new UpdateStrategyStateDto();
        dto.setEstado(estado);
        dto.setNotaResultado(nota);
        return dto;
    }

    // ── Tests: validación de input ────────────────────────────────────────────

    @Test
    void executeShouldThrowBadRequestWhenRequestIsNull() {
        assertThrows(BadRequestException.class,
                () -> useCase.execute(STRATEGY_ID, null));
    }

    @Test
    void executeShouldThrowBadRequestWhenEstadoIsNull() {
        assertThrows(BadRequestException.class,
                () -> useCase.execute(STRATEGY_ID, stateDto(null, null)));
    }

    @Test
    void executeShouldThrowBadRequestWhenEstadoIsInvalid() {
        assertThrows(BadRequestException.class,
                () -> useCase.execute(STRATEGY_ID, stateDto("invalido", null)));
    }

    // ── Tests: estrategia no encontrada / forbidden ───────────────────────────

    @Test
    void executeShouldThrowNotFoundWhenStrategyDoesNotExist() {
        UUID unknown = UUID.randomUUID();
        when(repository.findOneById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> useCase.execute(unknown, stateDto(MarketingStrategy.ESTADO_EJECUTADA, null)));
    }

    @Test
    void executeShouldThrowForbiddenWhenStrategyBelongsToAnotherUser() {
        strategy.setUsuarioId(OTHER_ID);

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, null)));
    }

    @Test
    void executeShouldNotPersistWhenForbidden() {
        strategy.setUsuarioId(OTHER_ID);

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, null)));
        verify(repository, never()).update(any());
    }

    // ── Tests: caso feliz — todos los estados válidos ─────────────────────────

    @Test
    void executeShouldAcceptEstadoPropuesta() {
        assertDoesNotThrow(() ->
                useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_PROPUESTA, null)));
    }

    @Test
    void executeShouldAcceptEstadoEjecutada() {
        assertDoesNotThrow(() ->
                useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, null)));
    }

    @Test
    void executeShouldAcceptEstadoDescartada() {
        assertDoesNotThrow(() ->
                useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_DESCARTADA, null)));
    }

    @Test
    void executeShouldUpdateEstadoOnStrategy() {
        useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, null));

        assertEquals(MarketingStrategy.ESTADO_EJECUTADA, strategy.getEstado());
    }

    @Test
    void executeShouldSetNotaResultadoWhenProvided() {
        useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, "Excelentes resultados en Q1"));

        assertEquals("Excelentes resultados en Q1", strategy.getNotaResultado());
    }

    @Test
    void executeShouldSetNotaResultadoToNullWhenBlank() {
        // emptyToNull: string en blanco debe quedar como null
        useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, "   "));

        assertNull(strategy.getNotaResultado());
    }

    @Test
    void executeShouldSetNotaResultadoToNullWhenNotProvided() {
        useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, null));

        assertNull(strategy.getNotaResultado());
    }

    @Test
    void executeShouldSetFechaRevision() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, null));

        assertNotNull(strategy.getFechaRevision());
        assertTrue(strategy.getFechaRevision().isAfter(before));
    }

    @Test
    void executeShouldCallUpdateOnRepository() {
        useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, null));

        verify(repository, times(1)).update(strategy);
    }

    @Test
    void executeShouldDelegateMappingToDtoFactory() {
        useCase.execute(STRATEGY_ID, stateDto(MarketingStrategy.ESTADO_EJECUTADA, null));

        verify(dtoFactory, times(1)).from(any(MarketingStrategy.class));
    }
}
