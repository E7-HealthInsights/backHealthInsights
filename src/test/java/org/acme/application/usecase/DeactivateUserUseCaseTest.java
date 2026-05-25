package org.acme.application.usecase;

import org.acme.application.dto.DeactivateUserDto;
import org.acme.domain.exception.UserNotFoundException;
import org.acme.domain.models.LogActividad;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeactivateUserUseCaseTest {

    private UserRepository userRepository;
    private AuthContext authContext;
    private LogActividadRepository logActividadRepository;
    private DeactivateUserUseCase useCase;

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID LOG_ID = UUID.randomUUID();
    private User existingUser;
    private LogActividad logEntry;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authContext = mock(AuthContext.class);
        logActividadRepository = mock(LogActividadRepository.class);
        useCase = new DeactivateUserUseCase(userRepository, authContext, logActividadRepository);

        Role adminRole = new Role((byte) 1, "ADMIN");
        User adminUser = new User(UUID.randomUUID(), "Admin", "Test", "admin@test.com", adminRole, true, "firebase-admin");
        existingUser = new User(USER_ID, "Juan", "Pérez", "juan@test.com", adminRole, true, "firebase-uid");
        logEntry = mock(LogActividad.class);

        when(userRepository.findUserById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authContext.getUser()).thenReturn(adminUser);
        when(logEntry.getId()).thenReturn(LOG_ID);
        when(logActividadRepository.findLatestByEntidadId(USER_ID.toString())).thenReturn(Optional.of(logEntry));
    }

    @Test
    void executeShouldSetStatusFalse() {
        useCase.execute(USER_ID, null);

        assertFalse(existingUser.isStatus());
    }

    @Test
    void executeShouldCallUpdateAfterDeactivating() {
        useCase.execute(USER_ID, null);

        verify(userRepository, times(1)).update(existingUser);
    }

    @Test
    void executeShouldThrowUserNotFoundWhenUserDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findUserById(unknownId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(unknownId, null));
        verify(userRepository, never()).update(any());
    }

    @Test
    void executeShouldUpdateDetalleWhenJustificationProvided() {
        DeactivateUserDto dto = new DeactivateUserDto();
        dto.setJustification("Acceso no autorizado");

        useCase.execute(USER_ID, dto);

        verify(logActividadRepository, times(1)).updateDetalle(LOG_ID, "Acceso no autorizado");
    }

    @Test
    void executeShouldNotUpdateDetalleWhenJustificationIsNull() {
        DeactivateUserDto dto = new DeactivateUserDto();

        useCase.execute(USER_ID, dto);

        verify(logActividadRepository, never()).updateDetalle(any(), any());
    }

    @Test
    void executeShouldNotUpdateDetalleWhenDtoIsNull() {
        useCase.execute(USER_ID, null);

        verify(logActividadRepository, never()).updateDetalle(any(), any());
    }

    @Test
    void executeShouldNotModifyOtherFields() {
        useCase.execute(USER_ID, null);

        assertEquals("Juan", existingUser.getName());
        assertEquals("Pérez", existingUser.getLastName());
        assertEquals("juan@test.com", existingUser.getEmail());
    }
}
