package org.acme.domain.models;

/**
 * Estado del ciclo de vida de un Dataset.
 *
 * <pre>
 *   PENDING    → se persistió el registro pero el CSV aún no fue procesado
 *                (el mensaje Kafka fue publicado, esperando al consumer)
 *   PROCESSING → el consumer tomó el mensaje y está ejecutando el ingest
 *   READY      → tabla dinámica creada e inserción completada con éxito
 *   ERROR      → el ingest falló; el campo errorMensaje contiene el detalle
 * </pre>
 */
public enum DatasetEstado {
    PENDING,
    PROCESSING,
    READY,
    ERROR,
    INACTIVE
}
