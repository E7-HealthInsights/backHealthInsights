// ProyeccionNotFoundException.java
package org.acme.domain.exception;

import java.util.UUID;

public class ProyeccionNotFoundException extends RuntimeException {
    public ProyeccionNotFoundException(UUID id) {
        super("Proyección no encontrada: " + id);
    }
}