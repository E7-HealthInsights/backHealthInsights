package org.acme.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateWidgetDto {
    @NotBlank(message = "El elemento debe tener un titulo")
    private String titulo;

    @NotNull(message = "El elemento debe ser de un tipo especifico")
    @Min(value = 1, message = "El tipo de elemento debe ser valido")
    private Byte tipoId;

    @NotNull
    private String queryConfig;  // JSON serializado como String

    private int orden;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Byte getTipoId() {
        return tipoId;
    }

    public void setTipoId(Byte tipoId) {
        this.tipoId = tipoId;
    }

    public String getQueryConfig() {
        return queryConfig;
    }

    public void setQueryConfig(String queryConfig) {
        this.queryConfig = queryConfig;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}
