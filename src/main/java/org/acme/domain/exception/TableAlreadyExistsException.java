package org.acme.domain.exception;

public class TableAlreadyExistsException extends RuntimeException {
    public TableAlreadyExistsException(String nombreTabla) {
        super("Ya existe un dataset con la tabla \"" + nombreTabla + "\". Usa un nombre de archivo diferente.");
    }
}
