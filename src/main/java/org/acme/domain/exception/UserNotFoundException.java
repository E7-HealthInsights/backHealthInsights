package org.acme.domain.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID userId) {
        super("El usuario con id " + userId + " no existe");
    }
}
