package org.acme.application.usecase;

import org.acme.application.dto.CreateReporteDto;
import org.acme.domain.models.Reporte;
import org.acme.domain.models.ReporteTipo;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.ReporteRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateReporteUseCaseTest {

    private ReporteRepository reporteRepository;
    private AuthContext authContext;
    private CreateReporteUseCase useCase;

    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        authContext = mock(AuthContext.class);
        useCase = new CreateReporteUseCase(reporteRepository, authContext);

        Role role = new Role((byte) 1, "ADMIN");
        User user = new User(USER_ID, "Test", "User", "test@test.com", role, true, "firebase-uid");
        when(authContext.getUser()).thenReturn(user);
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void executeShouldReturnReporteWithCorrectTitulo() {
        CreateReporteDto dto = new CreateReporteDto();
        dto.setTitulo("Reporte de prueba");
        dto.setTipo(ReporteTipo.DASHBOARD);

        Reporte result = useCase.execute(dto);

        assertEquals("Reporte de prueba", result.getTitulo());
    }

    @Test
    void executeShouldReturnReporteWithCorrectTipo() {
        CreateReporteDto dto = new CreateReporteDto();
        dto.setTitulo("Reporte");
        dto.setTipo(ReporteTipo.PROYECCION);

        Reporte result = useCase.execute(dto);

        assertEquals(ReporteTipo.PROYECCION, result.getTipo());
    }

    @Test
    void executeShouldAssignUsuarioIdFromAuthContext() {
        CreateReporteDto dto = new CreateReporteDto();
        dto.setTitulo("Reporte");
        dto.setTipo(ReporteTipo.ACTIVIDAD);

        Reporte result = useCase.execute(dto);

        assertEquals(USER_ID, result.getUsuarioId());
    }

    @Test
    void executeShouldGenerateNonNullId() {
        CreateReporteDto dto = new CreateReporteDto();
        dto.setTitulo("Reporte");
        dto.setTipo(ReporteTipo.DASHBOARD);

        Reporte result = useCase.execute(dto);

        assertNotNull(result.getId());
    }

    @Test
    void executeShouldSetFechaCreacion() {
        CreateReporteDto dto = new CreateReporteDto();
        dto.setTitulo("Reporte");
        dto.setTipo(ReporteTipo.DASHBOARD);

        Reporte result = useCase.execute(dto);

        assertNotNull(result.getFechaCreacion());
    }

    @Test
    void executeShouldCallRepositorySaveExactlyOnce() {
        CreateReporteDto dto = new CreateReporteDto();
        dto.setTitulo("Reporte");
        dto.setTipo(ReporteTipo.DASHBOARD);

        useCase.execute(dto);

        verify(reporteRepository, times(1)).save(any(Reporte.class));
    }

    @Test
    void executeShouldPreserveReferenciaIdWhenProvided() {
        CreateReporteDto dto = new CreateReporteDto();
        dto.setTitulo("Reporte");
        dto.setTipo(ReporteTipo.DASHBOARD);
        dto.setReferenciaId("dashboard-123");

        Reporte result = useCase.execute(dto);

        assertEquals("dashboard-123", result.getReferenciaId());
    }

    @Test
    void executeShouldAllowNullReferenciaId() {
        CreateReporteDto dto = new CreateReporteDto();
        dto.setTitulo("Reporte");
        dto.setTipo(ReporteTipo.DASHBOARD);

        Reporte result = useCase.execute(dto);

        assertNull(result.getReferenciaId());
    }
}
