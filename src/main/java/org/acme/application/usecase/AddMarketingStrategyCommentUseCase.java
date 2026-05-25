package org.acme.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.acme.application.dto.AddCommentDto;
import org.acme.application.dto.MarketingStrategyDto;
import org.acme.application.dto.MarketingStrategyDtoFactory;
import org.acme.domain.models.MarketingStrategy;
import org.acme.domain.repository.MarketingStrategyRepository;
import org.acme.infrastructure.security.AuthContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AddMarketingStrategyCommentUseCase {

    @Inject MarketingStrategyRepository repository;
    @Inject AuthContext authContext;
    @Inject MarketingStrategyDtoFactory dtoFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarketingStrategyDto execute(UUID strategyId, AddCommentDto request) {
        if (request == null || request.getContenido() == null || request.getContenido().isBlank()) {
            throw new BadRequestException("El comentario no puede estar vacío.");
        }

        MarketingStrategy strategy = repository.findOneById(strategyId)
                .orElseThrow(() -> new NotFoundException("Estrategia no encontrada."));

        UUID currentUser = authContext.getUser().getId();
        if (!strategy.getUsuarioId().equals(currentUser)) {
            throw new ForbiddenException("Esta estrategia pertenece a otro usuario.");
        }

        List<Map<String, Object>> comentarios = parseComentarios(strategy.getComentariosJson());

        Map<String, Object> nuevo = new LinkedHashMap<>();
        nuevo.put("id", UUID.randomUUID().toString());
        nuevo.put("contenido", request.getContenido().trim());
        nuevo.put("creadoEn", LocalDateTime.now().toString());
        comentarios.add(nuevo);

        try {
            strategy.setComentariosJson(objectMapper.writeValueAsString(comentarios));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar los comentarios.", e);
        }

        MarketingStrategy updated = repository.update(strategy);
        return dtoFactory.from(updated);
    }

    private List<Map<String, Object>> parseComentarios(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
