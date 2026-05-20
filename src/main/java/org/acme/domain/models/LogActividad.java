package org.acme.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class LogActividad {
    private UUID id;
    private String usuarioId;
    private String accion;
    private String detalle;
    private EntidadTipo entidadTipo;
    private String entidadId;
    private LocalDateTime fecha;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

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
}
