package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.UserResponseDto;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetUsersUseCase {

    private final UserRepository userRepository;

    @Inject
    public GetUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDto> execute() {
        return userRepository.findAllUsers()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private UserResponseDto toDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getName());
        dto.setStatus(user.isStatus());
        return dto;
    }
}
