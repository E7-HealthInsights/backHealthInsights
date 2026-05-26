package org.acme.infrastructure.messaging;

import org.acme.application.dto.ColumnDefinitionDto;

import java.util.List;
import java.util.UUID;

/**
 * Payload del mensaje Kafka publicado al topic {@code dataset-csv-uploaded}.
 *
 * Contiene todo lo que el consumer necesita para ejecutar el ingest
 * sin tener que consultar la base de datos:
 *  - datasetId  → para actualizar el estado al terminar
 *  - nombreTabla → nombre de la tabla dinámica a crear en MySQL
 *  - gsObjectName → nombre del objeto en GCS (no la URI completa, para facilitar
 *                   operaciones de download y delete sin re-parsear)
 *  - columnas   → definición de columnas para el CREATE TABLE y el INSERT
 */
public class DatasetCsvUploadedEvent {

    private UUID datasetId;
    private String nombreTabla;
    private String gsObjectName;
    private List<ColumnDefinitionDto> columnas;

    public DatasetCsvUploadedEvent() {}

    public DatasetCsvUploadedEvent(
            UUID datasetId,
            String nombreTabla,
            String gsObjectName,
            List<ColumnDefinitionDto> columnas
    ) {
        this.datasetId    = datasetId;
        this.nombreTabla  = nombreTabla;
        this.gsObjectName = gsObjectName;
        this.columnas     = columnas;
    }

    public UUID getDatasetId() { return datasetId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }

    public String getNombreTabla() { return nombreTabla; }
    public void setNombreTabla(String nombreTabla) { this.nombreTabla = nombreTabla; }

    public String getGsObjectName() { return gsObjectName; }
    public void setGsObjectName(String gsObjectName) { this.gsObjectName = gsObjectName; }

    public List<ColumnDefinitionDto> getColumnas() { return columnas; }
    public void setColumnas(List<ColumnDefinitionDto> columnas) { this.columnas = columnas; }
}
