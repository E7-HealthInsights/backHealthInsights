package org.acme.application.usecase;

import org.acme.domain.exception.ReporteNotFoundException;
import org.acme.domain.exception.UnauthorizedException;
import org.acme.domain.models.Reporte;
import org.acme.domain.models.ReporteTipo;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.ReporteRepository;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteReporteUseCaseTest {

    private ReporteRepository reporteRepository;
    private AuthContext authContext;
    private DeleteReporteUseCase useCase;

    private final UUID USER_ID    = UUID.randomUUID();
    private final UUID REPORTE_ID = UUID.randomUUID();
    private Reporte reporte;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        authContext = mock(AuthContext.class);
        useCase = new DeleteReporteUseCase(reporteRepository, authContext);

        Role role = new Role((byte) 1, "ADMIN");
        User user = new User(USER_ID, "Test", "User", "test@test.com", role, true, "firebase-uid");
        when(authContext.getUser()).thenReturn(user);

        reporte = new Reporte();
        reporte.setId(REPORTE_ID);
        reporte.setUsuarioId(USER_ID);
        reporte.setTitulo("Reporte de prueba");
        reporte.setTipo(ReporteTipo.DASHBOARD);
        reporte.setFechaCreacion(LocalDateTime.now());

        when(reporteRepository.findReporteById(REPORTE_ID)).thenReturn(Optional.of(reporte));
    }

    @Test
    void executeShouldCallEliminarReporte() {
        useCase.execute(REPORTE_ID);

        verify(reporteRepository, times(1)).eliminarReporte(REPORTE_ID);
    }

    @Test
    void executeShouldFindReporteBeforeDeleting() {
        useCase.execute(REPORTE_ID);

        verify(reporteRepository, times(1)).findReporteById(REPORTE_ID);
    }

    @Test
    void executeShouldThrowReporteNotFoundWhenReporteDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(reporteRepository.findReporteById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ReporteNotFoundException.class, () -> useCase.execute(unknownId));
    }

    @Test
    void executeShouldNotDeleteWhenReporteNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(reporteRepository.findReporteById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ReporteNotFoundException.class, () -> useCase.execute(unknownId));
        verify(reporteRepository, never()).eliminarReporte(any());
    }

    @Test
    void executeShouldThrowUnauthorizedWhenUserIsNotOwner() {
        reporte.setUsuarioId(UUID.randomUUID());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(REPORTE_ID));
    }

    @Test
    void executeShouldNotDeleteWhenUserIsNotOwner() {
        reporte.setUsuarioId(UUID.randomUUID());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(REPORTE_ID));
        verify(reporteRepository, never()).eliminarReporte(any());
    }
}
