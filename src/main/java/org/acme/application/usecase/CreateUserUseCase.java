package org.acme.application.usecase;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.CreateUserDto;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.RoleRepository;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.firebase.FirebaseUserCreator;
import java.util.UUID;
import org.acme.domain.exception.EmailAlreadyExistsException;
import org.acme.domain.exception.RoleNotFoundException;
import com.google.firebase.auth.FirebaseAuthException;

@ApplicationScoped
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FirebaseUserCreator firebaseUserCreator;

    @Inject
    public CreateUserUseCase(UserRepository userRepository, FirebaseUserCreator firebaseUserCreator, RoleRepository roleRepository){
        this.userRepository = userRepository;
        this.firebaseUserCreator = firebaseUserCreator;
        this.roleRepository = roleRepository;
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

        return userRepository.create(user);
    }
}
