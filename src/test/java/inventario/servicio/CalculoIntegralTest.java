package inventario.servicio;

import static org.junit.jupiter.api.Assertions.*;

import inventario.estructuras.ListaEnlazada;
import inventario.modelo.Movimiento;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CalculoIntegralTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 6);

    // Los valores esperados se calcularon a mano; las tablas de resultados de los bancos no coinciden (438.40 y 118.91).

    @Test
    void simpsonConLosNodosDelProblema1_1() {
        // r(t) del problema 1.1 en t = 0, 2, ..., 12 (n = 6, h = 2)
        double[] r = new double[7];
        for (int i = 0; i <= 6; i++) {
            double t = 2 * i;
            r[i] = t <= 6 ? 15 + 4 * t * t - 0.08 * Math.pow(t, 4) : 120 * Math.exp(-0.15 * (t - 6)) / (t - 2);
        }
        assertEquals(359.76, CalculoIntegral.simpson(r, 2), 0.01);
    }

    @Test
    void simpsonEsExactoConDemandaConstante() {
        assertEquals(12.0, CalculoIntegral.simpson(new double[] {3, 3, 3, 3, 3}, 1), 1e-12);
    }

    @Test
    void simpsonRechazaNImpar() {
        assertThrows(IllegalArgumentException.class, () -> CalculoIntegral.simpson(new double[] {1, 2, 3, 4}, 1));
        assertThrows(IllegalArgumentException.class, () -> CalculoIntegral.simpson(new double[] {1}, 1));
    }

    @Test
    void fraccionesParcialesDelProblema1_3() {
        // ∫0^3 (40t + 100) / (t² + 5t + 6) dt = 20 ln(5/2) + 20 ln(6/3) = 20 ln 5
        assertEquals(20 * Math.log(5), CalculoIntegral.integralFraccionesParciales(40, 100, 2, 3, 0, 3), 1e-12);
    }

    @Test
    void fraccionesParcialesRechazaDenominadorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> CalculoIntegral.integralFraccionesParciales(1, 1, 2, 2, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> CalculoIntegral.integralFraccionesParciales(1, 1, -1, 3, 0, 3));
    }

    @Test
    void productoSinHistorialNoSeCalcula() {
        ListaEnlazada<Movimiento> vacia = new ListaEnlazada<>();
        assertNull(CalculoIntegral.existenciaMinima(vacia, "1001", 4, HOY));

        // solo tiene salidas viejas (fuera de los últimos 4 días) y entradas: tampoco se calcula
        ListaEnlazada<Movimiento> viejas = new ListaEnlazada<>();
        viejas.agregar(salida("1001", 5, HOY.minusDays(10)));
        viejas.agregar(new Movimiento(0, "1001", Movimiento.Tipo.ENTRADA, 50, HOY.atTime(9, 0), null));
        assertNull(CalculoIntegral.existenciaMinima(viejas, "1001", 4, HOY));
    }

    @Test
    void demandaConstanteDosPorDia() {
        // 2 unidades diarias durante 10 días (hoy incluido), L = 4 días
        ListaEnlazada<Movimiento> movs = new ListaEnlazada<>();
        for (int d = 9; d >= 0; d--) {
            movs.agregar(salida("1001", 2, HOY.minusDays(d)));
        }
        movs.agregar(salida("1002", 50, HOY)); // otro producto: no cuenta

        CalculoIntegral.Resultado r = CalculoIntegral.existenciaMinima(movs, "1001", 4, HOY);
        assertEquals(8.0, r.consumoSimpson(), 1e-9);            // ∫ de 2 und/día durante 4 días
        assertEquals(2.0, r.demandaPromedio(), 1e-9);
        double cola = 2.0 * 6 / 100 * 20 * (Math.log(6.0 / 2) + Math.log(7.0 / 3)); // s · 20 [ln((L+2)/2) + ln((L+3)/3)]
        assertEquals(cola, r.colaPedidos(), 1e-9);
        assertEquals((int) Math.ceil(8 + cola), r.minimo());    // techo(12.67) = 13
        assertEquals(13, r.minimo());
    }

    @Test
    void tiempoDeEntregaImparUsaUnDiaMas() {
        ListaEnlazada<Movimiento> movs = new ListaEnlazada<>();
        movs.agregar(salida("1001", 3, HOY));
        CalculoIntegral.Resultado r = CalculoIntegral.existenciaMinima(movs, "1001", 5, HOY);
        assertEquals(6, r.diasVentana());
        assertEquals(7, r.salidasPorDia().length);
    }

    private static Movimiento salida(String codigo, int cantidad, LocalDate dia) {
        return new Movimiento(0, codigo, Movimiento.Tipo.SALIDA, cantidad, dia.atTime(10, 0), null);
    }
}
