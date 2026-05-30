package org.acme.application.usecase;

import org.acme.application.dto.WidgetOrdenDto;
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

class UpdateWidgetOrdenUseCaseTest {

    private WidgetRepository widgetRepository;
    private AuthContext authContext;
    private UpdateWidgetOrdenUseCase useCase;

    private User authenticatedUser;
    private final UUID OWNED_WIDGET_1 = UUID.randomUUID();
    private final UUID OWNED_WIDGET_2 = UUID.randomUUID();
    private final UUID FOREIGN_WIDGET = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        widgetRepository = mock(WidgetRepository.class);
        authContext      = mock(AuthContext.class);

        Role role = new Role((byte) 3, "DIRECTOR_FINANZAS");
        authenticatedUser = new User(
                UUID.randomUUID(), "Luis", "Gómez", "luis@test.com", role, true, "firebase-uid");

        when(authContext.getUser()).thenReturn(authenticatedUser);

        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        Widget w1 = new Widget(OWNED_WIDGET_1, "Widget A", authenticatedUser, tipo, "{}", 1, null);
        Widget w2 = new Widget(OWNED_WIDGET_2, "Widget B", authenticatedUser, tipo, "{}", 2, null);

        when(widgetRepository.findByUserId(authenticatedUser.getId()))
                .thenReturn(List.of(w1, w2));

        useCase = new UpdateWidgetOrdenUseCase(widgetRepository, authContext);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WidgetOrdenDto ordenDto(UUID id, int orden) {
        WidgetOrdenDto dto = new WidgetOrdenDto();
        dto.setId(id);
        dto.setOrden(orden);
        return dto;
    }

    // ── Tests: caso feliz ─────────────────────────────────────────────────────

    @Test
    void executeShouldUpdateOrderForOwnedWidgets() {
        List<WidgetOrdenDto> items = List.of(
                ordenDto(OWNED_WIDGET_1, 3),
                ordenDto(OWNED_WIDGET_2, 5)
        );

        useCase.execute(items);

        verify(widgetRepository, times(1)).updateOrden(OWNED_WIDGET_1, 3);
        verify(widgetRepository, times(1)).updateOrden(OWNED_WIDGET_2, 5);
    }

    @Test
    void executeShouldUpdateSingleOwnedWidget() {
        List<WidgetOrdenDto> items = List.of(ordenDto(OWNED_WIDGET_1, 10));

        useCase.execute(items);

        verify(widgetRepository, times(1)).updateOrden(OWNED_WIDGET_1, 10);
    }

    // ── Tests: widgets ajenos se ignoran silenciosamente ─────────────────────

    @Test
    void executeShouldIgnoreWidgetsThatDoNotBelongToUser() {
        List<WidgetOrdenDto> items = List.of(ordenDto(FOREIGN_WIDGET, 1));

        // No debe lanzar excepción — los ajenos simplemente se omiten
        assertDoesNotThrow(() -> useCase.execute(items));
        verify(widgetRepository, never()).updateOrden(FOREIGN_WIDGET, 1);
    }

    @Test
    void executeShouldOnlyUpdateOwnedWidgetsFromMixedList() {
        List<WidgetOrdenDto> items = List.of(
                ordenDto(OWNED_WIDGET_1, 2),
                ordenDto(FOREIGN_WIDGET, 99),
                ordenDto(OWNED_WIDGET_2, 4)
        );

        useCase.execute(items);

        verify(widgetRepository, times(1)).updateOrden(OWNED_WIDGET_1, 2);
        verify(widgetRepository, times(1)).updateOrden(OWNED_WIDGET_2, 4);
        verify(widgetRepository, never()).updateOrden(FOREIGN_WIDGET, 99);
    }

    // ── Tests: lista vacía ────────────────────────────────────────────────────

    @Test
    void executeShouldDoNothingWithEmptyList() {
        assertDoesNotThrow(() -> useCase.execute(List.of()));

        verify(widgetRepository, never()).updateOrden(any(), anyInt());
    }

    // ── Tests: lookups ────────────────────────────────────────────────────────

    @Test
    void executeShouldFetchOwnedWidgetsByUserId() {
        useCase.execute(List.of(ordenDto(OWNED_WIDGET_1, 1)));

        verify(widgetRepository, times(1)).findByUserId(authenticatedUser.getId());
    }
}
