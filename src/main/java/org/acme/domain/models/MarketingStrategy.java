package org.acme.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class MarketingStrategy {
    public static final String ESTADO_PROPUESTA  = "propuesta";
    public static final String ESTADO_EJECUTADA  = "ejecutada";
    public static final String ESTADO_DESCARTADA = "descartada";

    private UUID id;
    private UUID usuarioId;
    private LocalDateTime creadoEn;
    private String contextoExtra;
    private String payloadJson;
    private String estado;
    private String notaResultado;
    private LocalDateTime fechaRevision;
    private String comentariosJson;   // JSON array de {id, contenido, creadoEn}

    public MarketingStrategy() {
    }

    public MarketingStrategy(UUID id, UUID usuarioId, LocalDateTime creadoEn, String contextoExtra, String payloadJson) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.creadoEn = creadoEn;
        this.contextoExtra = contextoExtra;
        this.payloadJson = payloadJson;
        this.estado = ESTADO_PROPUESTA;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getContextoExtra() { return contextoExtra; }
    public void setContextoExtra(String contextoExtra) { this.contextoExtra = contextoExtra; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNotaResultado() { return notaResultado; }
    public void setNotaResultado(String notaResultado) { this.notaResultado = notaResultado; }

    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }

    public String getComentariosJson() { return comentariosJson; }
    public void setComentariosJson(String comentariosJson) { this.comentariosJson = comentariosJson; }

    public static boolean isEstadoValido(String estado) {
        return ESTADO_PROPUESTA.equals(estado)
            || ESTADO_EJECUTADA.equals(estado)
            || ESTADO_DESCARTADA.equals(estado);
    }
}
