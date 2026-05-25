package org.acme.application.usecase;

import org.acme.application.dto.GuardarProyeccionDto;
import org.acme.domain.exception.ProyeccionNotFoundException;
import org.acme.domain.exception.UnauthorizedException;
import org.acme.domain.models.Proyeccion;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.ProyeccionRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActualizarProyeccionUseCaseTest {

    // Prueba unitaria — verifica ownership check y actualización de campos

    private ProyeccionRepository proyeccionRepository;
    private AuthContext authContext;
    private ActualizarProyeccionUseCase useCase;

    private User owner;
    private User otherUser;
    private Proyeccion existingProyeccion;
    private UUID proyeccionId;

    @BeforeEach
    void setUp() {
        proyeccionRepository = mock(ProyeccionRepository.class);
        authContext          = mock(AuthContext.class);

        owner     = new User(UUID.randomUUID(), "Owner", "User",
                             "owner@test.com", mock(Role.class), true, "uid-owner");
        otherUser = new User(UUID.randomUUID(), "Other", "User",
                             "other@test.com", mock(Role.class), true, "uid-other");

        proyeccionId = UUID.randomUUID();
        existingProyeccion = new Proyeccion();
        existingProyeccion.setId(proyeccionId);
        existingProyeccion.setTitulo("Título original");
        existingProyeccion.setDescripcion("Descripción original");
        existingProyeccion.setParametros("{\"old\":true}");
        existingProyeccion.setUsuario(owner);

        when(proyeccionRepository.findProyeccionById(proyeccionId))
                .thenReturn(Optional.of(existingProyeccion));
        when(proyeccionRepository.update(any(Proyeccion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        useCase = new ActualizarProyeccionUseCase(proyeccionRepository, authContext);
    }

    @Test
    void executeShouldUpdateFieldsWhenOwnerEdits() {
        when(authContext.getUser()).thenReturn(owner);

        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Título actualizado");
        dto.setDescripcion("Nueva descripción");
        dto.setResultado("{\"new\":true}");

        var result = useCase.execute(proyeccionId, dto);

        assertEquals("Título actualizado", result.getTitulo());
        assertEquals("Nueva descripción", result.getDescripcion());
        assertEquals("{\"new\":true}", result.getResultado());
    }

    @Test
    void executeShouldThrow403WhenNotOwner() {
        when(authContext.getUser()).thenReturn(otherUser);

        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Intento");
        dto.setResultado("{}");

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(proyeccionId, dto));

        verify(proyeccionRepository, never()).update(any());
    }

    @Test
    void executeShouldThrow404WhenProyeccionNotFound() {
        when(authContext.getUser()).thenReturn(owner);
        UUID randomId = UUID.randomUUID();
        when(proyeccionRepository.findProyeccionById(randomId)).thenReturn(Optional.empty());

        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Test");
        dto.setResultado("{}");

        assertThrows(ProyeccionNotFoundException.class,
                () -> useCase.execute(randomId, dto));
    }

    @Test
    void executeShouldCallUpdateExactlyOnce() {
        when(authContext.getUser()).thenReturn(owner);

        GuardarProyeccionDto dto = new GuardarProyeccionDto();
        dto.setTitulo("Test");
        dto.setResultado("{}");

        useCase.execute(proyeccionId, dto);

        verify(proyeccionRepository, times(1)).update(any(Proyeccion.class));
    }
}