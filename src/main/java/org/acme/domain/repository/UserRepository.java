package org.acme.domain.repository;

import org.acme.domain.models.User;

import java.util.Optional;

public interface UserRepository {
    User create(User user);
    Optional<User> findByFirebaseUuid(String firebaseUuid);
    boolean existsByEmail(String email); // ← nuevo
}
