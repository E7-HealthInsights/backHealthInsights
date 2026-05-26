package org.acme.application.usecase;

import org.acme.domain.models.Reporte;
import org.acme.domain.models.ReporteTipo;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.ReporteRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetReportesUseCaseTest {

    private ReporteRepository reporteRepository;
    private AuthContext authContext;
    private GetReportesUseCase useCase;

    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        authContext = mock(AuthContext.class);
        useCase = new GetReportesUseCase(reporteRepository, authContext);

        Role role = new Role((byte) 1, "ADMIN");
        User user = new User(USER_ID, "Test", "User", "test@test.com", role, true, "firebase-uid");
        when(authContext.getUser()).thenReturn(user);
    }

    @Test
    void executeShouldReturnReportesFromRepository() {
        Reporte r1 = buildReporte(USER_ID, "Reporte 1", ReporteTipo.DASHBOARD);
        Reporte r2 = buildReporte(USER_ID, "Reporte 2", ReporteTipo.PROYECCION);
        when(reporteRepository.findByUsuarioId(USER_ID)).thenReturn(List.of(r1, r2));

        List<Reporte> result = useCase.execute();

        assertEquals(2, result.size());
        assertSame(r1, result.get(0));
        assertSame(r2, result.get(1));
    }

    @Test
    void executeShouldReturnEmptyListWhenNoReportes() {
        when(reporteRepository.findByUsuarioId(USER_ID)).thenReturn(List.of());

        List<Reporte> result = useCase.execute();

        assertTrue(result.isEmpty());
    }

    @Test
    void executeShouldCallFindByUsuarioIdExactlyOnce() {
        when(reporteRepository.findByUsuarioId(USER_ID)).thenReturn(List.of());

        useCase.execute();

        verify(reporteRepository, times(1)).findByUsuarioId(USER_ID);
    }

    @Test
    void executeShouldFilterByAuthenticatedUserId() {
        when(reporteRepository.findByUsuarioId(USER_ID)).thenReturn(List.of());

        useCase.execute();

        verify(reporteRepository).findByUsuarioId(USER_ID);
    }

    @Test
    void executeShouldPreserveTituloFromRepository() {
        Reporte r = buildReporte(USER_ID, "Dashboard Q4", ReporteTipo.DASHBOARD);
        when(reporteRepository.findByUsuarioId(USER_ID)).thenReturn(List.of(r));

        Reporte result = useCase.execute().get(0);

        assertEquals("Dashboard Q4", result.getTitulo());
    }

    private Reporte buildReporte(UUID usuarioId, String titulo, ReporteTipo tipo) {
        Reporte r = new Reporte();
        r.setId(UUID.randomUUID());
        r.setUsuarioId(usuarioId);
        r.setTitulo(titulo);
        r.setTipo(tipo);
        r.setFechaCreacion(LocalDateTime.now());
        return r;
    }
}
