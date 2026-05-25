package org.acme.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.acme.domain.models.ReporteTipo;

public class CreateReporteDto {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 300, message = "El título no debe exceder 300 caracteres")
    private String titulo;

    @NotNull(message = "El tipo es obligatorio")
    private ReporteTipo tipo;

    private String referenciaId;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public ReporteTipo getTipo() { return tipo; }
    public void setTipo(ReporteTipo tipo) { this.tipo = tipo; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }
}
