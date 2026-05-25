package org.acme.application.dto;

import org.acme.domain.models.ReporteTipo;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReporteResponseDto {
    private UUID id;
    private String titulo;
    private ReporteTipo tipo;
    private String referenciaId;
    private LocalDateTime fechaCreacion;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public ReporteTipo getTipo() { return tipo; }
    public void setTipo(ReporteTipo tipo) { this.tipo = tipo; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
