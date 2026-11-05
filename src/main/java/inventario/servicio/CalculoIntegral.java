package inventario.servicio;

import inventario.estructuras.ListaEnlazada;
import inventario.modelo.Movimiento;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Cálculo integral de la existencia mínima de un producto.
 *
 *   mínimo = consumo mientras llega el pedido   (integral aproximada: regla de Simpson 1/3)
 *          + pedidos que se acumulan en un pico (integral analítica: fracciones parciales)
 *
 * No tiene SQL: recorre la lista enlazada de movimientos que ya está en memoria.
 */
public final class CalculoIntegral {

    /** Resultado con los pasos intermedios, para poder mostrar el cálculo en el menú. */
    public record Resultado(int diasVentana, double[] salidasPorDia, double consumoSimpson,
                            double demandaPromedio, double colaPedidos, int minimo) { }

    // Forma del pico de pedidos pendientes (pedidos que llegan - pedidos despachados), tomada del problema 1.3
    // del banco (RabbitMQ): r(t) = (40t + 100) / ((t + 2)(t + 3)). Se escala para que empiece en r(0) = demanda promedio.
    private static final double ALFA = 40, BETA = 100, P = 2, Q = 3;
    private static final double R_CERO = BETA / (P * Q); // r(0) = 100/6

    private CalculoIntegral() { }

    /**
     * Regla de Simpson 1/3 compuesta sobre n + 1 puntos separados h:
     * h/3 [y0 + 4y1 + 2y2 + ... + 2y(n-2) + 4y(n-1) + yn]. n tiene que ser par.
     */
    public static double simpson(double[] y, double h) {
        int n = y.length - 1;
        if (n < 2 || n % 2 != 0) {
            throw new IllegalArgumentException("Simpson 1/3 necesita un número par de subintervalos (n = " + n + ").");
        }
        double suma = y[0] + y[n];
        for (int i = 1; i < n; i++) {
            suma += (i % 2 == 1 ? 4 : 2) * y[i];
        }
        return h / 3 * suma;
    }

    /**
     * Integral de a a b de (αt + β) / ((t + p)(t + q)) dt, resuelta por fracciones parciales:
     * (αt + β) / ((t + p)(t + q)) = A / (t + p) + B / (t + q),  con A = (β - αp) / (q - p) y B = (αq - β) / (q - p).
     * Cada fracción se integra como un logaritmo: A ln((b + p)/(a + p)) + B ln((b + q)/(a + q)).
     */
    public static double integralFraccionesParciales(double alfa, double beta, double p, double q, double a, double b) {
        if (p == q) {
            throw new IllegalArgumentException("Los factores del denominador deben ser distintos (p ≠ q).");
        }
        if (a > b || a + p <= 0 || a + q <= 0) {
            throw new IllegalArgumentException("El denominador no puede anularse dentro del intervalo.");
        }
        double coefA = (beta - alfa * p) / (q - p);
        double coefB = (alfa * q - beta) / (q - p);
        return coefA * Math.log((b + p) / (a + p)) + coefB * Math.log((b + q) / (a + q));
    }

    /**
     * Existencia mínima del producto a partir de su historial de salidas.
     * Devuelve null si no tuvo salidas en los últimos días: en ese caso se usa el mínimo manual.
     *
     * @param diasEntrega L: los días que tarda el proveedor en traer un pedido
     * @param hoy         último día de la ventana (se pasa como dato para poder probarlo)
     */
    public static Resultado existenciaMinima(ListaEnlazada<Movimiento> movimientos, String codigo,
                                             int diasEntrega, LocalDate hoy) {
        if (diasEntrega <= 0) {
            return null;
        }
        // Simpson pide n par; con L impar se toma un día más, que da un mínimo un poco mayor (lo conservador)
        int n = diasEntrega % 2 == 0 ? diasEntrega : diasEntrega + 1;
        double[] y = new double[n + 1];     // y[i] = unidades que salieron el día (hoy - n + i); h = 1 día
        long[] total = {0};                 // unidades que han salido en todo el historial
        LocalDate[] primera = {hoy};        // fecha de la primera salida
        boolean[] hayEnVentana = {false};
        movimientos.recorrer(m -> {
            if (m.getTipo() != Movimiento.Tipo.SALIDA || !m.getCodigoProducto().equals(codigo)) {
                return;
            }
            LocalDate dia = m.getFecha().toLocalDate();
            long diasAtras = ChronoUnit.DAYS.between(dia, hoy);
            if (diasAtras < 0) {
                return; // fecha futura: no cuenta
            }
            total[0] += m.getCantidad();
            if (dia.isBefore(primera[0])) {
                primera[0] = dia;
            }
            if (diasAtras <= n) {
                y[(int) (n - diasAtras)] += m.getCantidad();
                hayEnVentana[0] = true;
            }
        });
        if (!hayEnVentana[0]) {
            return null;
        }

        double consumo = simpson(y, 1);
        double demandaPromedio = total[0] / (double) (ChronoUnit.DAYS.between(primera[0], hoy) + 1);
        double cola = demandaPromedio / R_CERO * integralFraccionesParciales(ALFA, BETA, P, Q, 0, diasEntrega);
        // el -1e-9 evita que un error de redondeo (12.0000000001) suba el mínimo una unidad
        int minimo = (int) Math.ceil(consumo + cola - 1e-9);
        return new Resultado(n, y, consumo, demandaPromedio, cola, minimo);
    }
}
