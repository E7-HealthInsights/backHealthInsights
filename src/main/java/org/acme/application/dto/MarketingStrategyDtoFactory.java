package org.acme.application.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domain.models.MarketingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MarketingStrategyDtoFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarketingStrategyDto from(MarketingStrategy strategy) {
        MarketingStrategyDto dto = new MarketingStrategyDto();
        dto.setId(strategy.getId());
        dto.setUsuarioId(strategy.getUsuarioId());
        dto.setCreadoEn(strategy.getCreadoEn());
        dto.setEstado(strategy.getEstado() != null ? strategy.getEstado() : MarketingStrategy.ESTADO_PROPUESTA);
        dto.setNotaResultado(strategy.getNotaResultado());
        dto.setFechaRevision(strategy.getFechaRevision());

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    strategy.getPayloadJson(), new TypeReference<Map<String, Object>>() {});
            dto.setPayload(payload);
        } catch (Exception e) {
            dto.setPayload(Map.of("error", "Estrategia almacenada en formato inválido."));
        }

        if (strategy.getComentariosJson() != null && !strategy.getComentariosJson().isBlank()) {
            try {
                List<Map<String, Object>> comentarios = objectMapper.readValue(
                        strategy.getComentariosJson(), new TypeReference<List<Map<String, Object>>>() {});
                dto.setComentarios(comentarios);
            } catch (Exception e) {
                dto.setComentarios(new ArrayList<>());
            }
        }

        return dto;
    }
}
