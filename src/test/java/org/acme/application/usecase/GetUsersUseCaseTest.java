package org.acme.application.usecase;

import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetUsersUseCaseTest {

    private UserRepository userRepository;
    private GetUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        useCase = new GetUsersUseCase(userRepository);
    }

    @Test
    void executeShouldReturnAllUsers() {
        Role role = new Role((byte) 1, "ADMIN");

        User u1 = new User(UUID.randomUUID(), "Juan", "Pérez", "juan@test.com", role, true, "firebase-uid-1");
        User u2 = new User(UUID.randomUUID(), "Ana", "García", "ana@test.com", role, true, "firebase-uid-2");

        ArrayList<User> users = new ArrayList<>();
        users.add(u1);
        users.add(u2);

        when(userRepository.findAllUsers()).thenReturn(users);

        ArrayList<User> result = useCase.execute();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Juan", result.get(0).getName());
        assertEquals("Ana", result.get(1).getName());
        verify(userRepository, times(1)).findAllUsers();
    }

    @Test
    void executeShouldReturnEmptyListWhenNoUsers() {
        when(userRepository.findAllUsers()).thenReturn(new ArrayList<>());

        ArrayList<User> result = useCase.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAllUsers();
    }

    @Test
    void executeShouldDelegateToRepositoryExactlyOnce() {
        when(userRepository.findAllUsers()).thenReturn(new ArrayList<>());

        useCase.execute();

        verify(userRepository, times(1)).findAllUsers();
        verifyNoMoreInteractions(userRepository);
    }
}
