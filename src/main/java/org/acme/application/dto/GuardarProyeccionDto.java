package org.acme.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GuardarProyeccionDto {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    // JSON completo calculado por el frontend: params + puntos + kpis
    @NotNull(message = "El resultado es obligatorio")
    private String resultado;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
}