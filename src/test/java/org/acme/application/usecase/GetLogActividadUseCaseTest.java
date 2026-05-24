package org.acme.application.usecase;

import org.acme.domain.models.EntidadTipo;
import org.acme.domain.models.LogActividad;
import org.acme.domain.repository.LogActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetLogActividadUseCaseTest {

    private LogActividadRepository logActividadRepository;
    private GetLogActividadUseCase useCase;

    @BeforeEach
    void setUp() {
        logActividadRepository = mock(LogActividadRepository.class);
        useCase = new GetLogActividadUseCase(logActividadRepository);
    }

    @Test
    void executeShouldReturnLogsFromRepository() {
        LogActividad log1 = new LogActividad(
                UUID.randomUUID(), "CREAR_USUARIO", "Justificación",
                EntidadTipo.USUARIO, UUID.randomUUID().toString(),
                LocalDateTime.now(), "Ana López");
        LogActividad log2 = new LogActividad(
                UUID.randomUUID(), "EDITAR_DATASET", null,
                EntidadTipo.DATASET, UUID.randomUUID().toString(),
                LocalDateTime.now().minusHours(1), null);

        when(logActividadRepository.findAllLogs()).thenReturn(List.of(log1, log2));

        List<LogActividad> result = useCase.execute();

        assertEquals(2, result.size());
        assertSame(log1, result.get(0));
        assertSame(log2, result.get(1));
    }

    @Test
    void executeShouldReturnEmptyListWhenNoLogs() {
        when(logActividadRepository.findAllLogs()).thenReturn(List.of());

        List<LogActividad> result = useCase.execute();

        assertTrue(result.isEmpty());
    }

    @Test
    void executeShouldCallFindAllLogsExactlyOnce() {
        when(logActividadRepository.findAllLogs()).thenReturn(List.of());

        useCase.execute();

        verify(logActividadRepository, times(1)).findAllLogs();
    }

    @Test
    void executeShouldPreserveAdminNombreFromRepository() {
        LogActividad log = new LogActividad(
                UUID.randomUUID(), "CREAR_USUARIO", null,
                EntidadTipo.USUARIO, UUID.randomUUID().toString(),
                LocalDateTime.now(), "Carlos Ramírez");

        when(logActividadRepository.findAllLogs()).thenReturn(List.of(log));

        LogActividad result = useCase.execute().get(0);

        assertEquals("Carlos Ramírez", result.getAdminNombre());
    }

    @Test
    void executeShouldPreserveNullAdminNombreWhenUserNotLinked() {
        LogActividad log = new LogActividad(
                UUID.randomUUID(), "EDITAR_USUARIO", null,
                EntidadTipo.USUARIO, UUID.randomUUID().toString(),
                LocalDateTime.now(), null);

        when(logActividadRepository.findAllLogs()).thenReturn(List.of(log));

        LogActividad result = useCase.execute().get(0);

        assertNull(result.getAdminNombre());
    }
}
