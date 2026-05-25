package org.acme.application.usecase;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.CreateUserDto;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.LogActividadRepository;
import org.acme.domain.repository.RoleRepository;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.firebase.FirebaseUserCreator;
import org.acme.infrastructure.security.AuthContext;

import java.util.UUID;
import org.acme.domain.exception.EmailAlreadyExistsException;
import org.acme.domain.exception.RoleNotFoundException;
import com.google.firebase.auth.FirebaseAuthException;

@ApplicationScoped
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FirebaseUserCreator firebaseUserCreator;
    private final AuthContext authContext;
    private final LogActividadRepository logActividadRepository;

    @Inject
    public CreateUserUseCase(UserRepository userRepository, FirebaseUserCreator firebaseUserCreator, RoleRepository roleRepository, AuthContext authContext, LogActividadRepository logActividadRepository){
        this.userRepository = userRepository;
        this.firebaseUserCreator = firebaseUserCreator;
        this.roleRepository = roleRepository;
        this.authContext = authContext;
        this.logActividadRepository = logActividadRepository;
    }

    public User execute(CreateUserDto createUserDto) throws FirebaseAuthException {

        // Validar rol
        Role role = roleRepository.findRoleById(createUserDto.getRoleId())
                .orElseThrow(() -> new RoleNotFoundException(createUserDto.getRoleId()));

        // Validar email duplicado en BD antes de ir a Firebase
        if (userRepository.existsByEmail(createUserDto.getEmail())) {
            throw new EmailAlreadyExistsException(createUserDto.getEmail());
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(createUserDto.getName());
        user.setLastName(createUserDto.getLastName());
        user.setEmail(createUserDto.getEmail());
        user.setStatus(true);
        user.setRole(role);
        // user.setModifiedBy(authContext.getUser().getId().toString());
        user.setModifiedBy(authContext.getUser().getId().toString());


        try {
            UserRecord firebaseUserRecord = firebaseUserCreator.create(user.getEmail(), createUserDto.getPassword());
            user.setProviderId(firebaseUserRecord.getUid());
        } catch (FirebaseAuthException e) {
            // Firebase también puede detectar el email duplicado
            if (e.getAuthErrorCode() != null &&
                    e.getAuthErrorCode().name().equals("EMAIL_ALREADY_EXISTS")) {
                throw new EmailAlreadyExistsException(createUserDto.getEmail());
            }
            throw e; // cualquier otro error de Firebase sí es un 500
        }

        // 1 — Persiste usuario (trigger dispara → LogActividad creado con detalle=NULL)
        User savedUser = userRepository.create(user);

        // 2 — Busca el log recién creado por el trigger
        // Filtra por entidad_id del usuario creado → UUID único, sin riesgo de colisión
        logActividadRepository
                .findLatestByEntidadId(savedUser.getId().toString())
                .ifPresent(log -> {
                    // 3 — Actualiza detalle solo si el admin escribió justificación
                    if (createUserDto.getJustification() != null
                            && !createUserDto.getJustification().isBlank()) {
                        logActividadRepository.updateDetalle(
                                log.getId(),
                                createUserDto.getJustification()
                        );
                    }
                });

        return savedUser;
    }
}
