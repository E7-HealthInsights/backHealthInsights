package org.acme.domain.exception;

import java.util.UUID;

public class DatasetNotFoundException extends RuntimeException {
    public DatasetNotFoundException(UUID datasetId) {
        super("El dataset con id " + datasetId + " no existe");
    }
}
