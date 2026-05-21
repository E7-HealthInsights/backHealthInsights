package org.acme.application.usecase;

import org.acme.domain.exception.UserNotFoundException;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeactivateUserUseCaseTest {

    private UserRepository userRepository;
    private DeactivateUserUseCase useCase;

    private final UUID USER_ID = UUID.randomUUID();
    private User existingUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        useCase = new DeactivateUserUseCase(userRepository);

        Role adminRole = new Role((byte) 1, "ADMIN");
        existingUser = new User(USER_ID, "Juan", "Pérez", "juan@test.com", adminRole, true, "firebase-uid");

        when(userRepository.findUserById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void executeShouldSetStatusFalse() {
        useCase.execute(USER_ID);

        assertFalse(existingUser.isStatus());
    }

    @Test
    void executeShouldCallUpdateAfterDeactivating() {
        useCase.execute(USER_ID);

        verify(userRepository, times(1)).update(existingUser);
    }

    @Test
    void executeShouldThrowUserNotFoundWhenUserDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findUserById(unknownId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(unknownId));
        verify(userRepository, never()).update(any());
    }

    @Test
    void executeShouldCallFindBeforeUpdate() {
        useCase.execute(USER_ID);

        var inOrder = inOrder(userRepository);
        inOrder.verify(userRepository).findUserById(USER_ID);
        inOrder.verify(userRepository).update(any());
    }

    @Test
    void executeShouldNotModifyOtherFields() {
        useCase.execute(USER_ID);

        assertEquals("Juan", existingUser.getName());
        assertEquals("Pérez", existingUser.getLastName());
        assertEquals("juan@test.com", existingUser.getEmail());
    }
}
