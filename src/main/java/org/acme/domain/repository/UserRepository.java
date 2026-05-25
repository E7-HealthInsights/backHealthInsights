package org.acme.domain.repository;

import org.acme.domain.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User create(User user);
    Optional<User> findByFirebaseUuid(String firebaseUuid);
    Optional<User> findUserById(UUID id);
    boolean existsByEmail(String email);
    ArrayList<User> findAllUsers();
    User update(User user);
    List<User> findPaginated(int page, int size, String search, Boolean status);
    long countUsers(String search, Boolean status);
}
