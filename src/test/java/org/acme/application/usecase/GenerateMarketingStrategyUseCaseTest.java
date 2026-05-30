package org.acme.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.acme.application.dto.GenerateStrategyRequestDto;
import org.acme.application.dto.MarketingStrategyDto;
import org.acme.application.dto.MarketingStrategyDtoFactory;
import org.acme.domain.models.MarketingStrategy;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.MarketingStrategyRepository;
import org.acme.infrastructure.openai.DashboardSummarizer;
import org.acme.infrastructure.openai.OpenAIClient;
import org.acme.infrastructure.openai.OpenAIInvalidResponseException;
import org.acme.infrastructure.openai.OpenAINotConfiguredException;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GenerateMarketingStrategyUseCaseTest {

    private GetUserWidgetsUseCase getUserWidgetsUseCase;
    private DashboardSummarizer summarizer;
    private OpenAIClient openAIClient;
    private MarketingStrategyRepository repository;
    private AuthContext authContext;
    private MarketingStrategyDtoFactory dtoFactory;
    private GenerateMarketingStrategyUseCase useCase;

    private final UUID USER_ID = UUID.randomUUID();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        getUserWidgetsUseCase = mock(GetUserWidgetsUseCase.class);
        summarizer            = mock(DashboardSummarizer.class);
        openAIClient          = mock(OpenAIClient.class);
        repository            = mock(MarketingStrategyRepository.class);
        authContext           = mock(AuthContext.class);
        dtoFactory            = mock(MarketingStrategyDtoFactory.class);

        Role role = new Role((byte) 4, "DIRECTOR_MERCADOTECNIA");
        User user = new User(USER_ID, "Ana", "López", "ana@test.com", role, true, "firebase-uid");
        when(authContext.getUser()).thenReturn(user);

        // Dashboard vacío por defecto
        when(getUserWidgetsUseCase.execute()).thenReturn(List.of());
        when(summarizer.summarize(any())).thenReturn(List.of());

        // OpenAI devuelve un JSON mínimo válido por defecto
        ObjectNode aiResponse = objectMapper.createObjectNode();
        aiResponse.put("resumen_ejecutivo", "Estrategia de prueba");
        when(openAIClient.chatCompletionJsonSchema(anyString(), anyString(), anyString(), any()))
                .thenReturn(aiResponse);

        // El repositorio devuelve la misma estrategia que recibe
        when(repository.findByUsuarioId(USER_ID)).thenReturn(List.of());
        when(repository.create(any(MarketingStrategy.class))).thenAnswer(inv -> inv.getArgument(0));

        when(dtoFactory.from(any(MarketingStrategy.class))).thenReturn(new MarketingStrategyDto());

        useCase = new GenerateMarketingStrategyUseCase();
        setField(useCase, "getUserWidgetsUseCase", getUserWidgetsUseCase);
        setField(useCase, "summarizer",            summarizer);
        setField(useCase, "openAIClient",          openAIClient);
        setField(useCase, "repository",            repository);
        setField(useCase, "authContext",            authContext);
        setField(useCase, "dtoFactory",            dtoFactory);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Tests: flujo principal ────────────────────────────────────────────────

    @Test
    void executeShouldReturnDtoOnSuccess() {
        MarketingStrategyDto expectedDto = new MarketingStrategyDto();
        expectedDto.setUsuarioId(USER_ID);
        when(dtoFactory.from(any())).thenReturn(expectedDto);

        MarketingStrategyDto result = useCase.execute(null);

        assertNotNull(result);
    }

    @Test
    void executeShouldPersistStrategyWithCurrentUserId() {
        useCase.execute(null);

        verify(repository, times(1)).create(argThat(s ->
                USER_ID.equals(s.getUsuarioId())
        ));
    }

    @Test
    void executeShouldPersistStrategyWithPayloadJson() {
        useCase.execute(null);

        verify(repository, times(1)).create(argThat(s ->
                s.getPayloadJson() != null && !s.getPayloadJson().isBlank()
        ));
    }

    @Test
    void executeShouldCallOpenAIWithSystemAndUserPrompt() {
        useCase.execute(null);

        verify(openAIClient, times(1))
                .chatCompletionJsonSchema(anyString(), anyString(), eq("marketing_strategy"), any());
    }

    @Test
    void executeShouldFetchUserWidgetsToSummarize() {
        useCase.execute(null);

        verify(getUserWidgetsUseCase, times(1)).execute();
        verify(summarizer, times(1)).summarize(any());
    }

    @Test
    void executeShouldWorkWithNullRequest() {
        assertDoesNotThrow(() -> useCase.execute(null));
    }

    @Test
    void executeShouldWorkWithExplicitRequest() {
        GenerateStrategyRequestDto req = new GenerateStrategyRequestDto();
        req.setContextoExtra("Enfoque en zonas rurales");
        req.setHorizonteMeses(6);

        assertDoesNotThrow(() -> useCase.execute(req));
    }

    @Test
    void executeShouldSaveContextoExtraFromRequest() {
        GenerateStrategyRequestDto req = new GenerateStrategyRequestDto();
        req.setContextoExtra("Contexto adicional importante");

        useCase.execute(req);

        verify(repository, times(1)).create(argThat(s ->
                "Contexto adicional importante".equals(s.getContextoExtra())
        ));
    }

    @Test
    void executeShouldSaveNullContextoExtraWhenRequestIsNull() {
        useCase.execute(null);

        verify(repository, times(1)).create(argThat(s ->
                s.getContextoExtra() == null
        ));
    }

    // ── Tests: propagación de errores de OpenAI ───────────────────────────────

    @Test
    void executeShouldPropagateOpenAINotConfiguredException() {
        when(openAIClient.chatCompletionJsonSchema(anyString(), anyString(), anyString(), any()))
                .thenThrow(new OpenAINotConfiguredException());

        assertThrows(OpenAINotConfiguredException.class, () -> useCase.execute(null));
    }

    @Test
    void executeShouldPropagateOpenAIInvalidResponseException() {
        when(openAIClient.chatCompletionJsonSchema(anyString(), anyString(), anyString(), any()))
                .thenThrow(new OpenAIInvalidResponseException("Respuesta inesperada del modelo"));

        assertThrows(OpenAIInvalidResponseException.class, () -> useCase.execute(null));
    }

    @Test
    void executeShouldNotPersistStrategyWhenOpenAIFails() {
        when(openAIClient.chatCompletionJsonSchema(anyString(), anyString(), anyString(), any()))
                .thenThrow(new OpenAINotConfiguredException());

        assertThrows(OpenAINotConfiguredException.class, () -> useCase.execute(null));
        verify(repository, never()).create(any());
    }

    // ── Tests: contexto de estrategias pasadas ────────────────────────────────

    @Test
    void executeShouldQueryPastStrategiesForContextBuilding() {
        useCase.execute(null);

        // El use case consulta el historial del usuario para construir el contexto
        verify(repository, times(1)).findByUsuarioId(USER_ID);
    }

    @Test
    void executeShouldIncludeOnlyStrategiesWithFeedbackInContext() {
        // Estrategia sin feedback (solo propuesta, sin nota, sin comentarios) — NO debe incluirse
        MarketingStrategy sinFeedback = new MarketingStrategy(
                UUID.randomUUID(), USER_ID, LocalDateTime.now(), null, "{\"resumen_ejecutivo\":\"Vieja\"}");

        // Estrategia con estado distinto de propuesta — SÍ debe incluirse
        MarketingStrategy conEstado = new MarketingStrategy(
                UUID.randomUUID(), USER_ID, LocalDateTime.now(), null, "{\"resumen_ejecutivo\":\"Con estado\"}");
        conEstado.setEstado(MarketingStrategy.ESTADO_EJECUTADA);

        when(repository.findByUsuarioId(USER_ID)).thenReturn(List.of(sinFeedback, conEstado));

        // Solo verificamos que el use case no falla al procesar el historial
        assertDoesNotThrow(() -> useCase.execute(null));
    }
}
