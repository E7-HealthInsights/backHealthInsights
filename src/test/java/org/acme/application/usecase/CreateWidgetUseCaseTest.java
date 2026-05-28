// application/usecase/widget/CreateWidgetUseCaseTest.java
package org.acme.application.usecase;

import jakarta.inject.Inject;
import org.acme.application.dto.CreateWidgetDto;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.models.Widget;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateWidgetUseCaseTest {

    private WidgetRepository widgetRepository;
    private CreateWidgetUseCase useCase;
    private AuthContext authContext;
    private User authenticatedUser;

    @BeforeEach
    void setUp() {
        widgetRepository = mock(WidgetRepository.class);
        authContext = mock(AuthContext.class);

        authenticatedUser = new User(
                UUID.randomUUID(), "Test", "Testt", "test@test.com", mock(Role.class), true, "firebase-uid");

        when(authContext.getUser()).thenReturn(authenticatedUser);
        // El repositorio devuelve el mismo widget que recibe
        when(widgetRepository.create(any(Widget.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        useCase = new CreateWidgetUseCase(widgetRepository, authContext);
    }

    @Test
    void executeShouldCreateWidgetWithCorrectData() {
        CreateWidgetDto dto = new CreateWidgetDto();
        dto.setTitulo("Total detecciones 2023");
        dto.setTipoId((byte) 1);
        dto.setQueryConfig("{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"SUM\",\"columna\":\"detecciones\"}");
        dto.setOrden(1);

        Widget result = useCase.execute(dto);

        assertNotNull(result);
        assertEquals("Total detecciones 2023", result.getTitulo());
        assertEquals("{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"SUM\",\"columna\":\"detecciones\"}", result.getQuery());
        assertEquals(1, result.getOrden());
        assertNotNull(result.getId());                              // UUID generado
        assertEquals(authContext.getUser().getId(), result.getUsuario().getId()); // usuario correcto
        assertEquals((byte) 1, result.getTipo().getId());          // tipo correcto
    }

    @Test
    void executeShouldGenerateUniqueIds() {
        CreateWidgetDto dto = new CreateWidgetDto();
        dto.setTitulo("Widget A");
        dto.setTipoId((byte) 1);
        dto.setQueryConfig("{\"tabla\":\"inegi_defunciones\",\"funcion\":\"COUNT\",\"columna\":\"_id\"}");
        dto.setOrden(1);

        Widget w1 = useCase.execute(dto);
        Widget w2 = useCase.execute(dto);

        assertNotEquals(w1.getId(), w2.getId()); // cada widget tiene su propio UUID
    }

    @Test
    void executeShouldAlwaysSetRolIdToNull() {
        // Los widgets creados por usuario siempre son personales, nunca de rol
        CreateWidgetDto dto = new CreateWidgetDto();
        dto.setTitulo("Widget personal");
        dto.setTipoId((byte) 2);
        dto.setQueryConfig("{\"tabla\":\"f4_pib_bancomundial\",\"funcion\":\"AVG\",\"columna\":\"obs_value\"}");
        dto.setOrden(1);

        Widget result = useCase.execute(dto);

        assertNull(result.getRolId()); // nunca asociado a un rol
    }

    @Test
    void executeShouldAssignAuthenticatedUserAsOwner() {
        CreateWidgetDto dto = new CreateWidgetDto();
        dto.setTitulo("Widget de Alejandra");
        dto.setTipoId((byte) 1);
        dto.setQueryConfig("{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"SUM\",\"columna\":\"detecciones\"}");
        dto.setOrden(2);

        Widget result = useCase.execute(dto);

        assertNotNull(result.getUsuario());
        assertEquals(authenticatedUser.getId(), result.getUsuario().getId());
        assertEquals(authenticatedUser.getEmail(), result.getUsuario().getEmail());
    }

    @Test
    void executeShouldCallRepositoryExactlyOnce() {
        CreateWidgetDto dto = new CreateWidgetDto();
        dto.setTitulo("Widget conteo");
        dto.setTipoId((byte) 1);
        dto.setQueryConfig("{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"SUM\",\"columna\":\"detecciones\"}");
        dto.setOrden(1);

        useCase.execute(dto);

        verify(widgetRepository, times(1)).create(any(Widget.class));
    }

    @Test
    void executeShouldStoreQueryConfigAsIs() {
        // El queryConfig se guarda exactamente como llega — sin transformaciones
        String queryJson = "{\"tabla\":\"f4_pib_bancomundial\",\"colX\":\"time_period\",\"colY\":\"obs_value\",\"funcion\":\"AVG\",\"groupBy\":\"time_period\"}";

        CreateWidgetDto dto = new CreateWidgetDto();
        dto.setTitulo("Evolución PIB");
        dto.setTipoId((byte) 2);
        dto.setQueryConfig(queryJson);
        dto.setOrden(3);

        Widget result = useCase.execute(dto);

        assertEquals(queryJson, result.getQuery());
    }

    @Test
    void executeShouldRespectOrdenFromDto() {
        CreateWidgetDto dto = new CreateWidgetDto();
        dto.setTitulo("Widget orden 5");
        dto.setTipoId((byte) 1);
        dto.setQueryConfig("{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"COUNT\",\"columna\":\"_id\"}");
        dto.setOrden(5);

        Widget result = useCase.execute(dto);

        assertEquals(5, result.getOrden());
    }

    @Test
    void executeShouldSetDefaultOrdenZeroWhenNotProvided() {
        // Si no se manda orden en el DTO, queda en 0 por defecto del tipo primitivo int
        CreateWidgetDto dto = new CreateWidgetDto();
        dto.setTitulo("Widget sin orden");
        dto.setTipoId((byte) 1);
        dto.setQueryConfig("{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"COUNT\",\"columna\":\"_id\"}");
        // orden no se setea — queda en 0

        Widget result = useCase.execute(dto);

        assertEquals(0, result.getOrden());
    }
}