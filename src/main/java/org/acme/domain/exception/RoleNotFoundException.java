package org.acme.domain.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(Byte roleId) {
        super("El rol con id " + roleId + " no existe");
    }
}