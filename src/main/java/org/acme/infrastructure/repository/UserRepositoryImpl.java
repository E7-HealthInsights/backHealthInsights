package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.entities.UserEntity;
import org.acme.infrastructure.entities.RoleEntity;
import org.acme.infrastructure.mapper.UserMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserRepositoryImpl implements UserRepository, PanacheRepositoryBase<UserEntity, UUID> {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public User create(User user){
        UserEntity userEntity = UserMapper.toEntity(user);
        persist(userEntity);   //metodo de PanacheRepositoryBase para guardar en la base
        return UserMapper.toDomain(userEntity);
    }

    @Override
    @Transactional
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

    @Override
    public ArrayList<User> findAllUsers() {
        List<UserEntity> entities = em.createQuery("SELECT u FROM UserEntity u", UserEntity.class)
                .setHint("jakarta.persistence.fetchgraph", em.getEntityGraph("User.full"))
                .getResultList();

        return entities.stream()
                .map(UserMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public Optional<User> findUserById(UUID id) {
        Optional<UserEntity> optEntity = find("id", id)
                .withHint("jakarta.persistence.loadgraph", getEntityManager().getEntityGraph("User.full"))
                .firstResultOptional();
        return optEntity.map(UserMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteUserById(UUID id) {
        deleteById(id);
    }

    @Override
    @Transactional
    public User update(User user) {
        UserEntity entity = em.find(UserEntity.class, user.getId());
        entity.setName(user.getName());
        entity.setLastName(user.getLastName());
        entity.setStatus(user.isStatus());
        if (user.getRole() != null) {
            entity.setRole(em.getReference(RoleEntity.class, user.getRole().getId()));
        }
        return user;
    }
}
