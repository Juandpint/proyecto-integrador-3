package inventario.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Una entrada o salida de mercancía (tabla movimiento). Se guarda en la lista enlazada. */
public class Movimiento {

    public enum Tipo { ENTRADA, SALIDA }

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int id; // 0 mientras no se haya guardado en la BD
    private final String codigoProducto;
    private final Tipo tipo;
    private int cantidad;
    private final LocalDateTime fecha;
    private final String observacion;

    public Movimiento(int id, String codigoProducto, Tipo tipo, int cantidad, LocalDateTime fecha, String observacion) {
        this.id = id;
        this.codigoProducto = codigoProducto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.observacion = observacion;
    }

    public Movimiento copia() {
        return new Movimiento(id, codigoProducto, tipo, cantidad, fecha, observacion);
    }

    /** Cuánto cambia el stock del producto: +cantidad si es entrada, -cantidad si es salida. */
    public int efectoEnStock() {
        return tipo == Tipo.ENTRADA ? cantidad : -cantidad;
    }

    public int getId() { return id; }
    public String getCodigoProducto() { return codigoProducto; }
    public Tipo getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public LocalDateTime getFecha() { return fecha; }
    public String getObservacion() { return observacion; }

    public void setId(int id) { this.id = id; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    @Override
    public String toString() {
        return String.format("#%-4d %s  %-7s  producto %-6s x%-5d %s",
                id, fecha.format(FORMATO_FECHA), tipo, codigoProducto, cantidad,
                observacion == null ? "" : observacion);
    }
}
