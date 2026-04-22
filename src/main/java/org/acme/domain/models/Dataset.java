package org.acme.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Dataset {
    private UUID id;
    private String nombre;
    private String nombreTabla;
    private String descripcion;
    private String fuente;
    private String archivoCsv;
    private String link;
    private boolean estado;
    private LocalDateTime fechaActualizacion;

    public Dataset() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNombreTabla() { return nombreTabla; }
    public void setNombreTabla(String nombreTabla) { this.nombreTabla = nombreTabla; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public String getArchivoCsv() { return archivoCsv; }
    public void setArchivoCsv(String archivoCsv) { this.archivoCsv = archivoCsv; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}