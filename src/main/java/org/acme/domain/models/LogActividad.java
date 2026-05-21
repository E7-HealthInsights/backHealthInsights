package org.acme.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class LogActividad {
    private UUID id;
    private String accion;
    private String detalle;
    private EntidadTipo entidadTipo;
    private String entidadId;
    private LocalDateTime fecha;
    private String adminNombre;

    public LogActividad(UUID id, String accion, String detalle,
        EntidadTipo entidadTipo, String entidadId,
        LocalDateTime fecha, String adminNombre) {
    this.id = id;
    this.accion = accion;
    this.detalle = detalle;
    this.entidadTipo = entidadTipo;
    this.entidadId = entidadId;
    this.fecha = fecha;
    this.adminNombre = adminNombre;
    }

    // Constructor vacío — necesario para otros usos
    public LogActividad() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public EntidadTipo getEntidadTipo() { return entidadTipo; }
    public void setEntidadTipo(EntidadTipo entidadTipo) { this.entidadTipo = entidadTipo; }

    public String getEntidadId() { return entidadId; }
    public void setEntidadId(String entidadId) { this.entidadId = entidadId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getAdminNombre() { return adminNombre; }
    public void setAdminNombre(String adminNombre) { this.adminNombre = adminNombre; }
}
