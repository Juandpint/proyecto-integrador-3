-- 02_dml_catalogos.sql: categorías y proveedores. Ejecutar después de 01_ddl.sql.
-- Con solo 01 + 02 el programa arranca con el árbol y la lista VACÍOS (sirve para probar casos límite).
USE inventario;
SET NAMES utf8mb4; -- para que las tildes y la ñ se guarden bien sin importar el cliente

INSERT INTO categoria (id, nombre) VALUES
    (1, 'Herramientas'),
    (2, 'Ferretería'),
    (3, 'Eléctricos');

INSERT INTO proveedor (id, nombre, telefono, tiempo_entrega_dias) VALUES
    (1, 'Distribuidora El Tornillo', '3001234567', 4),
    (2, 'Ferrecaribe S.A.S.', '3109876543', 6),
    (3, 'Electro Barranquilla', '3205551234', 8);
