# Inventario de almacén con búsqueda por código y control de existencias mínimas

## a. Datos del proyecto

- **Programa:** Ingeniería de Datos e IA, semestre 3. Universidad Simón Bolívar.
- **Proyecto Integrador de Aula:** Estructura de Datos T-01 y Base de Datos T-01.
- **ODS 9: Industria, innovación e infraestructura.**
  - Meta 9.3: integrar a las pequeñas empresas en las cadenas de valor.
  - Meta 9.4: usar los recursos de forma más eficiente.

| Integrante | Rol |
|---|---|
| Juan Pinto | Desarrollador (aplicación Java, interfaz y JDBC) |
| Jose Monroy | Gestor de base de datos (modelo E-R, 3FN, scripts SQL) |
| Juan Cervantes | Diseño de estructura de datos (árbol AVL) |
| Luis Mercado | Diseño de estructura de datos (lista enlazada y pila) |

## b. Descripción

Muchos almacenes pequeños llevan el inventario a mano. Así es difícil encontrar un producto, mantener las existencias al día y darse cuenta a tiempo de que algo se está acabando, y eso genera sobrecompras y faltantes. Este prototipo es una aplicación de consola en Java que:
- registra, busca, modifica y elimina productos por código usando un **árbol AVL**,
- guarda el historial de entradas y salidas en una **lista enlazada**,
- permite deshacer las últimas acciones con una **pila**,
- avisa cuando un producto llega a su existencia mínima, que se calcula con **cálculo integral** a partir del historial de salidas (ver sección e).

Todo se guarda en MySQL mediante JDBC con sentencias preparadas. Al iniciar, el programa carga los datos de la base de datos en las estructuras.

## c. Instrucciones de ejecución

**Requisitos**
- **Java:** JDK 17 o superior.
- **Base de datos:** MySQL 8.x. Se probó con MySQL 8.4.
- **IDE (opcional):** NetBeans 17 o superior. También funciona Maven 3.9 desde la consola.
- **Driver JDBC:** `mysql-connector-j` 9.1.0. No hay que descargarlo: Maven lo baja solo la primera vez, y para eso se necesita internet.

**1. Crear la base de datos.** Ejecute los scripts de la carpeta `sql/` en este orden:

| Script | Qué hace |
|---|---|
| `01_ddl.sql` | Crea la base de datos `inventario` y sus 4 tablas. **Si ya existían, las borra.** |
| `02_dml_catalogos.sql` | Agrega las categorías y los proveedores, con su tiempo de entrega (4, 6 y 8 días). |
| `03_dml_productos.sql` | Agrega 12 productos y 45 días de movimientos simulados (425 en total). Es opcional. |

Las fechas de `03` terminan **el día en que se ejecuta el script**, porque el mínimo se calcula con las salidas de los últimos días. Para la presentación, vuelva a ejecutar `01`, `02` y `03` ese mismo día.

Si ya tenía la base de datos de una versión anterior, **vuelva a ejecutar los tres scripts**. La tabla `proveedor` ahora tiene la columna `tiempo_entrega_dias`, y sin ella el programa no carga los datos.

Puede ejecutarlos de dos formas:
- **MySQL Workbench:** *File → Open SQL Script*, elija el script y presione el rayo ⚡. Repita con cada uno.
- **Consola:**
  ```
  mysql -u root -p < sql/01_ddl.sql
  ```
  Haga lo mismo con `02` y `03`. En Windows, PowerShell no acepta `<`. Use Workbench, o use `cmd`, o entre al cliente con `mysql -u root -p` y escriba `source sql/01_ddl.sql` (y así con los demás).

Para probar los casos de **estructura vacía**, ejecute solo `01` y `02`.

**2. Configurar la conexión.** Copie `config.example.properties` con el nombre `config.properties`, en la misma carpeta, y escriba su usuario y clave de MySQL:
```
db.url=jdbc:mysql://localhost:3306/inventario?useSSL=false&allowPublicKeyRetrieval=true
db.usuario=root
db.clave=su_clave
```
`config.properties` no se sube al repositorio, porque está en `.gitignore`.

**3. Ejecutar.** Hay dos formas:
- **NetBeans:** *File → Open Project*, elija la carpeta del proyecto (tiene el ícono de Maven) y presione **Run** (F6). El menú aparece en la ventana *Output*, y ahí mismo se escriben las opciones.
- **Consola,** desde la carpeta del proyecto:
  ```
  mvn package
  java -jar target/inventario-jar-with-dependencies.jar
  ```

Si el programa no encuentra `config.properties` o no logra conectarse a MySQL, muestra un mensaje que dice qué revisar.

**Pruebas unitarias:** `mvn test`. En NetBeans también se pueden correr con *Test*. Son 29 pruebas:
- 21 de las estructuras: estructura vacía, un elemento, elemento inexistente y 1000 códigos insertados en orden en el AVL.
- 8 del cálculo integral: los problemas 1.1 y 1.3 del banco, demanda constante, producto sin historial y n impar.

## d. Estructuras implementadas

Las tres estructuras están hechas con clases y nodos propios. No usan las colecciones de Java.

| Estructura | Clase | Entidad o tabla que gestiona | Operaciones (opción del menú) |
|---|---|---|---|
| Árbol AVL | `inventario.estructuras.ArbolAVL` | `producto` (la llave es el código) | insertar (1), buscar (2), modificar (3), eliminar (4), recorrido inorden (5 y 6) |
| Lista enlazada | `inventario.estructuras.ListaEnlazada<T>` | `movimiento`. También guarda `categoria` y `proveedor`, solo para consulta. | insertar al final en O(1) (7), recorrer en orden cronológico (8), eliminar (al deshacer un movimiento, 9) |
| Pila | `inventario.estructuras.Pila<T>` | Acciones para deshacer (`modelo.Accion`). Vive solo en memoria. | apilar (en cada cambio), ver el tope y desapilar = deshacer (9) |

- Modificar un elemento del medio no es una operación propia de una pila. Por eso la pila solo tiene apilar, ver el tope, desapilar y recorrer.
- Deshacer también escribe en la base de datos. La pila empieza vacía cada vez que se abre el programa.

**Paquetes** (`src/main/java/inventario/`): `modelo` (entidades), `estructuras` (AVL, lista y pila), `dao` (el único lugar con SQL, todo con `PreparedStatement`), `servicio` (une estructuras y BD, maneja deshacer y, en `CalculoIntegral`, calcula la existencia mínima) y `ui` (menú).

## e. Existencia mínima con cálculo integral

La existencia mínima de cada producto no se escribe a mano. Se calcula con dos integrales sobre el historial de salidas que está en la lista enlazada:

**mínimo = ⌈ consumo mientras llega el pedido + pedidos que se acumulan en un pico ⌉**

1. **Integral aproximada: regla de Simpson 1/3** (método del problema 1.1 del banco). Se suman las salidas de cada uno de los últimos L días, donde L es el tiempo de entrega del proveedor, y se integran con h = 1 día:
   `∫ r(t) dt ≈ h/3 [y0 + 4y1 + 2y2 + … + 4y(n−1) + yn]`.
   Simpson 1/3 necesita n par. Si L es impar, se toma un día más.
2. **Integral analítica: fracciones parciales** (método del problema 1.3 del banco, la cola de RabbitMQ). La tasa de pedidos pendientes es `r(t) = s · (40t + 100) / ((t + 2)(t + 3)) = s · [20/(t + 2) + 20/(t + 3)]`. El factor s hace que el pico empiece en la demanda diaria promedio del producto. Por eso:
   `Q = s · 20 [ln((L + 2)/2) + ln((L + 3)/3)]`.

Si un producto no tuvo salidas en los últimos L días, se usa su mínimo manual. La opción 2 (buscar) muestra el cálculo paso a paso. Las opciones 5 y 6 muestran el mínimo manual y el calculado, y la alerta usa el calculado.

## f. Estado actual

**Funciona**
- **Las cinco operaciones en las estructuras propias,** con los casos de estructura vacía, un solo elemento y código o número inexistente.
- **Carga de la base de datos a las estructuras al iniciar.**
- **Persistencia:** todo cambio se guarda primero en MySQL y después en memoria. Los movimientos actualizan el stock en una sola transacción. Después de reiniciar el programa, los datos se conservan.
- **Validaciones:**
  - no se aceptan códigos repetidos ni con letras,
  - el stock nunca queda negativo,
  - no se puede eliminar un producto que tiene movimientos (el programa lo explica),
  - si se escribe algo que no es un número, el programa lo vuelve a pedir.
- **Alerta de existencia mínima** al iniciar, después de cada movimiento y en la opción 6. Usa el mínimo calculado con Simpson 1/3 y fracciones parciales, que cambia con cada salida nueva.
- **Deshacer** funciona para los 4 tipos de acción (crear, modificar y eliminar producto, y registrar movimiento). Se probó que al deshacer todo, la base de datos queda idéntica a como estaba.

**Falta o limitaciones conocidas**
- **Categorías y proveedores** se cargan solo desde el script; no se editan desde el menú. Esto incluye el tiempo de entrega.
- **Datos de prueba que envejecen:** si pasan más días que el tiempo de entrega desde que se ejecutó `03`, los productos dejan de tener salidas recientes y vuelven a su mínimo manual.
- **Interfaz gráfica (Swing):** pendiente para la Feria Innova. Usará la misma capa `servicio`.
- **Control por voz y lectura del inventario en voz alta (TTS):** está planeado para la Feria Innova y responde a la observación del profesor de hacer algo más innovador. La idea es reconocimiento de voz sin internet con una lista fija de comandos, más la voz del sistema operativo.
- **Pruebas en otros entornos:** se probó en macOS con MySQL 8.4. Faltan pruebas en Windows, y XAMPP/MariaDB no se ha probado.
- **Tildes en la consola de Windows (`cmd`):** pueden verse mal. En NetBeans se ven bien.
