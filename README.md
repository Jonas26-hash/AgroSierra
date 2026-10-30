# AgroSierra · Aplicación Android (KMP)

Aplicación Android construida con Kotlin Multiplatform para consultar lotes y registrar labores de campo contra la API de `AgroSierraBackend`. Usa Jetpack Compose, Ktor, SQLDelight, Koin y autenticación JWT, con almacenamiento local que permite registrar labores sin conexión y sincronizarlas después.

> Este documento describe el estado real del código. Las piezas que existen pero no están conectadas a la interfaz se indican en **Limitaciones conocidas**; no se presentan como funcionalidad terminada.

## Módulos

- `shared`: módulo Kotlin Multiplatform con target Android. Contiene dominio, DTO, cliente Ktor, repositorios, caso de uso de sincronización, esquema SQLDelight y pruebas comunes.
- `androidApp`: aplicación Android. Contiene `MainActivity`, la interfaz Compose, el registro de Koin para Android, el almacenamiento cifrado del token, el driver SQLite y el monitor de conectividad.
- `AgroSierraBackend`: backend Spring Boot, alojado en su propio repositorio.

## Estructura de paquetes

### `shared/src/commonMain/kotlin/pe/upeu/agrosierra`

```
data/
  local/        AlmacenSeguro (contrato) y BaseLocal
  remote/       ClienteHttp, MapeoErrores, ApiConfig
    dto/        AuthDtos, Dtos
  repository/   AuthRepositoryImpl, AgroRepositoryImpl
di/             AppModule y PlatformModule (contrato)
domain/
  model/        Labor, Lote, TipoLabor, Sesion, ErrorApi
  repository/   AgroRepository
  usecase/      SincronizarAutomaticoUseCase
```

El esquema y las consultas SQLDelight están en `shared/src/commonMain/sqldelight/pe/upeu/agrosierra/AgroDb.sq`.

### `shared/src/androidMain/kotlin/pe/upeu/agrosierra`

`BaseLocal`, `AlmacenSeguro` (basado en `EncryptedSharedPreferences`), `ApiConfig.android`, `HttpClientEngine.android` y `PlatformModule`.

### `shared/src/commonTest/kotlin/pe/upeu/agrosierra`

`DominioTest`, `SyncYErroresTest` y `SyncOrdenTest`.

### `androidApp/src/main/kotlin/pe/upeu/agrosierra`

```
App.kt                        Interfaz Compose y estado de las pantallas
MainActivity.kt               Punto de entrada de la actividad
MainApplication.kt            Arranque de Koin
NetworkConnectivityManager.kt Monitor de conectividad (no conectado aún)
```

`MainActivity` y `MainApplication` declaran el paquete `pe.upeu.agrosierra.android`, coherente con el `applicationId`.

## Arquitectura y decisiones

- **Separación por capas.** `domain` no depende de Android ni de Ktor; `data` implementa los repositorios y traduce DTO a modelos de dominio; la interfaz Compose consume los repositorios a través de Koin.
- **DTO y serialización.** Los DTO usan `kotlinx.serialization`; el cliente configura `ignoreUnknownKeys`, `isLenient` y `explicitNulls = false` para tolerar cambios compatibles del servidor.
- **Cliente HTTP único.** Koin inyecta una sola instancia de `HttpClient` con `ContentNegotiation`, `Logging`, timeouts de 10 s (conexión) y 15 s (petición y socket), y `Auth` con `Bearer` centralizado. `expectSuccess = false` permite que `MapeoErrores` traduzca cada código HTTP a un `ErrorApi` tipado.
- **Sesión y token.** El token se guarda con `EncryptedSharedPreferences`. La cabecera `Authorization` se añade en el cliente y no en cada petición. Un `401` borra el token y vuelve al inicio de sesión; un `403` informa la operación no autorizada y conserva la sesión.
- **Persistencia local observable.** SQLDelight es la fuente de datos de la pantalla principal: lotes y labores se recolectan como `Flow` y la interfaz se actualiza al cambiar la base local.
- **Creación offline-first.** `registrarLabor` inserta la labor con identificador negativo y `pendiente = 1`. Al pulsar **Sincronizar** se envían primero las labores pendientes, se sustituyen los identificadores provisionales por los del servidor en una transacción y después se descargan lotes y labores.
- **Edición y eliminación.** Se envían directamente al servidor (`PUT` y `DELETE`); requieren conexión y no se encolan como pendientes.
- **Autorización en la interfaz.** Las acciones se muestran u ocultan según el conjunto `privilegios` devuelto por el login.

## Contrato del API consumido

La URL base se elige por plataforma:

- Emulador Android: `http://10.0.2.2:8080/`
- Dispositivo físico: `http://127.0.0.1:8080/`, con `adb reverse tcp:8080 tcp:8080`

| Método | Ruta | Uso en la aplicación |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Inicio de sesión; devuelve token, usuario, rol y privilegios |
| `GET` | `/api/v1/lotes?pagina=0&tamanio=100` | Descarga del padrón de lotes |
| `GET` | `/api/v1/lotes/{id}/labores?pagina=0&tamanio=100` | Descarga de labores del lote |
| `POST` | `/api/v1/labores` | Envío de labores pendientes |
| `PUT` | `/api/v1/labores/{id}` | Edición de una labor |
| `DELETE` | `/api/v1/labores/{id}` | Eliminación de una labor |

El backend expone además `GET /api/v1/lotes/{id}`, `POST /api/v1/lotes/{id}/cerrar-campania` y `GET /api/v1/usuarios/me`; esta aplicación todavía no los consume.

Los listados usan el envoltorio:

```json
{
  "contenido": [],
  "pagina": 0,
  "tamanio": 20,
  "totalElementos": 0,
  "totalPaginas": 0,
  "ultima": true
}
```

Los errores del backend tienen la forma:

```json
{
  "timestamp": "2026-10-27T18:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Datos invalidos",
  "path": "/api/v1/labores",
  "validationErrors": { "cantidad": "..." }
}
```

## Roles y privilegios

| Rol | Privilegios sembrados | Acciones ofrecidas en la interfaz |
| --- | --- | --- |
| `TECNICO` | `LOTE_LEER`, `LABOR_LEER`, `LABOR_CREAR`, `LABOR_EDITAR` | Ver lotes y labores; crear y editar labores |
| `JEFE_CAMPO` | `LOTE_LEER`, `LABOR_LEER`, `LABOR_CREAR`, `LABOR_EDITAR`, `LABOR_ELIMINAR`, `CAMPANIA_CERRAR` | Ver, crear, editar y eliminar labores |
| `ADMINISTRADOR` | `LOTE_LEER`, `LABOR_LEER`, `USUARIO_GESTIONAR` | Ver lotes y labores |

El backend siembra los usuarios `tecnico1`, `jefe1` y `admin`. La contraseña de demostración no se publica en este repositorio: se entrega en el anexo privado de la evaluación.

## Requisitos

- JDK 17.
- Android Studio con Android SDK 34, o el SDK de Android configurado por línea de comandos.
- Emulador o dispositivo con API 24 o superior.
- Backend `AgroSierraBackend` en ejecución en el puerto 8080.

Versiones principales: Gradle Wrapper 9.1.0, AGP 8.5.2, Kotlin 2.1.0, Compose BOM 2024.09.02, Ktor 2.3.12, Koin 3.5.6 y SQLDelight 2.0.2. `minSdk = 24`, `compileSdk = 34`, `targetSdk = 35`.

## Backend relacionado

Este proyecto móvil utiliza el backend desarrollado con Spring Boot.

- Backend: https://github.com/Jonas26-hash/AgroSierraBackend

## Ejecución

Windows:

```powershell
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :androidApp:assembleDebug
```

Linux o macOS:

```bash
./gradlew :shared:testDebugUnitTest
./gradlew :androidApp:assembleDebug
```

El APK de depuración se genera en:

```
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Instalación en un dispositivo conectado:

```powershell
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

Para un dispositivo físico, habilita la depuración USB y redirige el puerto del backend:

```powershell
adb reverse tcp:8080 tcp:8080
```

La aplicación permite tráfico HTTP en claro (`usesCleartextTraffic`) porque el backend local no usa TLS. No debe mantenerse así en una compilación de producción.

## Pruebas

```powershell
.\gradlew.bat :shared:testDebugUnitTest
```

Resultado en la validación local: **12 pruebas, 0 fallos**.

- `DominioTest` (4): mapeo de `LoteDto` a `Lote` y marcado de labores pendientes.
- `SyncYErroresTest` (4): identificadores negativos y positivos, mensaje de `ErrorApi` y enumeración `TipoLabor`.
- `SyncOrdenTest` (4): conversión de `LaborDto`, mapeo de un `400` con `validationErrors` mediante `MockEngine`, marcado de una labor pendiente y una comprobación de orden que registra manualmente los pasos `SUBIR_PENDIENTE` y `DESCARGAR_LOTES`. Esta última no ejecuta `AgroRepositoryImpl`.
