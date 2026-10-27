# AgroCampo · Unidad 2

Aplicación Android multiplataforma Kotlin (Compose + Ktor + SQLDelight) para consultar lotes y registrar labores de campo con sesión JWT y almacenamiento local.

## Componentes

- `shared`: dominio, DTO, cliente Ktor, repositorios, esquema SQLDelight y pruebas comunes.
- `androidApp`: interfaz Compose, motor OkHttp, almacenamiento cifrado y driver SQLite Android.
- Backend Spring Boot: proyecto hermano `AgroSierraBackend`.

## Contrato principal

La aplicación usa `http://10.0.2.2:8080/` como dirección del backend en emulador Android. Los endpoints bajo `/api/v1` incluyen `POST auth/login`, listado/detalle de lotes, labores por lote, creación/edición/eliminación de labores, cierre de campaña y usuario actual. Los listados se consumen como `{ contenido, pagina, tamanio, totalElementos, totalPaginas, ultima }`. La autenticación transmite el JWT como Bearer.

## Roles de prueba

El backend siembra `tecnico1`, `jefe1` y `admin`; la contraseña indicada por el proyecto es `Agro2026*`. Las acciones de UI se habilitan a partir de los privilegios del token.

## Persistencia y sincronización

El padrón y labores se observan desde SQLDelight. Una labor creada sin conexión obtiene ID negativo y queda con `pendiente=1`. Al sincronizar se intenta publicar primero cada fila pendiente y se sustituye por la respuesta del servidor dentro de una transacción local; luego se descarga el padrón y se actualiza la caché. Si falla la red, los pendientes se conservan.

## Abrir y ejecutar

El backend requiere Java 17 y Maven; inicia desde `AgroSierraBackend` con `mvn spring-boot:run`. Android requiere Android Studio, SDK API 34, JDK 17 y Gradle compatible con AGP 8.5.2. Ejecuta `:androidApp:assembleDebug` desde Android Studio y abre el emulador antes de iniciar sesión.

> En la copia local revisada faltan `mvnw.cmd`, `gradlew.bat` y `gradle/wrapper`; por eso no se puede compilar con wrapper hasta recuperar esos archivos del repositorio. La compilación debe confirmarse en el equipo de entrega.

## Verificación

Las pruebas comunes están en `shared/src/commonTest`. Ejecutar `:shared:allTests` desde Gradle. Esta carpeta necesita completar el escenario de cliente HTTP simulado y base SQLDelight en memoria descrito por la guía; no declarar esos escenarios como verificados mientras no se ejecuten.

## Git

El proyecto debe conservar ramas `main`, `develop` y ramas `feature/*`, fusiones `--no-ff`, commits con prefijos `feat:`, `fix:`, `refactor:`, `test:` y `docs:` y el tag `v1.0-unidad2`. La carpeta recibida no contiene `.git`; el historial previo debe recuperarse/clonarse desde el repositorio de GitHub para mantener autoría y fechas reales.
