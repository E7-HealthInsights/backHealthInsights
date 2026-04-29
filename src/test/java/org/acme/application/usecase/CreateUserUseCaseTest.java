package org.acme.application.usecase;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.acme.application.dto.CreateUserDto;
import org.acme.domain.exception.EmailAlreadyExistsException;
import org.acme.domain.exception.RoleNotFoundException;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.RoleRepository;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.firebase.FirebaseUserCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CreateUserUseCaseTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private FirebaseUserCreator firebaseUserCreator;
    private CreateUserUseCase useCase;

    private Role role;

    @BeforeEach
    void setUp() throws FirebaseAuthException {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        firebaseUserCreator = mock(FirebaseUserCreator.class);

        role = new Role((byte) 1, "ADMIN");

        UserRecord mockUserRecord = mock(UserRecord.class);
        when(mockUserRecord.getUid()).thenReturn("firebase-test-uid");

        when(roleRepository.findRoleById((byte) 1)).thenReturn(Optional.of(role));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(firebaseUserCreator.create(anyString(), anyString())).thenReturn(mockUserRecord);
        when(userRepository.create(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase = new CreateUserUseCase(userRepository, firebaseUserCreator, roleRepository);
    }

    @Test
    void executeShouldCreateUserWithCorrectData() throws FirebaseAuthException {
        CreateUserDto dto = new CreateUserDto();
        dto.setName("Juan");
        dto.setLastName("Pérez");
        dto.setEmail("juan@test.com");
        dto.setPassword("Password1");
        dto.setRoleId((byte) 1);

        User result = useCase.execute(dto);

        assertNotNull(result);
        assertEquals("Juan", result.getName());
        assertEquals("Pérez", result.getLastName());
        assertEquals("juan@test.com", result.getEmail());
        assertTrue(result.isStatus());
        assertNotNull(result.getId());
        assertEquals("firebase-test-uid", result.getProviderId());
        assertEquals(role, result.getRole());
    }

    @Test
    void executeShouldGenerateUniqueIds() throws FirebaseAuthException {
        CreateUserDto dto = new CreateUserDto();
        dto.setName("Ana");
        dto.setLastName("García");
        dto.setEmail("ana@test.com");
        dto.setPassword("Password1");
        dto.setRoleId((byte) 1);

        User u1 = useCase.execute(dto);
        User u2 = useCase.execute(dto);

        assertNotEquals(u1.getId(), u2.getId());
    }

    @Test
    void executeShouldThrowWhenRoleNotFound() throws FirebaseAuthException {
        CreateUserDto dto = new CreateUserDto();
        dto.setName("Luis");
        dto.setLastName("López");
        dto.setEmail("luis@test.com");
        dto.setPassword("Password1");
        dto.setRoleId((byte) 99);

        when(roleRepository.findRoleById((byte) 99)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> useCase.execute(dto));
        verify(userRepository, never()).create(any());
    }

    @Test
    void executeShouldThrowWhenEmailAlreadyExists() throws FirebaseAuthException {
        CreateUserDto dto = new CreateUserDto();
        dto.setName("María");
        dto.setLastName("Torres");
        dto.setEmail("repetido@test.com");
        dto.setPassword("Password1");
        dto.setRoleId((byte) 1);

        when(userRepository.existsByEmail("repetido@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> useCase.execute(dto));
        verify(firebaseUserCreator, never()).create(anyString(), anyString());
        verify(userRepository, never()).create(any());
    }
}
