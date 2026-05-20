package org.acme.infrastructure.mapper;

import org.acme.domain.models.User;
import org.acme.infrastructure.entities.UserEntity;
import org.hibernate.Hibernate;

public class UserMapper {
    public static User toDomain(UserEntity entity) {

        User user = new User();
        user.setId(entity.getId());
        user.setName(entity.getName());
        user.setLastName(entity.getLastName());
        user.setEmail(entity.getEmail());
        user.setStatus(entity.isStatus());
        user.setProviderId(entity.getProviderId());
        user.setModifiedBy(entity.getModifiedBy());

        // Solo mapeamos si la relacion esta inicializada por LazyLoading
        if(entity.getRole() != null && Hibernate.isInitialized(entity.getRole())){
            user.setRole(RoleMapper.toDomain(entity.getRole()));
        }
        return user;
    }

    public static UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setLastName(user.getLastName());
        entity.setEmail(user.getEmail());
        entity.setStatus(user.isStatus());
        entity.setProviderId(user.getProviderId());
        entity.setModifiedBy(user.getModifiedBy());
        entity.setRole(user.getRole() != null ? RoleMapper.toEntity(user.getRole()) : null);
        return entity;
    }
}
