-- 01_ddl.sql: crea la base de datos "inventario" y sus 4 tablas.
-- OJO: si las tablas ya existen las borra, así que deja la base de datos VACÍA.
-- Después ejecute 02_dml_catalogos.sql y (si quiere datos de prueba) 03_dml_productos.sql.

CREATE DATABASE IF NOT EXISTS inventario CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE inventario;

-- Se borran en orden inverso a las llaves foráneas
DROP TABLE IF EXISTS movimiento;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS proveedor;
DROP TABLE IF EXISTS categoria;

CREATE TABLE categoria (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE proveedor (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    nombre   VARCHAR(100) NOT NULL,
    telefono VARCHAR(20)
);

-- Una categoría y un proveedor tienen muchos productos (1:N)
CREATE TABLE producto (
    codigo       VARCHAR(20)   PRIMARY KEY,
    nombre       VARCHAR(100)  NOT NULL,
    cantidad     INT           NOT NULL DEFAULT 0,
    minimo       INT           NOT NULL DEFAULT 0,
    precio       DECIMAL(12,2) NOT NULL DEFAULT 0,
    id_categoria INT           NOT NULL,
    id_proveedor INT           NOT NULL,
    CONSTRAINT chk_producto_cantidad CHECK (cantidad >= 0),
    CONSTRAINT chk_producto_minimo   CHECK (minimo >= 0),
    CONSTRAINT chk_producto_precio   CHECK (precio >= 0),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id) ON DELETE RESTRICT,
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor (id) ON DELETE RESTRICT
);

-- Un producto tiene muchos movimientos (1:N).
-- RESTRICT: no se puede borrar un producto que tenga movimientos.
CREATE TABLE movimiento (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    codigo_producto VARCHAR(20)              NOT NULL,
    tipo            ENUM ('ENTRADA','SALIDA') NOT NULL,
    cantidad        INT                      NOT NULL,
    fecha           DATETIME                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion     VARCHAR(200),
    CONSTRAINT chk_movimiento_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (codigo_producto) REFERENCES producto (codigo) ON DELETE RESTRICT
);
