package org.acme.application.usecase;

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

class GetUserWidgetsUseCaseTest {

    private WidgetRepository widgetRepository;
    private AuthContext authContext;
    private GetUserWidgetsUseCase useCase;
    private User authenticatedUser;

    @BeforeEach
    void setUp() {
        widgetRepository = mock(WidgetRepository.class);
        authContext = mock(AuthContext.class);

        authenticatedUser = new User(
                UUID.randomUUID(), "Test", "Testt", "test@test.com", mock(Role.class), true, "firebase-uid");

        when(authContext.getUser()).thenReturn(authenticatedUser);

        useCase = new GetUserWidgetsUseCase(widgetRepository, authContext);
    }

    @Test
    void executeShouldReturnWidgetsBelongingToUser() {
        UUID userId = authenticatedUser.getId();

        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");

        Widget w1 = new Widget(UUID.randomUUID(), "Widget 1", authenticatedUser, tipo, "SELECT COUNT(*) FROM ihme_gbd", 1);
        Widget w2 = new Widget(UUID.randomUUID(), "Widget 2", authenticatedUser, tipo, "SELECT SUM(detecciones) FROM imss_deteccion_diabetes", 2);

        when(widgetRepository.findByUserId(userId)).thenReturn(List.of(w1, w2));

        List<Widget> result = useCase.execute(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Widget 1", result.get(0).getTitulo());
        assertEquals("Widget 2", result.get(1).getTitulo());
        verify(widgetRepository, times(1)).findByUserId(userId);  // verifica que se llamó al repo con el id correcto
    }

    @Test
    void executeShouldReturnEmptyListWhenUserHasNoWidgets() {
        UUID userId = authenticatedUser.getId();
        when(widgetRepository.findByUserId(userId)).thenReturn(List.of());

        List<Widget> result = useCase.execute(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(widgetRepository, times(1)).findByUserId(userId);
    }

    @Test
    void executeShouldOnlyQueryByTheProvidedUserId() {
        UUID userId = authenticatedUser.getId();
        UUID otroUserId = UUID.randomUUID();  // otro usuario

        when(widgetRepository.findByUserId(userId)).thenReturn(List.of());
        when(widgetRepository.findByUserId(otroUserId)).thenReturn(List.of(
                new Widget(UUID.randomUUID(), "Widget ajeno", null, null, "query", 1)
        ));

        List<Widget> result = useCase.execute(userId);

        assertTrue(result.isEmpty());
        verify(widgetRepository, never()).findByUserId(otroUserId);  // nunca consulta por otro usuario
    }
}