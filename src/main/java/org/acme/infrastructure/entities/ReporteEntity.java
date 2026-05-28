package org.acme.infrastructure.entities;

import jakarta.persistence.*;
import org.acme.domain.models.ReporteTipo;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Reporte")
public class ReporteEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;

    @Column(name = "titulo", nullable = false, length = 300)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private ReporteTipo tipo;

    @Column(name = "referencia_id", length = 100)
    private String referenciaId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public ReporteTipo getTipo() { return tipo; }
    public void setTipo(ReporteTipo tipo) { this.tipo = tipo; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
