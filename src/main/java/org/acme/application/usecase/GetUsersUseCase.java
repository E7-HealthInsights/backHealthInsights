package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.UserResponseDto;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetUsersUseCase {

    private final UserRepository userRepository;

    @Inject
    public GetUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ArrayList<User> execute() {
        return this.userRepository.findAllUsers();
    }
}
