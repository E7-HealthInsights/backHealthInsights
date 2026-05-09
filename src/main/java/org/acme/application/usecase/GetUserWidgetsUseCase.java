package org.acme.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.WidgetResponseDto;
import org.acme.domain.models.User;
import org.acme.domain.models.Widget;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.MetricaRepository;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.query.QueryExecutor;
import org.acme.infrastructure.security.AuthContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class GetUserWidgetsUseCase {

    private final WidgetRepository widgetRepository;
    private final AuthContext authContext;
    private final QueryExecutor queryExecutor;
    private final DatasetRepository datasetRepository;
    private final MetricaRepository metricaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public GetUserWidgetsUseCase(WidgetRepository widgetRepository, AuthContext authContext, QueryExecutor queryExecutor, DatasetRepository datasetRepository, MetricaRepository metricaRepository){
        this.widgetRepository = widgetRepository;
        this.authContext = authContext;
        this.queryExecutor = queryExecutor;
        this.datasetRepository = datasetRepository;
        this.metricaRepository = metricaRepository;
    }

    public List<WidgetResponseDto> execute() {
        List<Widget> todos = new ArrayList<>();
        todos.addAll(widgetRepository.findDefaultsByRolId(authContext.getUser().getRole().getId()));
        todos.addAll(widgetRepository.findByUserId(authContext.getUser().getId()));
        return todos.stream().map(this::toDto).toList();
    }

    private WidgetResponseDto toDto(Widget widget) {
        WidgetResponseDto dto = new WidgetResponseDto();
        dto.setId(widget.getId());
        dto.setTitulo(widget.getTitulo());
        dto.setOrden(widget.getOrden());

        String tipo = widget.getTipo() != null ? widget.getTipo().getNombre() : "STAT";
        dto.setTipo(tipo);

        // 1 — Ejecuta la query SIEMPRE primero
        Map<String, Object> data = new java.util.HashMap<>(
                queryExecutor.execute(widget.getQuery(), tipo)
        );
        dto.setData(data);

        try {
            JsonNode config = objectMapper.readTree(widget.getQuery());
            String nombreTabla = config.get("tabla").asText();

            datasetRepository.findByNombreTabla(nombreTabla).ifPresent(dataset -> {

                // 2 — Subtítulo
                dto.setSubtitulo("Fuente: " + dataset.getFuente());

                String columnaKey = tipo.equals("STAT") ? "columna" : "colY";

                if (config.has(columnaKey)) {
                    metricaRepository.findByColumnaCsvAndDatasetId(
                            config.get(columnaKey).asText(), dataset.getId()
                    ).ifPresent(metrica -> {
                        if (tipo.equals("STAT")) {
                            // 3a — Para STAT: agrega label al data ya ejecutado
                            if (metrica.getUnidad() != null) {
                                data.put("label", metrica.getUnidad());
                            }
                        } else {
                            // 3b — Para charts: seriesName y yAxisLabel
                            dto.setSeriesName(metrica.getNombre());
                            dto.setyAxisLabel(
                                    metrica.getUnidad() != null
                                            ? metrica.getNombre() + " (" + metrica.getUnidad() + ")"
                                            : metrica.getNombre()
                            );
                        }
                    });
                }

                // 4 — xAxisLabel solo para charts
                if (config.has("colX")) {
                    metricaRepository.findByColumnaCsvAndDatasetId(
                            config.get("colX").asText(), dataset.getId()
                    ).ifPresent(metrica -> dto.setxAxisLabel(metrica.getNombre()));
                }
            });

        } catch (Exception ignored) {}

        return dto;
    }
}
