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

    @BeforeEach
    void setUp() {
        widgetRepository = mock(WidgetRepository.class);
        authContext = mock(AuthContext.class);

        User authenticatedUser = new User(
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
        dto.setQueryConfig("SELECT SUM(detecciones) FROM imss_deteccion_diabetes WHERE ano = 2023");
        dto.setOrden(1);

        Widget result = useCase.execute(dto, authContext.getUser());

        assertNotNull(result);
        assertEquals("Total detecciones 2023", result.getTitulo());
        assertEquals("SELECT SUM(detecciones) FROM imss_deteccion_diabetes WHERE ano = 2023", result.getQuery());
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
        dto.setQueryConfig("SELECT COUNT(*) FROM inegi_defunciones");
        dto.setOrden(1);

        Widget w1 = useCase.execute(dto, authContext.getUser());
        Widget w2 = useCase.execute(dto, authContext.getUser());

        assertNotEquals(w1.getId(), w2.getId()); // cada widget tiene su propio UUID
    }
}