package org.acme.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class DatasetResponseDto {
    private UUID id;
    private String nombre;
    private String descripcion;
    private String fuente;
    private String link;
    private LocalDateTime fechaActualizacion;
    private String modifiedBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}