package org.acme.application.usecase;

import org.acme.application.dto.GuardarProyeccionDto;
import org.acme.application.dto.ProyeccionResponseDto;
import org.acme.domain.models.Proyeccion;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.ProyeccionRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GuardarProyeccionUseCaseTest {

    // Prueba unitaria — sin DB, sin Quarkus, todo mockeado
    // Verifica que el use case construye y persiste la proyección correctamente

    private ProyeccionRepository proyeccionRepository;
    private AuthContext authContext;
    private GuardarProyeccionUseCase useCase;
    private User authenticatedUser;

    @BeforeEach
    void setUp() {
        proyeccionRepository = mock(ProyeccionRepository.class);
        authContext          = mock(AuthContext.class);

        authenticatedUser = new User(
                UUID.randomUUID(), "Test", "Director",
                "director@test.com", mock(Role.class), true, "firebase-uid");

        when(authContext.getUser()).thenReturn(authenticatedUser);
        when(proyeccionRepository.save(any(Proyeccion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        useCase = new GuardarProyeccionUseCase(proyeccionRepository, authContext);
    }

    @Test
    void executeShouldPersistProyeccionWithCorrectData() {
        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Escenario optimista 2040");
        dto.setDescripcion("Inversión intensiva en prevención");
        dto.setResultado("{\"params\":{},\"puntos\":[],\"kpis\":{}}");

        ProyeccionResponseDto result = useCase.execute(dto);

        assertNotNull(result);
        assertEquals("Escenario optimista 2040", result.getTitulo());
        assertEquals("Inversión intensiva en prevención", result.getDescripcion());
        assertEquals("{\"params\":{},\"puntos\":[],\"kpis\":{}}", result.getResultado());
        assertNotNull(result.getId());
        assertNotNull(result.getFechaCreacion());
    }

    @Test
    void executeShouldAssignAuthenticatedUserAsOwner() {
        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Escenario test");
        dto.setResultado("{}");

        useCase.execute(dto);

        verify(proyeccionRepository).save(argThat(p ->
            p.getUsuario().getId().equals(authenticatedUser.getId())
        ));
    }

    @Test
    void executeShouldUseDescripcionFromDto() {
        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Título");
        dto.setDescripcion("Descripción personalizada");
        dto.setResultado("{}");

        ProyeccionResponseDto result = useCase.execute(dto);

        assertEquals("Descripción personalizada", result.getDescripcion());
    }

    @Test
    void executeShouldFallbackToTituloWhenDescripcionIsNull() {
        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Mi escenario");
        dto.setDescripcion(null);
        dto.setResultado("{}");

        ProyeccionResponseDto result = useCase.execute(dto);

        assertEquals("Mi escenario", result.getDescripcion());
    }

    @Test
    void executeShouldCallRepositorySaveExactlyOnce() {
        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Test");
        dto.setResultado("{}");

        useCase.execute(dto);

        verify(proyeccionRepository, times(1)).save(any(Proyeccion.class));
    }

    @Test
    void executeShouldGenerateUniqueIds() {
        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Test");
        dto.setResultado("{}");

        ProyeccionResponseDto r1 = useCase.execute(dto);
        ProyeccionResponseDto r2 = useCase.execute(dto);

        assertNotEquals(r1.getId(), r2.getId());
    }
}