package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.DeactivateUserDto;
import org.acme.domain.exception.UserNotFoundException;
import org.acme.domain.models.User;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.UUID;

@ApplicationScoped
public class DeactivateUserUseCase {

    private final UserRepository userRepository;
    private final AuthContext authContext;
    private final LogActividadRepository logActividadRepository;

    @Inject
    public DeactivateUserUseCase(UserRepository userRepository, AuthContext authContext, LogActividadRepository logActividadRepository) {
        this.userRepository = userRepository;
        this.authContext = authContext;
        this.logActividadRepository = logActividadRepository;
    }

    public void execute(UUID userId, DeactivateUserDto dto) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setStatus(false);
        user.setModifiedBy(authContext.getUser().getId().toString());

        // 1 — Actualiza usuario (trigger dispara → LogActividad creado)
        userRepository.update(user);

        // 2 y 3 — Busca el log y actualiza detalle si hay justificación
        if (dto != null) {
            logActividadRepository
                    .findLatestByEntidadId(userId.toString())
                    .ifPresent(log -> {
                        if (dto.getJustification() != null
                                && !dto.getJustification().isBlank()) {
                            logActividadRepository.updateDetalle(
                                    log.getId(),
                                    dto.getJustification()
                            );
                        }
                    });
        }
    }
}
