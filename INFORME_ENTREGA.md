# Informe de avance y evidencias — AgroSierra

**Fecha de revisión:** 27 de octubre de 2026  
**Caso:** Examen Parcial II, Unidad 2, Versión A  
**Repositorios:** AgroCampoApp (KMP Android) y AgroSierraBackend (Spring Boot)

## Resultado implementado

- Interfaz Android Compose con login JWT, diseño oscuro translúcido Liquid Glass, padrón, filtros de valle/campaña, detalle de labores, formulario, perfil y logout.
- Contratos de dominio y persistencia local SQLDelight para padrón y labores.
- Registro local de labor con ID provisional negativo y marca `pendiente`.
- Sincronización que publica primero pendientes y reemplaza el ID provisional por el ID del servidor dentro de transacción antes de descargar lotes.
- Cuatro pruebas básicas en `commonTest` y README en cada proyecto.

## Pendiente de comprobar

- El backend compila correctamente con Maven 3.9.11 (`clean compile`) después de corregir el tipo de autoridades en el filtro JWT. La compilación Android no terminó: el wrapper falta y Gradle disponible en caché se quedó sin memoria antes de crear el APK (la máquina reportó 98% de memoria usada). El daemon de Docker está instalado, pero actualmente apagado.
- Las cuatro pruebas actuales cubren mapeo de datos de dominio y estados de labor. Aún falta el conjunto exacto de la guía: `MockEngine`, controlador SQLDelight en memoria, registro offline y orden de sincronización.
- La sincronización descarga el padrón de la primera página y refresca labores al abrir un lote; para una entrega completa conviene probar paginación y expiración JWT extremo a extremo. Edición y eliminación todavía no están conectadas en la app y requieren implementarse para cubrir el CRUD completo.
- El backend ya tenía estructura y reglas, pero requiere `mvn clean compile` y arranque real para confirmar el entorno.

## Inicio rápido del backend

Docker Desktop está instalado, pero su motor no estaba arrancado en esta sesión. Si lo prefieres: abrir Docker Desktop y esperar a que el motor esté activo; desde `AgroSierraBackend`, ejecutar `docker compose up --build`. Alternativamente, ejecutar `mvn spring-boot:run` desde esa carpeta con el Maven del equipo.

El servicio usa puerto `8080`; el emulador Android accede mediante `http://10.0.2.2:8080/`.

## Trazabilidad Git

Las carpetas locales entregadas no contienen `.git`. No se fabricaron ramas, fusiones, fechas ni tags porque la guía califica el historial auténtico distribuido en el tiempo. Recupera/clona los repositorios remotos y continúa en ramas feature con commits reales; después integra a `develop` y etiqueta solo el commit que hayas ejecutado.

## Capturas para la evidencia

1. **Backend iniciado:** consola mostrando Spring Boot levantado en el puerto 8080.
2. **Autenticación:** respuesta de login con token ocultando el valor sensible y datos de usuario/privilegios.
3. **Paginación y filtros:** respuesta de `GET /api/v1/lotes` con valle/estado y envoltorio de paginación.
4. **Seguridad:** un endpoint protegido sin token (401) y una operación sin privilegio (403).
5. **Reglas de negocio:** capturas de RN-01, RN-02, RN-03 (409) y RN-04 (400 con error de `cantidad`).
6. **Android — login y padrón:** aplicación en emulador con el diseño final y filtro visible.
7. **Android — detalle y registro:** lista de labores y formulario de nueva labor.
8. **Offline-first:** activar modo avión, guardar labor y mostrar indicio pendiente; restaurar red, sincronizar y comprobar ID definitivo.
9. **Roles:** iniciar sesión con técnico y jefe y mostrar las acciones disponibles distintas.
10. **Pruebas:** salida de `:shared:allTests` cuando se completen los escenarios requeridos.
11. **Git en ambos repos:** `git log --graph --oneline --all`, historial con fechas, PR/MR cerradas y tag `v1.0-unidad2` apuntando al commit evaluado.

## Datos de demostración

Usuarios del backend: `tecnico1`, `jefe1`, `admin`; contraseña `Agro2026*`. En emulador Android, el cliente usa `10.0.2.2:8080` para acceder al backend de la máquina anfitriona.
