package org.acme.application.usecase;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.acme.application.dto.AddCommentDto;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddMarketingStrategyCommentUseCaseTest {

    private MarketingStrategyRepository repository;
    private AuthContext authContext;
    private MarketingStrategyDtoFactory dtoFactory;
    private AddMarketingStrategyCommentUseCase useCase;

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

        useCase = new AddMarketingStrategyCommentUseCase();
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

    private AddCommentDto commentDto(String contenido) {
        AddCommentDto dto = new AddCommentDto();
        dto.setContenido(contenido);
        return dto;
    }

    // ── Tests: validación de input ────────────────────────────────────────────

    @Test
    void executeShouldThrowBadRequestWhenDtoIsNull() {
        assertThrows(BadRequestException.class,
                () -> useCase.execute(STRATEGY_ID, null));
    }

    @Test
    void executeShouldThrowBadRequestWhenContenidoIsNull() {
        assertThrows(BadRequestException.class,
                () -> useCase.execute(STRATEGY_ID, commentDto(null)));
    }

    @Test
    void executeShouldThrowBadRequestWhenContenidoIsBlank() {
        assertThrows(BadRequestException.class,
                () -> useCase.execute(STRATEGY_ID, commentDto("   ")));
    }

    // ── Tests: estrategia no encontrada / forbidden ───────────────────────────

    @Test
    void executeShouldThrowNotFoundWhenStrategyDoesNotExist() {
        UUID unknown = UUID.randomUUID();
        when(repository.findOneById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> useCase.execute(unknown, commentDto("Comentario válido")));
    }

    @Test
    void executeShouldThrowForbiddenWhenStrategyBelongsToAnotherUser() {
        strategy.setUsuarioId(OTHER_ID);

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(STRATEGY_ID, commentDto("Comentario válido")));
    }

    @Test
    void executeShouldNotPersistWhenForbidden() {
        strategy.setUsuarioId(OTHER_ID);

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(STRATEGY_ID, commentDto("Comentario válido")));
        verify(repository, never()).update(any());
    }

    // ── Tests: caso feliz ─────────────────────────────────────────────────────

    @Test
    void executeShouldCallUpdateAfterAddingComment() {
        useCase.execute(STRATEGY_ID, commentDto("Campaña exitosa en Jalisco"));

        verify(repository, times(1)).update(strategy);
    }

    @Test
    void executeShouldPersistCommentInJsonField() {
        useCase.execute(STRATEGY_ID, commentDto("Excelente resultado"));

        // El JSON de comentarios debe haberse escrito en el strategy antes del update
        verify(repository, times(1)).update(argThat(s ->
                s.getComentariosJson() != null
                        && s.getComentariosJson().contains("Excelente resultado")
        ));
    }

    @Test
    void executeShouldTrimContenidoBeforePersisting() {
        useCase.execute(STRATEGY_ID, commentDto("  texto con espacios  "));

        verify(repository, times(1)).update(argThat(s ->
                s.getComentariosJson() != null
                        && s.getComentariosJson().contains("texto con espacios")
                        && !s.getComentariosJson().contains("  texto")
        ));
    }

    @Test
    void executeShouldAppendCommentWhenComentariosJsonIsNull() {
        strategy.setComentariosJson(null);

        // No debe lanzar excepción — parseComentarios devuelve lista vacía si json es nulo
        assertDoesNotThrow(() ->
                useCase.execute(STRATEGY_ID, commentDto("Primer comentario"))
        );
        verify(repository, times(1)).update(any());
    }

    @Test
    void executeShouldAppendCommentWhenComentariosJsonIsMalformed() {
        strategy.setComentariosJson("esto-no-es-json");

        // parseComentarios hace fallback a lista vacía ante JSON inválido
        assertDoesNotThrow(() ->
                useCase.execute(STRATEGY_ID, commentDto("Comentario tras JSON corrupto"))
        );
        verify(repository, times(1)).update(any());
    }

    @Test
    void executeShouldDelegateMappingToDtoFactory() {
        useCase.execute(STRATEGY_ID, commentDto("Comentario de prueba"));

        verify(dtoFactory, times(1)).from(strategy);
    }
}
