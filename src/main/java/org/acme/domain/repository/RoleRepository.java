package org.acme.domain.repository;

import org.acme.domain.models.Role;

import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findRoleById(Byte id);
}
