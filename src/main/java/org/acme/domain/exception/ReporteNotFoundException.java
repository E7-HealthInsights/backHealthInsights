package org.acme.domain.exception;

import java.util.UUID;

public class ReporteNotFoundException extends RuntimeException {
    public ReporteNotFoundException(UUID id) {
        super("El reporte con id " + id + " no existe");
    }
}
