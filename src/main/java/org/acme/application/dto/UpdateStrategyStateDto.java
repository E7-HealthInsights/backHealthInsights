package org.acme.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateStrategyStateDto {

    @NotBlank(message = "estado es obligatorio")
    @Pattern(
            regexp = "^(propuesta|ejecutada|descartada)$",
            message = "estado debe ser propuesta, ejecutada o descartada"
    )
    private String estado;

    private String notaResultado;

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNotaResultado() { return notaResultado; }
    public void setNotaResultado(String notaResultado) { this.notaResultado = notaResultado; }
}
