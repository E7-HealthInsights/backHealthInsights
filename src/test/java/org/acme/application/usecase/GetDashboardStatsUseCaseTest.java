package org.acme.application.usecase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.acme.application.dto.DashboardStatsResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GetDashboardStatsUseCaseTest {

    // Prueba unitaria — sin @QuarkusTest, sin DB
    // Mockea EntityManager para verificar que el use case
    // llama correctamente a las dos stored functions

    private EntityManager em;
    private Query         mockQuery;
    private GetDashboardStatsUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        em        = mock(EntityManager.class);
        mockQuery = mock(Query.class);

        when(em.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

        // Por defecto, todas las funciones retornan 5
        when(mockQuery.getSingleResult()).thenReturn(5);

        // Inyecta el EntityManager manualmente
        useCase = new GetDashboardStatsUseCase();
        var field = GetDashboardStatsUseCase.class.getDeclaredField("em");
        field.setAccessible(true);
        field.set(useCase, em);
    }

    @Test
    void executeShouldReturnDtoWithAllFields() {
        DashboardStatsResponseDto result = useCase.execute();

        assertNotNull(result);
    }

    @Test
    void executeShouldCallFnTotalUsuariosForActiveUsers() {
        useCase.execute();

        verify(em, atLeastOnce()).createNativeQuery(
            contains("fn_total_usuarios")
        );
    }

    @Test
    void executeShouldCallFnTotalDatasetsForEachEstado() {
        useCase.execute();

        verify(em, atLeastOnce()).createNativeQuery(
            contains("fn_total_datasets")
        );
    }

    @Test
    void executeShouldMapUsuariosActivosFromFunction() {
        when(mockQuery.getSingleResult()).thenReturn(10);

        DashboardStatsResponseDto result = useCase.execute();

        assertEquals(10, result.getUsuariosActivos());
    }

    @Test
    void executeShouldMapDatasetsReadyFromFunction() {
        when(mockQuery.getSingleResult()).thenReturn(3);

        DashboardStatsResponseDto result = useCase.execute();

        assertEquals(3, result.getDatasetsActivos());
    }

    @Test
    void executeShouldCallFnUsuariosSixTimes() {
        // NULL activos, NULL inactivos, ADMIN, DG, DF, DM
        useCase.execute();

        verify(em, times(6)).createNativeQuery(
            contains("fn_total_usuarios")
        );
    }

    @Test
    void executeShouldReturnZeroWhenFunctionReturnsZero() {
        when(mockQuery.getSingleResult()).thenReturn(0);

        DashboardStatsResponseDto result = useCase.execute();

        assertEquals(0, result.getUsuariosActivos());
        assertEquals(0, result.getDatasetsActivos());
        assertEquals(0, result.getDatasetsInactivos());
    }
}