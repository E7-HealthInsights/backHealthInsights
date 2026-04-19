package org.acme.infrastructure.mapper;

import org.acme.domain.models.Role;
import org.acme.infrastructure.entities.RoleEntity;

public class RoleMapper {
    public static Role toDomain(RoleEntity entity) {
        Role role = new Role();
        role.setId(entity.getId());
        role.setName(entity.getName());
        return role;
    }

    public static RoleEntity toEntity(Role role) {
        RoleEntity entity = new RoleEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        return entity;
    }
}
