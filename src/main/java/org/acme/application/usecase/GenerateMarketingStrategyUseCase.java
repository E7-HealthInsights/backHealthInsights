package org.acme.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.GenerateStrategyRequestDto;
import org.acme.application.dto.MarketingStrategyDto;
import org.acme.application.dto.MarketingStrategyDtoFactory;
import org.acme.application.dto.WidgetResponseDto;
import org.acme.domain.models.MarketingStrategy;
import org.acme.domain.repository.MarketingStrategyRepository;
import org.acme.infrastructure.openai.DashboardSummarizer;
import org.acme.infrastructure.openai.OpenAIClient;
import org.acme.infrastructure.security.AuthContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class GenerateMarketingStrategyUseCase {

    @Inject GetUserWidgetsUseCase getUserWidgetsUseCase;
    @Inject DashboardSummarizer summarizer;
    @Inject OpenAIClient openAIClient;
    @Inject MarketingStrategyRepository repository;
    @Inject AuthContext authContext;
    @Inject MarketingStrategyDtoFactory dtoFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            Eres un consultor senior de mercadotecnia en salud pública en México con experiencia en \
            campañas regionales de prevención y detección de enfermedades crónicas. \
            A partir de los indicadores del dashboard del Director de Mercadotecnia y de las \
            restricciones que indique el usuario, produces una estrategia accionable, específica y \
            basada en evidencia.

            Reglas estrictas:
            - Responde SIEMPRE en español de México.
            - Usa SOLO las zonas que aparezcan en los datos del dashboard o en `zonasFoco` del usuario. No inventes regiones.
            - Si el usuario provee `zonasExcluir`, NO las menciones en prioridades ni en campañas.
            - Para cada campaña, propón entre 2 y 4 tácticas distintas con `tipo` concreto del enum permitido. Cada táctica debe estar adaptada a la zona y al rango de edad del segmento objetivo.
            - Si el usuario indicó `mediosPreferidos`, restringe el `tipo` de tácticas y el `tipo` de canales a esa lista (mapea de forma natural: ooh→espectacular/volantes, digital→red_social/influencer/email, messaging→sms_whatsapp, salud→evento/salud, prensa→prensa/podcast).
            - Si hay `presupuestoMxn`, distribuye el `presupuesto_pct` entre las tácticas de cada campaña (suma ≤ 100 por campaña, no necesariamente 100). Si no hay presupuesto, usa los pct para indicar peso relativo entre tácticas.
            - Si el usuario indicó `horizonteMeses`, escala `meta_3_meses` y el `cronograma` en consecuencia (mantén el campo `meta_3_meses` con el nombre pero ajusta el horizonte temporal del valor).
            - Si el usuario indicó `tono`, ajusta el `mensaje_clave` y el `resumen_ejecutivo` a ese tono.
            - El campo `contexto_analizado` debe reflejar exactamente qué inputs interpretaste del usuario: si vinieron vacíos, indícalo (ej. "todas las zonas", "todas las edades", `presupuesto_mxn: null`).
            - Las metas a 3 meses deben ser cuantitativas y realistas (porcentajes, números absolutos, alcance estimado).
            - Los canales recomendados deben ser medios concretos identificables (ej. radio "XEW 900AM", red social "Facebook páginas locales", centro de salud "IMSS módulos zona oriente"), no genéricos como "redes sociales" a secas.
            - TRAZABILIDAD OBLIGATORIA: cada elemento de `prioridades` y de `segmentos_objetivo` debe incluir el campo `basado_en`, citando 1 a 3 widgets del dashboard que sustentan esa recomendación. Cada cita usa el `widget_id` exacto y un `dato_observado` que parafrasee el dato concreto (con el número observado, la zona y la tendencia si aplica).
            - Cuando interpretes los datos del dashboard, respeta el `tipo_semantico` y `nivel_geografico` de cada widget si vienen indicados. Por ejemplo: porcentaje = valor 0–100, conteo = casos absolutos, tasa = relativa a población. No mezcles niveles geográficos: si las zonas son estados, no las trates como municipios.
            - Si en el contexto vienen `estrategias_pasadas` con `nota_resultado` o `comentarios`, úsalas como aprendizaje: refuerza lo que funcionó, evita repetir lo que se marcó como fallido, e incorpora el feedback explícito del usuario. Si una estrategia anterior fue `descartada`, NO repitas sus campañas ni sus tácticas tal cual.
            - Devuelve EXACTAMENTE el esquema JSON solicitado, sin texto adicional.
            """;

    private static final String SCHEMA_JSON = """
            {
              "type": "object",
              "additionalProperties": false,
              "required": [
                "contexto_analizado","resumen_ejecutivo","prioridades","segmentos_objetivo",
                "campanias","cronograma","oportunidades","riesgos","proxima_revision_dias"
              ],
              "properties": {
                "contexto_analizado": {
                  "type": "object",
                  "additionalProperties": false,
                  "required": ["zonas_analizadas","edades_consideradas","presupuesto_mxn","horizonte_meses","tono"],
                  "properties": {
                    "zonas_analizadas":    { "type": "array", "items": { "type": "string" } },
                    "edades_consideradas": { "type": "string" },
                    "presupuesto_mxn":     { "type": ["number","null"] },
                    "horizonte_meses":     { "type": "integer" },
                    "tono":                { "type": "string" }
                  }
                },
                "resumen_ejecutivo": { "type": "string" },
                "prioridades": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": ["zona","razon","severidad","poblacion_afectada","basado_en"],
                    "properties": {
                      "zona":               { "type": "string" },
                      "razon":              { "type": "string" },
                      "severidad":          { "type": "string", "enum": ["alta","media","baja"] },
                      "poblacion_afectada": { "type": "string" },
                      "basado_en": {
                        "type": "array",
                        "minItems": 1,
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "required": ["widget_id","dato_observado"],
                          "properties": {
                            "widget_id":      { "type": "string" },
                            "dato_observado": { "type": "string" }
                          }
                        }
                      }
                    }
                  }
                },
                "segmentos_objetivo": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": ["nombre","perfil","tamano_estimado","insight_clave","canales_recomendados","basado_en"],
                    "properties": {
                      "nombre":          { "type": "string" },
                      "perfil":          { "type": "string" },
                      "tamano_estimado": { "type": "string" },
                      "insight_clave":   { "type": "string" },
                      "basado_en": {
                        "type": "array",
                        "minItems": 1,
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "required": ["widget_id","dato_observado"],
                          "properties": {
                            "widget_id":      { "type": "string" },
                            "dato_observado": { "type": "string" }
                          }
                        }
                      },
                      "canales_recomendados": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "required": ["medio","tipo","razon"],
                          "properties": {
                            "medio": { "type": "string" },
                            "tipo":  { "type": "string", "enum": ["digital","ooh","radio","tv","comunitario","prensa","salud","messaging"] },
                            "razon": { "type": "string" }
                          }
                        }
                      }
                    }
                  }
                },
                "campanias": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": [
                      "titulo","objetivo","mensaje_clave","audiencia_objetivo","zonas",
                      "tacticas","kpi_principal","meta_3_meses","kpis_secundarios"
                    ],
                    "properties": {
                      "titulo":             { "type": "string" },
                      "objetivo":           { "type": "string" },
                      "mensaje_clave":      { "type": "string" },
                      "audiencia_objetivo": { "type": "string" },
                      "zonas":              { "type": "array", "items": { "type": "string" } },
                      "tacticas": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "required": ["tipo","descripcion","frecuencia","presupuesto_pct"],
                          "properties": {
                            "tipo": {
                              "type": "string",
                              "enum": ["espectacular","red_social","radio","tv","volantes","evento","salud","influencer","sms_whatsapp","prensa","podcast","email"]
                            },
                            "descripcion":     { "type": "string" },
                            "frecuencia":      { "type": "string" },
                            "presupuesto_pct": { "type": "integer", "minimum": 0, "maximum": 100 }
                          }
                        }
                      },
                      "kpi_principal":    { "type": "string" },
                      "meta_3_meses":     { "type": "string" },
                      "kpis_secundarios": { "type": "array", "items": { "type": "string" } }
                    }
                  }
                },
                "cronograma": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": ["mes","hito"],
                    "properties": {
                      "mes":  { "type": "integer", "minimum": 1, "maximum": 12 },
                      "hito": { "type": "string" }
                    }
                  }
                },
                "oportunidades":         { "type": "array", "items": { "type": "string" } },
                "riesgos":               { "type": "array", "items": { "type": "string" } },
                "proxima_revision_dias": { "type": "integer", "minimum": 7, "maximum": 365 }
              }
            }
            """;

    public MarketingStrategyDto execute(GenerateStrategyRequestDto request) {
        UUID usuarioId = authContext.getUser().getId();

        List<WidgetResponseDto> widgets = getUserWidgetsUseCase.execute();
        List<Map<String, Object>> summary = summarizer.summarize(widgets);

        List<Map<String, Object>> estrategiasPasadas = buildPastStrategiesContext(usuarioId);

        String userPrompt = buildUserPrompt(summary, estrategiasPasadas, request);

        JsonNode schema;
        try {
            schema = objectMapper.readTree(SCHEMA_JSON);
        } catch (Exception e) {
            throw new IllegalStateException("Schema JSON malformado en GenerateMarketingStrategyUseCase", e);
        }

        JsonNode aiResult = openAIClient.chatCompletionJsonSchema(
                SYSTEM_PROMPT, userPrompt, "marketing_strategy", schema);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(aiResult);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar la respuesta de OpenAI.", e);
        }

        MarketingStrategy strategy = new MarketingStrategy(
                UUID.randomUUID(),
                usuarioId,
                LocalDateTime.now(),
                request != null ? request.getContextoExtra() : null,
                payloadJson
        );
        MarketingStrategy saved = repository.create(strategy);

        return dtoFactory.from(saved);
    }

    private String buildUserPrompt(List<Map<String, Object>> summary,
                                   List<Map<String, Object>> estrategiasPasadas,
                                   GenerateStrategyRequestDto req) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== Restricciones del usuario ===\n");
        if (req == null) {
            sb.append("(ninguna — el usuario no envió restricciones; decide a partir del dashboard)\n");
        } else {
            appendList(sb, "zonasFoco",         req.getZonasFoco());
            appendList(sb, "zonasExcluir",      req.getZonasExcluir());
            appendValue(sb, "edadMin",          req.getEdadMin());
            appendValue(sb, "edadMax",          req.getEdadMax());
            appendBudget(sb, req.getPresupuestoMxn());
            appendList(sb, "mediosPreferidos",  req.getMediosPreferidos());
            appendValue(sb, "horizonteMeses",   req.getHorizonteMeses());
            appendValue(sb, "tono",             req.getTono());
            appendValue(sb, "contextoExtra",    req.getContextoExtra());
        }

        sb.append("\n=== Indicadores actuales del dashboard ===\n");
        sb.append("Cada widget incluye `widget_id` (úsalo en `basado_en`), `titulo`, ");
        sb.append("y cuando esté disponible `tipo_semantico` y `nivel_geografico`.\n");
        try {
            sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
        } catch (Exception e) {
            sb.append("(no se pudo serializar el resumen del dashboard)");
        }

        if (estrategiasPasadas != null && !estrategiasPasadas.isEmpty()) {
            sb.append("\n\n=== Estrategias pasadas del usuario (aprendizaje) ===\n");
            sb.append("Estado + nota de resultado + comentarios de las últimas estrategias del usuario. ");
            sb.append("Refuerza lo que funcionó, evita lo descartado.\n");
            try {
                sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(estrategiasPasadas));
            } catch (Exception e) {
                sb.append("(no se pudo serializar el historial)");
            }
        }

        sb.append("\n\n=== Instrucción ===\n");
        sb.append("Genera la estrategia siguiendo exactamente el esquema definido. ");
        sb.append("Recuerda: el campo `contexto_analizado` debe reflejar fielmente las restricciones del usuario, ");
        sb.append("cada campaña debe traer 2–4 tácticas con `tipo` del enum, adaptadas a zona y edad, ");
        sb.append("y cada `prioridad` y `segmento_objetivo` debe llevar `basado_en` con uno o más `widget_id` reales del bloque anterior.");
        return sb.toString();
    }

    /**
     * Construye un resumen compacto de las últimas estrategias del usuario que tengan
     * estado != propuesta o comentarios. Limita a 3 estrategias y a piezas mínimas
     * (resumen + estado + nota + comentarios) para mantener el prompt acotado.
     */
    private List<Map<String, Object>> buildPastStrategiesContext(UUID usuarioId) {
        List<MarketingStrategy> all = repository.findByUsuarioId(usuarioId);
        List<Map<String, Object>> out = new ArrayList<>();
        int included = 0;
        for (MarketingStrategy s : all) {
            if (included >= 3) break;
            boolean tieneFeedback =
                    (s.getEstado() != null && !MarketingStrategy.ESTADO_PROPUESTA.equals(s.getEstado()))
                    || (s.getNotaResultado() != null && !s.getNotaResultado().isBlank())
                    || (s.getComentariosJson() != null && !s.getComentariosJson().isBlank());
            if (!tieneFeedback) continue;

            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("creado_en", s.getCreadoEn() != null ? s.getCreadoEn().toString() : null);
            entry.put("estado", s.getEstado());
            if (s.getNotaResultado() != null) entry.put("nota_resultado", s.getNotaResultado());
            entry.put("resumen_ejecutivo", extractResumen(s.getPayloadJson()));
            List<Map<String, Object>> comentarios = parseComentariosBrief(s.getComentariosJson());
            if (!comentarios.isEmpty()) entry.put("comentarios", comentarios);
            out.add(entry);
            included++;
        }
        return out;
    }

    private String extractResumen(String payloadJson) {
        if (payloadJson == null) return null;
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            JsonNode resumen = node.get("resumen_ejecutivo");
            return resumen != null ? resumen.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> parseComentariosBrief(String comentariosJson) {
        if (comentariosJson == null || comentariosJson.isBlank()) return List.of();
        try {
            List<Map<String, Object>> all = objectMapper.readValue(
                    comentariosJson, new TypeReference<List<Map<String, Object>>>() {});
            int from = Math.max(0, all.size() - 3);
            List<Map<String, Object>> out = new ArrayList<>();
            for (int i = from; i < all.size(); i++) {
                Map<String, Object> orig = all.get(i);
                Map<String, Object> brief = new java.util.LinkedHashMap<>();
                if (orig.get("creadoEn") != null) brief.put("creado_en", orig.get("creadoEn"));
                if (orig.get("contenido") != null) brief.put("contenido", orig.get("contenido"));
                out.add(brief);
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void appendList(StringBuilder sb, String name, List<?> list) {
        if (list == null || list.isEmpty()) {
            sb.append("- ").append(name).append(": (no especificado)\n");
        } else {
            sb.append("- ").append(name).append(": ").append(list).append('\n');
        }
    }

    private void appendValue(StringBuilder sb, String name, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            sb.append("- ").append(name).append(": (no especificado)\n");
        } else {
            sb.append("- ").append(name).append(": ").append(value).append('\n');
        }
    }

    private void appendBudget(StringBuilder sb, BigDecimal budget) {
        if (budget == null) {
            sb.append("- presupuestoMxn: (no especificado)\n");
        } else {
            sb.append("- presupuestoMxn: ").append(budget.toPlainString()).append(" MXN\n");
        }
    }
}
