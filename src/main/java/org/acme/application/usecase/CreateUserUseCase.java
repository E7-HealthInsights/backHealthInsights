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

        Role role = roleRepository.findRoleById(createUserDto.getRoleId())   //bucar rol por id, aunque el dto solo tiene el id, el user necesita el objeto completo, entonces lo busco en la base de datos, y si no lo encuentro lanzo una excepcion
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontradi"));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(createUserDto.getName());
        user.setLastName(createUserDto.getLastName());
        user.setEmail(createUserDto.getEmail());
        user.setStatus(true);
        user.setRole(role);

        UserRecord firebaseUserRecord = firebaseUserCreator.create(user.getEmail(), createUserDto.getPassword());
        user.setProviderId(firebaseUserRecord.getUid());
        return userRepository.create(user);
    }
}
