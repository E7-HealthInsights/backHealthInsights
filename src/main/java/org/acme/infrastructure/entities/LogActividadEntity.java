package org.acme.infrastructure.entities;

import jakarta.persistence.*;
import org.acme.domain.models.EntidadTipo;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "LogActividad")
public class LogActividadEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "usuario_id", length = 36)
    private String usuarioId;

    @Column(name = "accion", nullable = false, length = 200)
    private String accion;

    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidad_tipo", nullable = false)
    private EntidadTipo entidadTipo;

    @Column(name = "entidad_id", nullable = false, length = 36)
    private String entidadId;

    @Column(name = "fecha", nullable = false)
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
