package org.acme.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inputs que el usuario puede mandar para guiar la generación de la estrategia.
 * Todos los campos son opcionales: si vienen vacíos, la IA decide a partir del dashboard.
 */
public class GenerateStrategyRequestDto {

    private String contextoExtra;
    private List<String> zonasFoco;        // zonas/regiones donde concentrar
    private List<String> zonasExcluir;     // zonas que NO debe considerar
    private Integer edadMin;
    private Integer edadMax;
    private BigDecimal presupuestoMxn;
    private List<String> mediosPreferidos; // enum: digital, ooh, radio, tv, comunitario, prensa, salud, messaging
    private Integer horizonteMeses;        // 3, 6 ó 12
    private String tono;                   // educativo | urgente | esperanzador | motivacional

    public GenerateStrategyRequestDto() {}

    public String getContextoExtra() { return contextoExtra; }
    public void setContextoExtra(String contextoExtra) { this.contextoExtra = contextoExtra; }

    public List<String> getZonasFoco() { return zonasFoco; }
    public void setZonasFoco(List<String> zonasFoco) { this.zonasFoco = zonasFoco; }

    public List<String> getZonasExcluir() { return zonasExcluir; }
    public void setZonasExcluir(List<String> zonasExcluir) { this.zonasExcluir = zonasExcluir; }

    public Integer getEdadMin() { return edadMin; }
    public void setEdadMin(Integer edadMin) { this.edadMin = edadMin; }

    public Integer getEdadMax() { return edadMax; }
    public void setEdadMax(Integer edadMax) { this.edadMax = edadMax; }

    public BigDecimal getPresupuestoMxn() { return presupuestoMxn; }
    public void setPresupuestoMxn(BigDecimal presupuestoMxn) { this.presupuestoMxn = presupuestoMxn; }

    public List<String> getMediosPreferidos() { return mediosPreferidos; }
    public void setMediosPreferidos(List<String> mediosPreferidos) { this.mediosPreferidos = mediosPreferidos; }

    public Integer getHorizonteMeses() { return horizonteMeses; }
    public void setHorizonteMeses(Integer horizonteMeses) { this.horizonteMeses = horizonteMeses; }

    public String getTono() { return tono; }
    public void setTono(String tono) { this.tono = tono; }
}
