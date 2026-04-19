package org.acme.infrastructure.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Role;
import org.acme.domain.repository.RoleRepository;
import org.acme.domain.repository.UserRepository;
import org.acme.infrastructure.entities.RoleEntity;
import org.acme.infrastructure.entities.UserEntity;
import org.acme.infrastructure.mapper.RoleMapper;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoleRepositoryImpl implements RoleRepository, PanacheRepositoryBase<RoleEntity, Byte> {

    @Override
    public Optional<Role> findRoleById(Byte id) {
        Optional<RoleEntity> optionalRoleEntity = find("id", id).firstResultOptional();
        return optionalRoleEntity.map(this::map);
    }

    private Role map(RoleEntity roleEntity) {
        return RoleMapper.toDomain(roleEntity);
    }

}
