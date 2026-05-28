package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.acme.application.dto.PaginadoResponseDto;
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

    public PaginadoResponseDto<User> execute(int page, int size, String search, boolean status) {
        List<User> data  = userRepository.findPaginated(page, size, search, status);
        long total       = userRepository.countUsers(search, status);
        return new PaginadoResponseDto<>(data, total, page, size);
    }
}
