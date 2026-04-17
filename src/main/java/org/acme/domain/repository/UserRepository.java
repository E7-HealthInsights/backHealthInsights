package org.acme.domain.repository;

import org.acme.domain.models.User;

public interface UserRepository {

    User create(User user);
}
