package org.acme.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Reporte {
    private UUID id;
    private UUID usuarioId;
    private String titulo;
    private ReporteTipo tipo;
    private String referenciaId;
    private LocalDateTime fechaCreacion;

    public Reporte() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public ReporteTipo getTipo() { return tipo; }
    public void setTipo(ReporteTipo tipo) { this.tipo = tipo; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
