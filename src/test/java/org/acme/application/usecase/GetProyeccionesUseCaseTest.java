package org.acme.application.usecase;

import org.acme.application.dto.ProyeccionResponseDto;
import org.acme.domain.models.Proyeccion;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.ProyeccionRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetProyeccionesUseCaseTest {

    // Prueba unitaria — verifica que el use case devuelve y mapea correctamente
    // las proyecciones del usuario autenticado

    private ProyeccionRepository proyeccionRepository;
    private AuthContext authContext;
    private GetProyeccionesUseCase useCase;

    private final UUID USER_ID = UUID.randomUUID();
    private User authenticatedUser;

    @BeforeEach
    void setUp() throws Exception {
        proyeccionRepository = mock(ProyeccionRepository.class);
        authContext          = mock(AuthContext.class);

        Role role = new Role((byte) 2, "DIRECTOR_FINANZAS");
        authenticatedUser = new User(
                USER_ID, "Carlos", "Ruiz", "carlos@test.com", role, true, "firebase-uid");

        when(authContext.getUser()).thenReturn(authenticatedUser);

        useCase = new GetProyeccionesUseCase();
        setField(useCase, "proyeccionRepository", proyeccionRepository);
        setField(useCase, "authContext",           authContext);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Proyeccion buildProyeccion(UUID id, String titulo, String descripcion) {
        Proyeccion p = new Proyeccion();
        p.setId(id);
        p.setTitulo(titulo);
        p.setDescripcion(descripcion);
        p.setParametros("{\"params\":{},\"puntos\":[],\"kpis\":{}}");
        p.setFechaCreacion(LocalDateTime.now());
        p.setUsuario(authenticatedUser);
        return p;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void executeShouldReturnEmptyListWhenNoProyeccionesExist() {
        when(proyeccionRepository.findByUsuarioId(USER_ID)).thenReturn(List.of());

        List<ProyeccionResponseDto> result = useCase.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void executeShouldReturnMappedDtosForCurrentUser() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Proyeccion p1 = buildProyeccion(id1, "Escenario optimista", "Inversión alta");
        Proyeccion p2 = buildProyeccion(id2, "Escenario base",      "Sin cambios");

        when(proyeccionRepository.findByUsuarioId(USER_ID)).thenReturn(List.of(p1, p2));

        List<ProyeccionResponseDto> result = useCase.execute();

        assertEquals(2, result.size());
    }

    @Test
    void executeShouldMapTituloCorrectly() {
        UUID id = UUID.randomUUID();
        Proyeccion p = buildProyeccion(id, "Proyección 2040", "Descripción");
        when(proyeccionRepository.findByUsuarioId(USER_ID)).thenReturn(List.of(p));

        List<ProyeccionResponseDto> result = useCase.execute();

        assertEquals("Proyección 2040", result.get(0).getTitulo());
    }

    @Test
    void executeShouldMapDescripcionCorrectly() {
        UUID id = UUID.randomUUID();
        Proyeccion p = buildProyeccion(id, "Título", "Descripción detallada del escenario");
        when(proyeccionRepository.findByUsuarioId(USER_ID)).thenReturn(List.of(p));

        List<ProyeccionResponseDto> result = useCase.execute();

        assertEquals("Descripción detallada del escenario", result.get(0).getDescripcion());
    }

    @Test
    void executeShouldMapIdCorrectly() {
        UUID id = UUID.randomUUID();
        Proyeccion p = buildProyeccion(id, "Título", "Descripción");
        when(proyeccionRepository.findByUsuarioId(USER_ID)).thenReturn(List.of(p));

        List<ProyeccionResponseDto> result = useCase.execute();

        assertEquals(id, result.get(0).getId());
    }

    @Test
    void executeShouldMapResultadoFromParametros() {
        Proyeccion p = buildProyeccion(UUID.randomUUID(), "Título", "Descripción");
        p.setParametros("{\"puntos\":[],\"kpis\":{}}");
        when(proyeccionRepository.findByUsuarioId(USER_ID)).thenReturn(List.of(p));

        List<ProyeccionResponseDto> result = useCase.execute();

        assertEquals("{\"puntos\":[],\"kpis\":{}}", result.get(0).getResultado());
    }

    @Test
    void executeShouldMapFechaCreacionCorrectly() {
        LocalDateTime fecha = LocalDateTime.of(2025, 3, 15, 10, 30);
        Proyeccion p = buildProyeccion(UUID.randomUUID(), "Título", "Descripción");
        p.setFechaCreacion(fecha);
        when(proyeccionRepository.findByUsuarioId(USER_ID)).thenReturn(List.of(p));

        List<ProyeccionResponseDto> result = useCase.execute();

        assertEquals(fecha, result.get(0).getFechaCreacion());
    }

    @Test
    void executeShouldQueryRepositoryUsingAuthenticatedUserId() {
        when(proyeccionRepository.findByUsuarioId(USER_ID)).thenReturn(List.of());

        useCase.execute();

        verify(proyeccionRepository, times(1)).findByUsuarioId(USER_ID);
    }
}
