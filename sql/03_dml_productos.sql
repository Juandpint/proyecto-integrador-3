-- 03_dml_productos.sql: productos y movimientos de prueba. Ejecutar después de 02_dml_catalogos.sql.
USE inventario;
SET NAMES utf8mb4; -- para que las tildes y la ñ se guarden bien sin importar el cliente

-- "minimo" es el mínimo manual: se usa solo si el producto no tiene salidas recientes.
-- Con el mínimo calculado, los productos 1003, 1007 y 1011 quedan por debajo y hay que reponerlos.
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

-- Historial simulado de 45 días que TERMINA EL DÍA EN QUE SE EJECUTA ESTE SCRIPT (fechas relativas a CURDATE()).
-- El cálculo integral de la existencia mínima mira los últimos días, así que conviene volver a ejecutar
-- 01, 02 y 03 el día de la presentación.
-- Por producto: una compra inicial hace 45 días y una salida diaria (los días con cantidad 0 no hay salida).
-- La cantidad diaria = base + MOD(7·día + código, rango): es un patrón fijo, así que el resultado siempre es el mismo.
-- La compra inicial = cantidad actual + todas las salidas, para que el historial cuadre con el stock.
-- Se inserta en orden de fecha, así el id sigue el orden cronológico.
INSERT INTO movimiento (codigo_producto, tipo, cantidad, fecha, observacion)
WITH RECURSIVE dias (n) AS (            -- n = días antes de hoy: 0..44
    SELECT 0 UNION ALL SELECT n + 1 FROM dias WHERE n < 44
),
demanda (codigo, base, rango) AS (      -- unidades por día entre base y base + rango - 1
    SELECT '1001', 0, 3 UNION ALL SELECT '1002', 1, 4 UNION ALL SELECT '1003', 0, 3 UNION ALL
    SELECT '1004', 0, 3 UNION ALL SELECT '1005', 2, 5 UNION ALL SELECT '1006', 1, 4 UNION ALL
    SELECT '1007', 1, 3 UNION ALL SELECT '1008', 2, 4 UNION ALL SELECT '1009', 0, 2 UNION ALL
    SELECT '1010', 0, 3 UNION ALL SELECT '1011', 0, 2 UNION ALL SELECT '1012', 0, 2
),
salidas AS (
    SELECT dm.codigo,
           dm.base + MOD(7 * d.n + CAST(dm.codigo AS UNSIGNED), dm.rango) AS cantidad,
           TIMESTAMP(CURDATE() - INTERVAL d.n DAY, '10:00:00') AS fecha
    FROM demanda dm CROSS JOIN dias d
)
SELECT p.codigo, 'ENTRADA', p.cantidad + (SELECT SUM(s.cantidad) FROM salidas s WHERE s.codigo = p.codigo),
       TIMESTAMP(CURDATE() - INTERVAL 45 DAY, '08:00:00'), 'Compra inicial'
FROM producto p
UNION ALL
SELECT codigo, 'SALIDA', cantidad, fecha, 'Venta mostrador' FROM salidas WHERE cantidad > 0
ORDER BY 4, 1;
