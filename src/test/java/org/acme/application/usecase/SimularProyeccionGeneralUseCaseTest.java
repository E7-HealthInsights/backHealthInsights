package org.acme.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimularProyeccionGeneralUseCaseTest {

    // Prueba unitaria pura — verifica la matemática del modelo General
    // Base: 13.587M personas con diabetes, IDF 2024

    private SimularProyeccionGeneralUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SimularProyeccionGeneralUseCase();
    }

    @Test
    void curvasSinIntervencionDebenEmpezarEnCasosBase() {
        var result = useCase.execute(2.1, 0, 2025, 2050);

        assertEquals(13.59, result.getPuntos().get(0).getSinIntervencion());
    }

    @Test
    void conIntensidadCeroAmbasCurvasDenIguales() {
        // Sin política pública, las dos curvas son idénticas
        var result = useCase.execute(2.1, 0, 2025, 2050);

        result.getPuntos().forEach(p ->
            assertEquals(p.getSinIntervencion(), p.getConIntervencion())
        );
    }

    @Test
    void conIntensidadMaximaCurvaConIntervencionCreceMasLento() {
        var result = useCase.execute(2.1, 100, 2025, 2050);

        var ultimo = result.getPuntos().get(result.getPuntos().size() - 1);
        assertTrue(ultimo.getConIntervencion() < ultimo.getSinIntervencion());
    }

    @Test
    void reduccionPorcentualDebeSerNegativaConPolitica() {
        var result = useCase.execute(2.1, 30, 2025, 2040);

        assertTrue(result.getKpis().getReduccionPorcentual() < 0);
    }

    @Test
    void casosEvitadosDebenSerPositivosConPolitica() {
        var result = useCase.execute(2.1, 20, 2025, 2040);

        assertTrue(result.getKpis().getCasosEvitados() > 0);
    }

    @Test
    void numeroDePuntosDebeCorresponderAlPeriodo() {
        // 2025 → 2030 = 6 puntos
        var result = useCase.execute(2.1, 0, 2025, 2030);

        assertEquals(6, result.getPuntos().size());
    }

    @Test
    void casosProyectadosDebenCrecerSinIntervencion() {
        var result = useCase.execute(2.1, 0, 2025, 2050);

        double primero = result.getPuntos().get(0).getSinIntervencion();
        double ultimo  = result.getPuntos().get(result.getPuntos().size() - 1)
                               .getSinIntervencion();
        assertTrue(ultimo > primero);
    }
}