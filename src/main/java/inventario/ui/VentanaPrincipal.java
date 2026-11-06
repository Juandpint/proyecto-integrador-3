package inventario.ui;

import inventario.dao.ConexionBD;
import inventario.estructuras.ListaEnlazada;
import inventario.modelo.Accion;
import inventario.modelo.Categoria;
import inventario.modelo.Movimiento;
import inventario.modelo.Producto;
import inventario.modelo.Proveedor;
import inventario.servicio.CalculoIntegral;
import inventario.servicio.InventarioServicio;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/** Ventana con las mismas operaciones del menú de consola. No tiene SQL ni lógica de estructuras. */
public class VentanaPrincipal extends JFrame {

    // Colores institucionales: verde y blanco de la Universidad Simón Bolívar
    private static final Color VERDE = new Color(0, 122, 61);
    private static final Color VERDE_OSCURO = new Color(0, 90, 45);
    private static final Color VERDE_CLARO = new Color(232, 245, 236);
    private static final Color BLANCO = Color.WHITE;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InventarioServicio servicio;
    private final DefaultTableModel modeloProductos = tablaSoloLectura(
            "Código", "Nombre", "Cantidad", "Mínimo", "Estado", "Precio", "Categoría", "Proveedor");
    private final DefaultTableModel modeloMinimo = tablaSoloLectura(
            "Código", "Nombre", "Cantidad", "Mínimo", "Cómo se obtuvo", "Proveedor");
    private final DefaultTableModel modeloMovimientos = tablaSoloLectura(
            "#", "Fecha", "Código", "Tipo", "Cantidad", "Observación");
    private final JTable tablaProductos = new JTable(modeloProductos);
    private final JLabel estado = new JLabel();

    public VentanaPrincipal(InventarioServicio servicio) {
        super("Inventario de almacén - Universidad Simón Bolívar");
        this.servicio = servicio;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                ConexionBD.cerrar();
            }
        });

        setLayout(new BorderLayout());
        add(encabezado(), BorderLayout.NORTH);
        add(botones(), BorderLayout.WEST);
        add(pestanas(), BorderLayout.CENTER);
        estado.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        estado.setOpaque(true);
        estado.setBackground(VERDE_CLARO);
        estado.setForeground(VERDE_OSCURO);
        add(estado, BorderLayout.SOUTH);

        refrescar();
        setSize(1150, 650);
        setLocationRelativeTo(null);
    }

    /** Usa el aspecto multiplataforma (Metal) para que los botones respeten los colores en Windows y macOS. */
    public static void aplicarTema() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // si falla, se queda el aspecto por defecto
        }
        UIManager.put("Panel.background", BLANCO);
        UIManager.put("OptionPane.background", BLANCO);
        UIManager.put("Button.select", VERDE_OSCURO);
        UIManager.put("TabbedPane.selected", VERDE_CLARO);
        UIManager.put("Table.selectionBackground", VERDE);
        UIManager.put("Table.selectionForeground", BLANCO);
    }

    // =================================================================== armado de la ventana

    private JPanel encabezado() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBackground(VERDE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel titulo = new JLabel("Inventario de almacén");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        titulo.setForeground(BLANCO);
        JLabel subtitulo = new JLabel("Universidad Simón Bolívar · Proyecto Integrador · ODS 9");
        subtitulo.setForeground(BLANCO);
        panel.add(titulo);
        panel.add(subtitulo);
        return panel;
    }

    private JPanel botones() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, VERDE_CLARO),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        panel.add(seccion("PRODUCTOS (árbol AVL)"));
        boton(panel, "Registrar producto", () -> ejecutar(this::registrarProducto));
        boton(panel, "Buscar por código", this::buscarProducto);
        boton(panel, "Modificar producto", () -> ejecutar(this::modificarProducto));
        boton(panel, "Eliminar producto", () -> ejecutar(this::eliminarProducto));
        panel.add(seccion("MOVIMIENTOS (lista enlazada)"));
        boton(panel, "Registrar entrada / salida", () -> ejecutar(this::registrarMovimiento));
        panel.add(seccion("DESHACER (pila)"));
        boton(panel, "Deshacer última acción", () -> ejecutar(this::deshacer));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JTabbedPane pestanas() {
        tablaProductos.setRowHeight(24);
        tablaProductos.setFillsViewportHeight(true);
        tablaProductos.getColumnModel().getColumn(1).setPreferredWidth(220); // nombre
        tablaProductos.getColumnModel().getColumn(7).setPreferredWidth(160); // proveedor
        tablaProductos.getTableHeader().setBackground(VERDE);
        tablaProductos.getTableHeader().setForeground(BLANCO);
        tablaProductos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && codigoSeleccionado() != null) {
                    mostrarDetalle(servicio.buscarProducto(codigoSeleccionado()));
                }
            }
        });

        JTable tablaMovimientos = new JTable(modeloMovimientos);
        tablaMovimientos.setRowHeight(24);
        tablaMovimientos.setFillsViewportHeight(true);
        tablaMovimientos.getTableHeader().setBackground(VERDE);
        tablaMovimientos.getTableHeader().setForeground(BLANCO);

        JTable tablaMinimo = new JTable(modeloMinimo);
        tablaMinimo.setRowHeight(24);
        tablaMinimo.setFillsViewportHeight(true);
        tablaMinimo.getTableHeader().setBackground(VERDE);
        tablaMinimo.getTableHeader().setForeground(BLANCO);
        tablaMinimo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tablaMinimo.getSelectedRow();
                if (e.getClickCount() == 2 && fila >= 0) {
                    mostrarDetalle(servicio.buscarProducto((String) modeloMinimo.getValueAt(fila, 0)));
                }
            }
        });

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Productos (recorrido inorden)", new JScrollPane(tablaProductos));
        pestanas.addTab("Existencia mínima (reponer)", new JScrollPane(tablaMinimo));
        pestanas.addTab("Historial de movimientos", new JScrollPane(tablaMovimientos));
        return pestanas;
    }

    private static JLabel seccion(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(VERDE_OSCURO);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        l.setBorder(BorderFactory.createEmptyBorder(14, 0, 6, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static void boton(JPanel panel, String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.setBackground(VERDE);
        b.setForeground(BLANCO);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        b.addActionListener(e -> accion.run());
        panel.add(b);
        JComponent espacio = (JComponent) Box.createVerticalStrut(6);
        espacio.setAlignmentX(Component.LEFT_ALIGNMENT); // en BoxLayout todos deben tener la misma alineación
        panel.add(espacio);
    }

    private static DefaultTableModel tablaSoloLectura(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    // =================================================================== tablas

    /** Vuelve a llenar las tablas desde las estructuras (árbol AVL y lista enlazada). */
    private void refrescar() {
        modeloProductos.setRowCount(0);
        servicio.listarProductos().recorrer(p -> modeloProductos.addRow(new Object[]{p.getCodigo(), p.getNombre(),
                p.getCantidad(), servicio.minimoEfectivo(p), servicio.estaEnMinimo(p) ? "REPONER" : "OK",
                String.format("$%,.2f", p.getPrecio()),
                servicio.nombreCategoria(p.getIdCategoria()), servicio.nombreProveedor(p.getIdProveedor())}));
        modeloMinimo.setRowCount(0);
        ListaEnlazada<Producto> enMinimo = servicio.productosEnMinimo();
        enMinimo.recorrer(p -> modeloMinimo.addRow(new Object[]{p.getCodigo(), p.getNombre(), p.getCantidad(),
                servicio.minimoEfectivo(p),
                servicio.calcularMinimo(p) == null ? "Manual (sin salidas recientes)" : "Cálculo integral",
                servicio.nombreProveedor(p.getIdProveedor())}));
        modeloMovimientos.setRowCount(0);
        servicio.recorrerMovimientos(m -> modeloMovimientos.insertRow(0, new Object[]{m.getId(),
                m.getFecha().format(FECHA), m.getCodigoProducto(), m.getTipo(), m.getCantidad(),
                m.getObservacion() == null ? "" : m.getObservacion()})); // el más reciente queda arriba
        estado.setText(servicio.cantidadProductos() + " productos (árbol AVL)   ·   "
                + servicio.cantidadMovimientos() + " movimientos (lista enlazada)"
                + (enMinimo.estaVacia() ? "" : "   ·   ALERTA: " + enMinimo.tamano() + " productos por reponer"));
    }

    private String codigoSeleccionado() {
        int fila = tablaProductos.getSelectedRow();
        return fila < 0 ? null : (String) modeloProductos.getValueAt(fila, 0);
    }

    // =================================================================== productos

    private void registrarProducto() throws SQLException {
        JTextField codigo = new JTextField();
        JTextField nombre = new JTextField();
        JTextField cantidad = new JTextField("0");
        JTextField minimo = new JTextField("0");
        JTextField precio = new JTextField();
        JComboBox<Categoria> categoria = new JComboBox<>();
        servicio.getCategorias().recorrer(categoria::addItem);
        JComboBox<Proveedor> proveedor = new JComboBox<>();
        servicio.getProveedores().recorrer(proveedor::addItem);
        if (!formulario("Registrar producto", "Código (solo números):", codigo, "Nombre:", nombre,
                "Cantidad inicial:", cantidad, "Mínimo manual (si no hay historial):", minimo, "Precio:", precio,
                "Categoría:", categoria, "Proveedor:", proveedor)) {
            return;
        }
        Producto p = new Producto(codigo.getText().trim(), obligatorio(nombre, "nombre"),
                entero(cantidad, "La cantidad"), entero(minimo, "El mínimo"), precio(precio),
                ((Categoria) categoria.getSelectedItem()).getId(), ((Proveedor) proveedor.getSelectedItem()).getId());
        servicio.crearProducto(p);
        informar("Producto " + p.getCodigo() + " registrado." + alerta(servicio.buscarProducto(p.getCodigo())));
    }

    private void buscarProducto() {
        String codigo = JOptionPane.showInputDialog(this, "Código a buscar:", "Buscar producto", JOptionPane.QUESTION_MESSAGE);
        if (codigo == null || codigo.isBlank()) {
            return;
        }
        Producto p = servicio.buscarProducto(codigo.trim());
        if (p == null) {
            error("No existe un producto con el código " + codigo.trim() + ".");
            return;
        }
        for (int fila = 0; fila < modeloProductos.getRowCount(); fila++) { // lo resalta en la tabla si está visible
            if (p.getCodigo().equals(modeloProductos.getValueAt(fila, 0))) {
                tablaProductos.setRowSelectionInterval(fila, fila);
                tablaProductos.scrollRectToVisible(tablaProductos.getCellRect(fila, 0, true));
            }
        }
        mostrarDetalle(p);
    }

    private void modificarProducto() throws SQLException {
        Producto actual = pedirProducto("modificar");
        if (actual == null) {
            return;
        }
        JTextField nombre = new JTextField(actual.getNombre());
        JTextField minimo = new JTextField(String.valueOf(actual.getMinimo()));
        JTextField precio = new JTextField(actual.getPrecio().toPlainString());
        JComboBox<Categoria> categoria = new JComboBox<>();
        servicio.getCategorias().recorrer(categoria::addItem);
        elegir(categoria, c -> c.getId() == actual.getIdCategoria());
        JComboBox<Proveedor> proveedor = new JComboBox<>();
        servicio.getProveedores().recorrer(proveedor::addItem);
        elegir(proveedor, p -> p.getId() == actual.getIdProveedor());
        if (!formulario("Modificar producto " + actual.getCodigo(), "Nombre:", nombre,
                "Mínimo manual (si no hay historial):", minimo,
                "Precio:", precio, "Categoría:", categoria, "Proveedor:", proveedor)) {
            return;
        }
        Producto cambios = new Producto(actual.getCodigo(), obligatorio(nombre, "nombre"), actual.getCantidad(),
                entero(minimo, "El mínimo"), precio(precio),
                ((Categoria) categoria.getSelectedItem()).getId(), ((Proveedor) proveedor.getSelectedItem()).getId());
        servicio.modificarProducto(cambios);
        informar("Producto " + actual.getCodigo() + " modificado." + alerta(servicio.buscarProducto(actual.getCodigo())));
    }

    private void eliminarProducto() throws SQLException {
        Producto p = pedirProducto("eliminar");
        if (p == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar " + p.getCodigo() + " - " + p.getNombre() + "?",
                "Eliminar producto", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        servicio.eliminarProducto(p.getCodigo());
        informar("Producto eliminado. Se puede deshacer con \"Deshacer última acción\".");
    }

    /** Toma el producto seleccionado en la tabla o, si no hay, pregunta el código. Devuelve null si se cancela. */
    private Producto pedirProducto(String verbo) {
        String codigo = codigoSeleccionado();
        if (codigo == null) {
            codigo = JOptionPane.showInputDialog(this, "Código del producto a " + verbo + ":");
            if (codigo == null || codigo.isBlank()) {
                return null;
            }
            codigo = codigo.trim();
        }
        Producto p = servicio.buscarProducto(codigo);
        if (p == null) {
            throw new IllegalArgumentException("No existe un producto con el código " + codigo + ".");
        }
        return p;
    }

    /** Muestra los datos del producto y, paso a paso, cómo se obtuvo su existencia mínima. */
    private void mostrarDetalle(Producto p) {
        StringBuilder s = new StringBuilder();
        s.append("Código:     ").append(p.getCodigo()).append('\n');
        s.append("Nombre:     ").append(p.getNombre()).append('\n');
        s.append("Cantidad:   ").append(p.getCantidad()).append('\n');
        s.append("Precio:     $").append(p.getPrecio()).append('\n');
        s.append("Categoría:  ").append(servicio.nombreCategoria(p.getIdCategoria())).append('\n');
        s.append("Proveedor:  ").append(servicio.nombreProveedor(p.getIdProveedor())).append('\n');
        CalculoIntegral.Resultado c = servicio.calcularMinimo(p);
        if (c == null) {
            s.append("Mínimo:     ").append(p.getMinimo()).append(" (manual: no hay salidas recientes para calcularlo)\n");
        } else {
            StringBuilder salidas = new StringBuilder();
            for (double y : c.salidasPorDia()) {
                salidas.append(salidas.length() == 0 ? "" : ", ").append((int) y);
            }
            s.append("Mínimo:     ").append(c.minimo()).append(" calculado (el manual es ").append(p.getMinimo()).append(")\n");
            s.append(String.format("  1. Simpson 1/3, salidas de los últimos %d días (h = 1): [%s]%n", c.diasVentana(), salidas));
            s.append(String.format("     consumo mientras llega el pedido = %.2f unidades%n", c.consumoSimpson()));
            s.append(String.format("  2. Fracciones parciales, demanda promedio %.2f und/día:%n", c.demandaPromedio()));
            s.append(String.format("     pedidos acumulados en un pico = %.2f unidades%n", c.colaPedidos()));
            s.append(String.format("  mínimo = techo(%.2f + %.2f) = %d%n", c.consumoSimpson(), c.colaPedidos(), c.minimo()));
        }
        s.append(alerta(p).trim());
        JTextArea texto = new JTextArea(s.toString());
        texto.setEditable(false);
        texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, new JScrollPane(texto), "Producto " + p.getCodigo(), JOptionPane.PLAIN_MESSAGE);
    }

    // =================================================================== movimientos

    private void registrarMovimiento() throws SQLException {
        JTextField codigo = new JTextField(codigoSeleccionado() == null ? "" : codigoSeleccionado());
        JComboBox<Movimiento.Tipo> tipo = new JComboBox<>(Movimiento.Tipo.values());
        JTextField cantidad = new JTextField();
        JTextField observacion = new JTextField();
        if (!formulario("Registrar entrada o salida", "Código del producto:", codigo, "Tipo:", tipo,
                "Cantidad:", cantidad, "Observación (opcional):", observacion)) {
            return;
        }
        String obs = observacion.getText().trim();
        Producto p = servicio.registrarMovimiento(codigo.getText().trim(), (Movimiento.Tipo) tipo.getSelectedItem(),
                entero(cantidad, "La cantidad"), obs.isEmpty() ? null : obs);
        informar("Movimiento registrado. Existencias de " + p.getCodigo() + ": " + p.getCantidad() + alerta(p));
    }

    // =================================================================== pila

    private void deshacer() throws SQLException {
        Accion accion = servicio.deshacer();
        informar(accion == null ? "No hay acciones para deshacer (la pila está vacía)."
                : "Deshecho: " + accion.getDescripcion());
    }

    // =================================================================== utilidades

    private interface Tarea {
        void hacer() throws SQLException;
    }

    /** Ejecuta una operación, muestra el error si lo hay y actualiza las tablas. */
    private void ejecutar(Tarea tarea) {
        try {
            tarea.hacer();
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (SQLException e) {
            error("Error de base de datos: " + e.getMessage());
        }
        refrescar();
    }

    /** Muestra un formulario de dos columnas (etiqueta, campo). Devuelve true si el usuario presionó Aceptar. */
    private boolean formulario(String titulo, Object... etiquetasYCampos) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        for (int i = 0; i < etiquetasYCampos.length; i += 2) {
            panel.add(new JLabel((String) etiquetasYCampos[i]));
            panel.add((Component) etiquetasYCampos[i + 1]);
        }
        return JOptionPane.showConfirmDialog(this, panel, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private static <T> void elegir(JComboBox<T> combo, Predicate<T> condicion) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (condicion.test(combo.getItemAt(i))) {
                combo.setSelectedIndex(i);
            }
        }
    }

    private static String obligatorio(JTextField campo, String nombre) {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("El " + nombre + " es obligatorio.");
        }
        return texto;
    }

    private static int entero(JTextField campo, String nombre) {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nombre + " debe ser un número entero.");
        }
    }

    private static BigDecimal precio(JTextField campo) {
        try {
            return new BigDecimal(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El precio debe ser un número, por ejemplo 15000 o 15000.50");
        }
    }

    /** Texto de alerta si el producto está en su existencia mínima; vacío si no. */
    private String alerta(Producto p) {
        return p != null && servicio.estaEnMinimo(p)
                ? "\n\nALERTA: tiene " + p.getCantidad() + " unidades y su mínimo es " + servicio.minimoEfectivo(p)
                        + ". Hay que reponer."
                : "";
    }

    private void informar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Inventario", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Atención", JOptionPane.WARNING_MESSAGE);
    }
}
