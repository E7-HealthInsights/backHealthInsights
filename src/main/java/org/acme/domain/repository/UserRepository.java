package org.acme.domain.repository;

import org.acme.domain.models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User create(User user);
    Optional<User> findByFirebaseUuid(String firebaseUuid);
    boolean existsByEmail(String email);
    List<User> findAllUsers();
}
