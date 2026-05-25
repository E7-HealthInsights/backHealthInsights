package org.acme.application.usecase;

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
import static org.mockito.Mockito.*;

class EliminarProyeccionUseCaseTest {

    // Prueba unitaria — verifica ownership check y llamada a delete

    private ProyeccionRepository proyeccionRepository;
    private AuthContext authContext;
    private EliminarProyeccionUseCase useCase;

    private User owner;
    private User otherUser;
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

        Proyeccion proyeccion = new Proyeccion();
        proyeccion.setId(proyeccionId);
        proyeccion.setUsuario(owner);

        when(proyeccionRepository.findProyeccionById(proyeccionId))
                .thenReturn(Optional.of(proyeccion));

        useCase = new EliminarProyeccionUseCase(proyeccionRepository, authContext);
    }

    @Test
    void executeShouldDeleteWhenOwnerRequests() {
        when(authContext.getUser()).thenReturn(owner);

        useCase.execute(proyeccionId);

        verify(proyeccionRepository, times(1)).delete(proyeccionId);
    }

    @Test
    void executeShouldThrow403WhenNotOwner() {
        when(authContext.getUser()).thenReturn(otherUser);

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(proyeccionId));

        verify(proyeccionRepository, never()).delete(any());
    }

    @Test
    void executeShouldThrow404WhenProyeccionNotFound() {
        when(authContext.getUser()).thenReturn(owner);
        UUID randomId = UUID.randomUUID();
        when(proyeccionRepository.findProyeccionById(randomId)).thenReturn(Optional.empty());

        assertThrows(ProyeccionNotFoundException.class,
                () -> useCase.execute(randomId));
    }
}