package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exception.UserNotFoundException;
import org.acme.domain.repository.UserRepository;

import java.util.UUID;

@ApplicationScoped
public class DeleteUserUseCase {

    private final UserRepository userRepository;

    @Inject
    public DeleteUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(UUID userId) {
        userRepository.findUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.deleteUserById(userId);
    }
}
