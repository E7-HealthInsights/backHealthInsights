package org.acme.application.dto;

import org.acme.domain.models.EntidadTipo;

import java.time.LocalDateTime;
import java.util.UUID;

public class LogActividadResponseDto {
    private UUID id;
    private String adminNombre;
    private String accion;
    private String detalle;
    private EntidadTipo entidadTipo;
    private String entidadId;
    private LocalDateTime fecha;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAdminNombre() { return adminNombre; }
    public void setAdminNombre(String adminNombre) { this.adminNombre = adminNombre; }

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
