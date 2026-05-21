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
import static org.mockito.Mockito.*;

class DeleteUserUseCaseTest {

    private UserRepository userRepository;
    private DeleteUserUseCase useCase;

    private final UUID USER_ID = UUID.randomUUID();
    private User existingUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        useCase = new DeleteUserUseCase(userRepository);

        Role adminRole = new Role((byte) 1, "ADMIN");
        existingUser = new User(USER_ID, "Juan", "Pérez", "juan@test.com", adminRole, true, "firebase-uid");

        when(userRepository.findUserById(USER_ID)).thenReturn(Optional.of(existingUser));
    }

    @Test
    void executeShouldCallDeleteWhenUserExists() {
        useCase.execute(USER_ID);

        verify(userRepository, times(1)).deleteUserById(USER_ID);
    }

    @Test
    void executeShouldThrowUserNotFoundWhenUserDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findUserById(unknownId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(unknownId));
        verify(userRepository, never()).deleteUserById(any());
    }

    @Test
    void executeShouldNotCallDeleteWhenUserNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findUserById(unknownId)).thenReturn(Optional.empty());

        try {
            useCase.execute(unknownId);
        } catch (UserNotFoundException ignored) {}

        verify(userRepository, never()).deleteUserById(unknownId);
    }

    @Test
    void executeShouldCallFindUserByIdBeforeDelete() {
        useCase.execute(USER_ID);

        var inOrder = inOrder(userRepository);
        inOrder.verify(userRepository).findUserById(USER_ID);
        inOrder.verify(userRepository).deleteUserById(USER_ID);
    }
}
