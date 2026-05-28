package org.acme.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CreateWidgetDto {
    @NotBlank(message = "El elemento debe tener un titulo")
    private String titulo;

    @NotNull(message = "El elemento debe ser de un tipo especifico")
    @Min(value = 1, message = "El tipo de elemento debe ser valido")
    private Byte tipoId;

    @NotNull
    private String queryConfig;  // JSON serializado como String

    private int orden;

    @Pattern(
            regexp = "^(porcentaje|conteo|tasa|moneda|indice|texto)?$",
            message = "tipoSemantico debe ser porcentaje, conteo, tasa, moneda, indice o texto"
    )
    private String tipoSemantico;

    @Pattern(
            regexp = "^(pais|estado|municipio|colonia|sin_geo)?$",
            message = "nivelGeografico debe ser pais, estado, municipio, colonia o sin_geo"
    )
    private String nivelGeografico;

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

    public String getTipoSemantico() {
        return tipoSemantico;
    }

    public void setTipoSemantico(String tipoSemantico) {
        this.tipoSemantico = tipoSemantico;
    }

    public String getNivelGeografico() {
        return nivelGeografico;
    }

    public void setNivelGeografico(String nivelGeografico) {
        this.nivelGeografico = nivelGeografico;
    }
}
