package org.acme.application.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Estrategia de mercadotecnia generada por IA, lista para enviar al frontend.
 * Los campos `resumen_ejecutivo`, `prioridades`, `segmentos_objetivo`, `campanias`,
 * `riesgos` y `proxima_revision_dias` viven dentro de `payload` y siguen el JSON Schema
 * forzado al modelo (ver GenerateMarketingStrategyUseCase.SCHEMA_JSON).
 */
public class MarketingStrategyDto {

    private UUID id;
    private UUID usuarioId;
    private LocalDateTime creadoEn;
    private Map<String, Object> payload;
    private String estado;
    private String notaResultado;
    private LocalDateTime fechaRevision;
    private List<Map<String, Object>> comentarios;

    public MarketingStrategyDto() {
        this.payload = new LinkedHashMap<>();
        this.comentarios = new ArrayList<>();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNotaResultado() { return notaResultado; }
    public void setNotaResultado(String notaResultado) { this.notaResultado = notaResultado; }

    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }

    public List<Map<String, Object>> getComentarios() { return comentarios; }
    public void setComentarios(List<Map<String, Object>> comentarios) { this.comentarios = comentarios; }
}
