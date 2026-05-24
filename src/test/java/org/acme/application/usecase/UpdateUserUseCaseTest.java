package org.acme.application.usecase;

import org.acme.application.dto.UpdateUserDto;
import org.acme.domain.exception.RoleNotFoundException;
import org.acme.domain.exception.UserNotFoundException;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.domain.repository.RoleRepository;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UpdateUserUseCaseTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private AuthContext authContext;
    private LogActividadRepository logActividadRepository;
    private UpdateUserUseCase useCase;

    private final UUID USER_ID = UUID.randomUUID();
    private User existingUser;
    private Role adminRole;
    private Role finanzasRole;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        authContext = mock(AuthContext.class);
        logActividadRepository = mock(LogActividadRepository.class);

        User adminUser = new User(UUID.randomUUID(), "Admin", "Test", "admin@test.com", null, true, "firebase-admin");
        when(authContext.getUser()).thenReturn(adminUser);
        when(logActividadRepository.findLatestByEntidadId(anyString())).thenReturn(Optional.empty());

        useCase = new UpdateUserUseCase(userRepository, roleRepository, authContext, logActividadRepository);

        adminRole = new Role((byte) 1, "ADMIN");
        finanzasRole = new Role((byte) 2, "FINANZAS");

        existingUser = new User(USER_ID, "Juan", "Pérez", "juan@test.com", adminRole, true, "firebase-uid");

        when(userRepository.findUserById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findRoleById((byte) 2)).thenReturn(Optional.of(finanzasRole));
    }

    @Test
    void executeShouldUpdateName() {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setName("Carlos");

        User result = useCase.execute(USER_ID, dto);

        assertEquals("Carlos", result.getName());
        assertEquals("Pérez", result.getLastName());
        assertEquals(adminRole, result.getRole());
    }

    @Test
    void executeShouldUpdateLastName() {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setLastName("López");

        User result = useCase.execute(USER_ID, dto);

        assertEquals("Juan", result.getName());
        assertEquals("López", result.getLastName());
    }

    @Test
    void executeShouldUpdateRole() {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setRoleId((byte) 2);

        User result = useCase.execute(USER_ID, dto);

        assertEquals(finanzasRole, result.getRole());
        assertEquals("Juan", result.getName());
    }

    @Test
    void executeShouldUpdateStatus() {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setStatus(false);

        User result = useCase.execute(USER_ID, dto);

        assertFalse(result.isStatus());
    }

    @Test
    void executeShouldUpdateAllFields() {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setName("Carlos");
        dto.setLastName("López");
        dto.setRoleId((byte) 2);
        dto.setStatus(false);

        User result = useCase.execute(USER_ID, dto);

        assertEquals("Carlos", result.getName());
        assertEquals("López", result.getLastName());
        assertEquals(finanzasRole, result.getRole());
        assertFalse(result.isStatus());
    }

    @Test
    void executeShouldNotModifyFieldsWhenDtoIsEmpty() {
        UpdateUserDto dto = new UpdateUserDto();

        User result = useCase.execute(USER_ID, dto);

        assertEquals("Juan", result.getName());
        assertEquals("Pérez", result.getLastName());
        assertEquals(adminRole, result.getRole());
        assertTrue(result.isStatus());
    }

    @Test
    void executeShouldThrowUserNotFoundWhenUserDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findUserById(unknownId)).thenReturn(Optional.empty());

        UpdateUserDto dto = new UpdateUserDto();
        dto.setName("Test");

        assertThrows(UserNotFoundException.class, () -> useCase.execute(unknownId, dto));
        verify(userRepository, never()).update(any());
    }

    @Test
    void executeShouldThrowRoleNotFoundWhenRoleDoesNotExist() {
        when(roleRepository.findRoleById((byte) 99)).thenReturn(Optional.empty());

        UpdateUserDto dto = new UpdateUserDto();
        dto.setRoleId((byte) 99);

        assertThrows(RoleNotFoundException.class, () -> useCase.execute(USER_ID, dto));
        verify(userRepository, never()).update(any());
    }

    @Test
    void executeShouldCallUpdateExactlyOnce() {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setName("Test");

        useCase.execute(USER_ID, dto);

        verify(userRepository, times(1)).update(any(User.class));
    }

    @Test
    void executeShouldNotCallRoleRepositoryWhenRoleIdIsNull() {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setName("Test");

        useCase.execute(USER_ID, dto);

        verify(roleRepository, never()).findRoleById(any());
    }
}
