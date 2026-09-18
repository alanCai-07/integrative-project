# Proyecto Integrador - Programacion II

## Descripcion

Sistema de gestion de ventas e inventario **TechZone** desarrollado en Java 21
con JavaFX. La aplicacion utiliza una arquitectura por capas:

**View -> Controller -> Services -> DAO -> PostgreSQL en Neon**

## Diagrama Entidad-Relacion

![MER](MER.png)

## Estructura principal

```text
proyecto-integrador-pii/
├── src/main/java/Proyecto/
│   ├── Model/          Entidades del dominio
│   ├── dao/            Acceso a datos y consultas PostgreSQL
│   ├── services/       Logica de negocio
│   ├── Controller/     Controladores MVC
│   ├── View/           Interfaz grafica JavaFX
│   ├── util/
│   │   ├── conexionBD.java  Pool HikariCP para Neon
│   │   └── conexionApp.java
│   └── MainGUI.java    Punto de entrada
├── src/main/resources/ Logo de la aplicacion
├── Techzone.sql        Esquema y datos iniciales PostgreSQL
├── MER.png             Diagrama entidad-relacion
├── config.properties   Configuracion local de la base de datos
└── pom.xml             Dependencias y configuracion Maven
```

## Tecnologias

- Java 21
- JavaFX 23.0.2
- Maven
- PostgreSQL en Neon
- PostgreSQL JDBC 42.7.4
- HikariCP 5.1.0 para el pool de conexiones
- JUnit Jupiter 5.10.2

## Configuracion de PostgreSQL en Neon

La aplicacion lee `config.properties` desde la raiz del proyecto. Este archivo
contiene credenciales y no debe publicarse ni compartirse.

Usa esta estructura, reemplazando los valores de ejemplo:

```properties
db.motor=postgresql
db.host=tu-host-pooler.neon.tech
db.puerto=5432
db.nombre=neondb
db.usuario=tu_usuario
db.contrasena=tu_contrasena
db.schema=Techzone
db.ssl=true
db.timeout_conexion=30
db.conexiones_max=10
db.conexiones_min=1
```

La configuracion actual:

- Usa PostgreSQL con SSL (`sslmode=require`).
- Trabaja con el esquema `"Techzone"` y `public` en el `search_path`.
- Usa el endpoint `-pooler` de Neon.
- Reutiliza conexiones mediante HikariCP para evitar una conexion fisica por
  cada consulta.
- Precalienta el pool en segundo plano al iniciar JavaFX, sin bloquear la
  ventana de login.

Los DAO esperan las tablas dentro del esquema `"Techzone"`. No se debe cambiar
`db.schema` a `public`.

## Base de datos

`Techzone.sql` es el script PostgreSQL compatible con la aplicacion. Incluye:

- `persona`, `cliente`, `empleado` y `proveedor`.
- `producto`, incluyendo la columna `imagen_url`.
- `categoria`, `carrito` e `item_carrito`.
- `documento` y `movimiento_inventario`.
- `tipo_documento`, `metodo_pago` y `cargo`.
- La vista `vista_persona_cliente`.
- Datos iniciales y usuarios de prueba.

Ejecuta el script conectado a la base de datos `neondb`. Si se utiliza una
version del script que elimina el esquema antes de crearlo, la operacion es
destructiva y borra los datos actuales. Usalo solo cuando quieras reconstruir
la base de datos desde cero.

## Requisitos

- JDK 21.
- Maven 3.9 o superior.
- Una base de datos PostgreSQL en Neon.
- Un archivo `config.properties` configurado con las credenciales reales.

## Compilar, probar y ejecutar

Desde la raiz del proyecto:

```powershell
# Compilar
mvn clean compile

# Ejecutar las pruebas
mvn test

# Ejecutar la aplicacion JavaFX
mvn javafx:run
```

## Usuarios de prueba

Las contrasenas se almacenan como hashes SHA-256 en PostgreSQL:

| Rol | Email | Contrasena |
|---|---|---|
| Administrador | admin@techzone.co | admin123 |
| Comprador | osoto@techzone.co | comprador123 |
| Vendedor | lbustamante@techzone.co | vendedor123 |
| Cajero | cmartinez@techzone.co | techzone |
| Bodeguero | jgaviria@techzone.co | bodeguero123 |
| Cliente | laura@gmail.com | laura123 |
| Cliente | pedro@gmail.com | pedro123 |

## Solucion de problemas

- **La conexion tarda varios segundos:** Neon puede suspender temporalmente
  una instancia sin actividad. El pool se precalienta al iniciar y reutiliza
  las conexiones posteriores.
- **No se encuentran tablas:** verifica `db.schema=Techzone` y que
  `Techzone.sql` se haya ejecutado en la base de datos correcta.
- **Error de credenciales:** revisa `db.usuario` y `db.contrasena` en
  `config.properties`. No uses las credenciales de ejemplo.
- **Motor de base de datos:** el codigo actual esta configurado para
  PostgreSQL en Neon; no debe conectarse directamente a MySQL.

## Autor

Alan Caicedo
