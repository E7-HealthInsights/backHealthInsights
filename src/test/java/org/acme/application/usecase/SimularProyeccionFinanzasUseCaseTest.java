package org.acme.application.usecase;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimularProyeccionFinanzasUseCaseTest {

    // Prueba unitaria pura — sin mocks, verifica la matemática del modelo
    // Fuentes: DPP Study NEJM 2002, WHO Global Action Plan 2013-2020, IDF 2024

    private SimularProyeccionFinanzasUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SimularProyeccionFinanzasUseCase();
    }

    @Test
    void curvasSinIntervencionDebenEmpezarEnPrevalenciaBase() {
        var result = useCase.execute(1000, 25, 25, 25, 25, 2030);

        assertEquals(16.4, result.getPuntos().get(0).getSinIntervencion());
    }

    @Test
    void curvasDebenEmpezarIgualesEnAñoInicio() {
        var result = useCase.execute(1000, 25, 25, 25, 25, 2030);

        var primerPunto = result.getPuntos().get(0);
        assertEquals(primerPunto.getSinIntervencion(),
                     primerPunto.getConIntervencion());
    }

    @Test
    void conIntervencionDebeSerMenorQueSinIntervencionEnAñoFinal() {
        // Con presupuesto positivo, la intervención debe reducir prevalencia
        var result = useCase.execute(2000, 30, 25, 25, 20, 2040);

        var ultimo = result.getPuntos().get(result.getPuntos().size() - 1);
        assertTrue(ultimo.getConIntervencion() < ultimo.getSinIntervencion());
    }

    @Test
    void sinPresupuestoAmbasCurvasDenIguales() {
        // Con presupuesto 0, no hay impacto — curvas idénticas
        var result = useCase.execute(0, 25, 25, 25, 25, 2030);

        result.getPuntos().forEach(p ->
            assertEquals(p.getSinIntervencion(), p.getConIntervencion())
        );
    }

    @Test
    void reduccionPctDebeSerNegativaCuandoHayImpacto() {
        var result = useCase.execute(5000, 40, 30, 20, 10, 2040);

        assertTrue(result.getKpis().getReduccionPct() < 0);
    }

    @Test
    void casosEvitadosDebenSerPositivosConIntervencion() {
        var result = useCase.execute(2000, 25, 25, 25, 25, 2040);

        assertTrue(result.getKpis().getCasosEvitados() > 0);
    }

    @Test
    void numeroDePuntosDebeCorresponderAlPeriodo() {
        // 2025 → 2035 = 11 puntos (inclusive)
        var result = useCase.execute(1000, 25, 25, 25, 25, 2035);

        assertEquals(11, result.getPuntos().size());
    }

    @Test
    void añosDeLosUntosDenSercorrectos() {
        var result = useCase.execute(1000, 25, 25, 25, 25, 2028);

        assertEquals(2025, result.getPuntos().get(0).getAño());
        assertEquals(2028, result.getPuntos().get(result.getPuntos().size() - 1).getAño());
    }

    @Test
    void ahorroEstimadoDebeSerPositivo() {
        var result = useCase.execute(2000, 25, 25, 25, 25, 2040);

        assertTrue(result.getKpis().getAhorroEstimadoUSD_M() > 0);
    }
}