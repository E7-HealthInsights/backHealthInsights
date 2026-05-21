package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exception.UserNotFoundException;
import org.acme.domain.repository.UserRepository;
import org.acme.domain.models.User;

import java.util.UUID;

@ApplicationScoped
public class DeactivateUserUseCase {

    private final UserRepository userRepository;

    @Inject
    public DeactivateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(UUID userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setStatus(false);
        userRepository.update(user);
    }
}
