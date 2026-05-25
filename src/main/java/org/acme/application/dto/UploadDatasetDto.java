package org.acme.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO recibido como JSON puro (application/json).
 * El archivo CSV viaja como string base64 en el campo {@code archivoCsvBase64}.
 */
public class UploadDatasetDto {

    @NotBlank(message = "El nombre del dataset es obligatorio")
    @Size(max = 300, message = "El nombre no debe exceder 300 caracteres")
    private String nombre;

    @Size(max = 5000, message = "La descripción no debe exceder 5000 caracteres")
    private String descripcion;

    @Size(max = 300, message = "La fuente no debe exceder 300 caracteres")
    private String fuente;

    @NotBlank(message = "El nombre del archivo es obligatorio")
    private String archivoNombre;

    /** Contenido del CSV codificado en Base64. */
    @NotBlank(message = "El archivo CSV es obligatorio")
    private String archivoCsvBase64;

    @NotEmpty(message = "Debe definir al menos una columna")
    @Valid
    private List<ColumnDefinitionDto> columnas;

    private String modifiedBy;

    private String justification;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public String getArchivoNombre() { return archivoNombre; }
    public void setArchivoNombre(String archivoNombre) { this.archivoNombre = archivoNombre; }

    public String getArchivoCsvBase64() { return archivoCsvBase64; }
    public void setArchivoCsvBase64(String archivoCsvBase64) { this.archivoCsvBase64 = archivoCsvBase64; }

    public List<ColumnDefinitionDto> getColumnas() { return columnas; }
    public void setColumnas(List<ColumnDefinitionDto> columnas) { this.columnas = columnas; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
}
