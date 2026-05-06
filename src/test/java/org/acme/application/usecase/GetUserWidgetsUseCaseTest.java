package org.acme.application.usecase;

import org.acme.application.dto.WidgetResponseDto;
import org.acme.domain.models.Role;
import org.acme.domain.models.TipoWidget;
import org.acme.domain.models.User;
import org.acme.domain.models.Widget;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.query.QueryExecutor;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetUserWidgetsUseCaseTest {

    private WidgetRepository widgetRepository;
    private AuthContext authContext;
    private GetUserWidgetsUseCase useCase;
    private User authenticatedUser;
    private Role role;
    private QueryExecutor queryExecutor;

    @BeforeEach
    void setUp() {
        widgetRepository = mock(WidgetRepository.class);
        authContext = mock(AuthContext.class);
        queryExecutor = mock(QueryExecutor.class);

        role = new Role((byte) 3, "DIRECTOR_FINANZAS");
        authenticatedUser = new User(
                UUID.randomUUID(), "Test", "Testt", "test@test.com", role, true, "firebase-uid");

        when(authContext.getUser()).thenReturn(authenticatedUser);

        // Por defecto los defaults y personales regresan vacío
        when(widgetRepository.findDefaultsByRolId(any())).thenReturn(List.of());
        when(widgetRepository.findByUserId(any())).thenReturn(List.of());

        // QueryExecutor retorna data simulada por defecto
        when(queryExecutor.execute(anyString(), anyString()))
                .thenReturn(Map.of("value", 42));

        useCase = new GetUserWidgetsUseCase(widgetRepository, authContext, queryExecutor);
    }

    @Test
    void executeShouldReturnWidgetsBelongingToUserAndRole() {
        UUID userId = authenticatedUser.getId();

        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");

        Widget wPersonal = new Widget(UUID.randomUUID(), "Widget personal", authenticatedUser, tipo,
                "{\"tabla\":\"f4_pib_bancomundial\",\"colX\":\"time_period\",\"colY\":\"obs_value\",\"funcion\":\"AVG\",\"groupBy\":\"time_period\"}",
                1, null);

        Widget wDefault = new Widget(UUID.randomUUID(), "Widget de rol", null, tipo,
                "{\"tabla\":\"f11_health_coverage_oecd\",\"colLabel\":\"insurance_type\",\"colValue\":\"obs_value\",\"funcion\":\"AVG\"}",
                1, (byte) 3);

        when(widgetRepository.findByUserId(userId)).thenReturn(List.of(wPersonal));
        when(widgetRepository.findDefaultsByRolId((byte) 3)).thenReturn(List.of(wDefault));


        List<WidgetResponseDto> result = useCase.execute();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Widget de rol", result.get(0).getTitulo());
        assertEquals("Widget personal", result.get(1).getTitulo());
        verify(widgetRepository, times(1)).findByUserId(userId);  // verifica que se llamó al repo con el id correcto
    }

    @Test
    void executeShouldOnlyQueryByTheProvidedUserId() {
        UUID userId = authenticatedUser.getId();
        UUID otroUserId = UUID.randomUUID();  // otro usuario

        when(widgetRepository.findByUserId(userId)).thenReturn(List.of());
        when(widgetRepository.findByUserId(otroUserId)).thenReturn(List.of(
                new Widget(UUID.randomUUID(), "Widget ajeno", null, null, "query", 1, null)
        ));

        List<WidgetResponseDto> result = useCase.execute();

        // el resultado está vacío porque el usuario autenticado no tiene widgets
        assertTrue(result.isEmpty());
        // nunca se consultó por el otro usuario
        verify(widgetRepository, never()).findByUserId(otroUserId);
    }

    @Test
    void executeShouldReturnEmptyListWhenUserHasNoWidgets() {
        List<WidgetResponseDto> result = useCase.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void executeShouldCallQueryExecutorForEachWidget() {
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        String queryJson = "{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"SUM\",\"columna\":\"detecciones\"}";

        Widget w1 = new Widget(UUID.randomUUID(), "Widget 1", authenticatedUser, tipo, queryJson, 1, null);
        Widget w2 = new Widget(UUID.randomUUID(), "Widget 2", authenticatedUser, tipo, queryJson, 2, null);

        when(widgetRepository.findByUserId(authenticatedUser.getId()))
                .thenReturn(List.of(w1, w2));

        useCase.execute();

        // Verifica que el executor se llamó una vez por cada widget
        verify(queryExecutor, times(2)).execute(queryJson, "STAT");
    }

    @Test
    void executeShouldNeverQueryAnotherUsersWidgets() {
        UUID otroUserId = UUID.randomUUID();

        when(widgetRepository.findByUserId(otroUserId)).thenReturn(List.of(
                new Widget(UUID.randomUUID(), "Widget ajeno", null, null, "{}", 1, null)
        ));

        List<WidgetResponseDto> result = useCase.execute();

        assertTrue(result.isEmpty());
        verify(widgetRepository, never()).findByUserId(otroUserId);
    }

    @Test
    void executeShouldIncludeDataFromQueryExecutorInResponse() {
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        String queryJson = "{\"tabla\":\"idf_diabetes_atlas\",\"funcion\":\"MAX\",\"columna\":\"expenditure_per_person_usd\"}";

        Widget widget = new Widget(UUID.randomUUID(), "Gasto per cápita", authenticatedUser, tipo, queryJson, 1, null);
        when(widgetRepository.findByUserId(authenticatedUser.getId())).thenReturn(List.of(widget));
        when(queryExecutor.execute(queryJson, "STAT")).thenReturn(Map.of("value", 1438));

        List<WidgetResponseDto> result = useCase.execute();

        assertEquals(1, result.size());
        assertEquals(1438, result.get(0).getData().get("value"));
    }
}