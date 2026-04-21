package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.entities.UserEntity;
import org.acme.infrastructure.mapper.UserMapper;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepositoryImpl implements UserRepository, PanacheRepositoryBase<UserEntity, UUID> {

    @Override
    @Transactional
    public User create(User user){
        UserEntity userEntity = UserMapper.toEntity(user);
        persist(userEntity);   //metodo de PanacheRepositoryBase para guardar en la base
        return UserMapper.toDomain(userEntity);
    }

    @Override
    public Optional<User> findByFirebaseUuid(String firebaseUuid) {
        Optional<UserEntity> optionalUserEntity = find("providerId", firebaseUuid)
                .withHint("jakarta.persistence.loadgraph", getEntityManager()
                        .getEntityGraph("User.full"))
                .firstResultOptional();
        return optionalUserEntity.map(this::map);
    }

    private User map(UserEntity userEntity) { return UserMapper.toDomain(userEntity); }

    @Override
    public boolean existsByEmail(String email) {
        return find("email", email).firstResultOptional().isPresent();
    }
}
