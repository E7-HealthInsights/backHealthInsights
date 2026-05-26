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
    private DatasetEstado estado;
    private String errorMensaje;
    private LocalDateTime fechaActualizacion;
    private String modifiedBy;

    // Constructor legacy — usa DatasetEstado en lugar del boolean original
    public Dataset(UUID id, String nombre, String nombreTabla, String descripcion,
                   String fuente, String archivoCsv, String link,
                   DatasetEstado estado, LocalDateTime fechaActualizacion) {
        this.id = id;
        this.nombre = nombre;
        this.nombreTabla = nombreTabla;
        this.descripcion = descripcion;
        this.fuente = fuente;
        this.archivoCsv = archivoCsv;
        this.link = link;
        this.estado = estado;
        this.fechaActualizacion = fechaActualizacion;
    }

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

    public DatasetEstado getEstado() { return estado; }
    public void setEstado(DatasetEstado estado) { this.estado = estado; }

    public String getErrorMensaje() { return errorMensaje; }
    public void setErrorMensaje(String errorMensaje) { this.errorMensaje = errorMensaje; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
