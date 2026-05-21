package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.UpdateUserDto;
import org.acme.domain.exception.RoleNotFoundException;
import org.acme.domain.exception.UserNotFoundException;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.domain.repository.RoleRepository;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.UUID;

@ApplicationScoped
public class UpdateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthContext authContext;
    private final LogActividadRepository logActividadRepository;

    @Inject
    public UpdateUserUseCase(UserRepository userRepository, RoleRepository roleRepository, AuthContext authContext, LogActividadRepository logActividadRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authContext = authContext;
        this.logActividadRepository = logActividadRepository;
    }

    public User execute(UUID userId, UpdateUserDto dto) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (dto.getRoleId() != null) {
            Role role = roleRepository.findRoleById(dto.getRoleId())
                    .orElseThrow(() -> new RoleNotFoundException(dto.getRoleId()));
            user.setRole(role);
        }

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        user.setModifiedBy(authContext.getUser().getId().toString());

        // 1 — Actualiza usuario (trigger dispara → LogActividad creado)
        User updatedUser = userRepository.update(user);

        // 2 y 3 — Busca el log y actualiza detalle si hay justificación
        logActividadRepository
                .findLatestByEntidadId(updatedUser.getId().toString())
                .ifPresent(log -> {
                    if (dto.getJustification() != null
                            && !dto.getJustification().isBlank()) {
                        logActividadRepository.updateDetalle(
                                log.getId(),
                                dto.getJustification()
                        );
                    }
                });

        return updatedUser;
    }
}
