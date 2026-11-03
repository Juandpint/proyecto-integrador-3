-- 03_dml_productos.sql: productos y movimientos de prueba. Ejecutar después de 02_dml_catalogos.sql.
USE inventario;
SET NAMES utf8mb4; -- para que las tildes y la ñ se guarden bien sin importar el cliente

-- Los productos 1003, 1007 y 1011 están en su existencia mínima o por debajo
INSERT INTO producto (codigo, nombre, cantidad, minimo, precio, id_categoria, id_proveedor) VALUES
    ('1001', 'Martillo de uña 16 oz',        25, 5,  32000.00, 1, 1),
    ('1002', 'Destornillador de estrella',   40, 10,  8500.00, 1, 1),
    ('1003', 'Llave inglesa 10 pulgadas',     4, 5,  45000.00, 1, 2),
    ('1004', 'Alicate universal',            18, 5,  22000.00, 1, 2),
    ('1005', 'Caja de tornillos 1/2 (100)',  60, 15,  9000.00, 2, 1),
    ('1006', 'Caja de puntillas 2 (500 g)',  35, 10,  7500.00, 2, 1),
    ('1007', 'Cinta aislante negra',          8, 10,  3500.00, 3, 3),
    ('1008', 'Bombillo LED 12 W',            50, 20,  6000.00, 3, 3),
    ('1009', 'Extensión eléctrica 5 m',      12, 4,  28000.00, 3, 3),
    ('1010', 'Silicona transparente',        22, 6,  14000.00, 2, 2),
    ('1011', 'Interruptor sencillo',          3, 3,   9500.00, 3, 3),
    ('1012', 'Flexómetro 5 m',               15, 5,  18000.00, 1, 2);

-- Historial en orden cronológico (el id sigue el orden de la fecha)
INSERT INTO movimiento (codigo_producto, tipo, cantidad, fecha, observacion) VALUES
    ('1001', 'ENTRADA', 30, '2026-10-01 08:30:00', 'Compra inicial'),
    ('1005', 'ENTRADA', 80, '2026-10-01 09:00:00', 'Compra inicial'),
    ('1001', 'SALIDA',   5, '2026-10-05 10:15:00', 'Venta mostrador'),
    ('1003', 'SALIDA',   6, '2026-10-08 11:40:00', 'Pedido obra'),
    ('1005', 'SALIDA',  20, '2026-10-12 15:20:00', 'Pedido obra'),
    ('1007', 'SALIDA',  12, '2026-10-15 09:05:00', 'Venta mostrador'),
    ('1008', 'ENTRADA', 50, '2026-10-18 14:00:00', 'Reposición'),
    ('1011', 'SALIDA',   7, '2026-10-22 16:45:00', 'Venta mostrador'),
    ('1010', 'ENTRADA', 22, '2026-10-25 08:10:00', 'Reposición'),
    ('1012', 'SALIDA',   3, '2026-10-30 17:30:00', 'Venta mostrador');
