package org.acme.application.usecase;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.acme.domain.models.Role;
import org.acme.domain.models.TipoWidget;
import org.acme.domain.models.User;
import org.acme.domain.models.Widget;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteWidgetUseCaseTest {

    private WidgetRepository widgetRepository;
    private AuthContext authContext;
    private DeleteWidgetUseCase useCase;

    private User authenticatedUser;
    private final UUID WIDGET_PERSONAL_ID = UUID.randomUUID();
    private final UUID WIDGET_DEFAULT_ID  = UUID.randomUUID();
    private final UUID WIDGET_UNKNOWN_ID  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        widgetRepository = mock(WidgetRepository.class);
        authContext      = mock(AuthContext.class);

        Role role = new Role((byte) 3, "DIRECTOR_FINANZAS");
        authenticatedUser = new User(
                UUID.randomUUID(), "Luis", "Gómez", "luis@test.com", role, true, "firebase-uid");

        when(authContext.getUser()).thenReturn(authenticatedUser);

        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");

        // Widget personal del usuario autenticado
        Widget widgetPersonal = new Widget(
                WIDGET_PERSONAL_ID, "Mi widget", authenticatedUser, tipo,
                "{\"tabla\":\"test\"}", 1, null);

        // Widget por defecto del rol (no pertenece al usuario)
        Widget widgetDefault = new Widget(
                WIDGET_DEFAULT_ID, "Widget de rol", null, tipo,
                "{\"tabla\":\"test\"}", 1, role.getId());

        when(widgetRepository.findByUserId(authenticatedUser.getId()))
                .thenReturn(List.of(widgetPersonal));
        when(widgetRepository.findDefaultsByRolId(role.getId()))
                .thenReturn(List.of(widgetDefault));

        useCase = new DeleteWidgetUseCase(widgetRepository, authContext);
    }

    // ── Tests: caso feliz ─────────────────────────────────────────────────────

    @Test
    void executeShouldDeletePersonalWidget() {
        useCase.execute(WIDGET_PERSONAL_ID);

        verify(widgetRepository, times(1)).removeById(WIDGET_PERSONAL_ID);
    }

    // ── Tests: widget del rol → 403 ───────────────────────────────────────────

    @Test
    void executeShouldThrowForbiddenWhenWidgetIsDefault() {
        assertThrows(ForbiddenException.class,
                () -> useCase.execute(WIDGET_DEFAULT_ID));
    }

    @Test
    void executeShouldNotDeleteDefaultWidget() {
        assertThrows(ForbiddenException.class,
                () -> useCase.execute(WIDGET_DEFAULT_ID));

        verify(widgetRepository, never()).removeById(WIDGET_DEFAULT_ID);
    }

    // ── Tests: widget no encontrado → 404 ────────────────────────────────────

    @Test
    void executeShouldThrowNotFoundWhenWidgetDoesNotExistInEitherList() {
        assertThrows(NotFoundException.class,
                () -> useCase.execute(WIDGET_UNKNOWN_ID));
    }

    @Test
    void executeShouldNotDeleteWhenWidgetNotFound() {
        assertThrows(NotFoundException.class,
                () -> useCase.execute(WIDGET_UNKNOWN_ID));

        verify(widgetRepository, never()).removeById(WIDGET_UNKNOWN_ID);
    }

    // ── Tests: verificación de lookups ───────────────────────────────────────

    @Test
    void executeShouldLookupPersonalWidgetsByUserId() {
        useCase.execute(WIDGET_PERSONAL_ID);

        verify(widgetRepository, times(1)).findByUserId(authenticatedUser.getId());
    }

    @Test
    void executeShouldLookupDefaultsByRolIdWhenPersonalNotFound() {
        // Al intentar eliminar un widget desconocido primero busca en personales,
        // luego verifica en los de rol para discriminar 403 vs 404
        assertThrows(NotFoundException.class, () -> useCase.execute(WIDGET_UNKNOWN_ID));

        verify(widgetRepository, times(1)).findByUserId(authenticatedUser.getId());
        verify(widgetRepository, times(1)).findDefaultsByRolId(authenticatedUser.getRole().getId());
    }
}
