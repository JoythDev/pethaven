# Roadmap de despliegue a producción

- **Estado:** planificado, sin implementar.
- **Fecha:** 2026-11-03.
- **Ramas:** `desarrollo` (integración) y `main` (producción).

## 1. Objetivo

Tener Pethaven (backend y frontend) en producción y funcional, con un flujo CI/CD en GitHub Actions que permita seguir desarrollando de forma segura. El primer despliegue real se realizará solo cuando la seguridad (Spring Security + BCrypt + JWT) esté implementada.

## 2. Estado de partida

| Área | Situación actual |
|---|---|
| Backend | Spring Boot 4.1, Java 21, Maven. Un único `application.properties` con H2 en archivo. |
| Frontend | Angular 19. `environment.production.ts` apunta a `http://localhost:8080/api`. |
| Base de datos | H2, `ddl-auto=create-drop`, consola H2 y log SQL activos. Sin migraciones. |
| Datos | `DataLoader` (datos de ejemplo) y `ExcelDrugImportRunner` (medicamentos) se ejecutan en cada arranque, sin distinguir entorno. |
| Seguridad | Sin Spring Security. Contraseñas en texto plano. Endpoints `/api/**` sin autenticación. |
| Infraestructura | Sin Dockerfiles, Compose, profiles ni `.github/`. |
| Pruebas | 12 clases de test de backend (mayoría de repositorio). Tests e2e de frontend sin definir. |

## 3. Decisiones tomadas

| # | Tema | Decisión |
|---|---|---|
| D1 | Base de datos en producción | PostgreSQL gestionado por Render. Docker Compose solo para uso local. |
| D2 | Seguridad | Spring Security + BCrypt + JWT es una fase aparte, posterior a las pruebas. Este roadmap se desarrolla antes, pero el primer deploy real espera a que la seguridad esté implementada. |
| D3 | Datos en producción | La base arranca vacía, excepto los medicamentos (datos reales del negocio, desde el Excel). Sin datos de ejemplo. |
| D4 | Hosting backend | Render. |
| D5 | Hosting frontend | Vercel. |
| D6 | Modelo de CD | GitHub Actions ejecuta el CI y, por separado, el CD que dispara el despliegue (CI y CD diferenciados). |
| D7 | Migraciones | Flyway. |
| D8 | Flujo de ramas | `desarrollo → main` siempre por PR. Solo entorno de producción (sin staging ni previews). |
| D9 | Dominio | Subdominios de las plataformas (`*.onrender.com`, `*.vercel.app`). |
| D10 | H2 | Se mantiene en desarrollo y pruebas. |

### Pendientes y recomendaciones no confirmadas

| Tema | Estado |
|---|---|
| Herramienta e2e de frontend | **Pendiente de decidir.** La recomendación inicial es Playwright, sujeta a la definición de la fase de pruebas de frontend. |
| Plan de Postgres en Render | Recomendación: plan de pago para producción. El plan gratuito tiene caducidad. Verificar condiciones vigentes al contratar. |
| Origen de la imagen en Render | Recomendación: Render despliega la imagen publicada en GHCR por el CI (un solo artefacto para CI y CD). Verificar soporte y credenciales para registro privado al implementar. |
| Validación de migraciones | Recomendación: job de CI con Postgres real (service container) que ejecute Flyway y `ddl-auto=validate`. |

## 4. Principios

- El CI valida; el CD despliega. Son workflows distintos.
- Se construye una sola vez: la imagen que pasa el CI es la que se despliega.
- Ninguna configuración sensible vive en el repositorio. Todo secreto va por variables de entorno de la plataforma o secretos de GitHub.
- El CD permanece desactivado (solo `workflow_dispatch`) hasta la Fase 9.

## 5. Resumen de fases y dependencias

| Fase | Nombre | Depende de |
|---|---|---|
| 1 | Profiles y configuración del backend | — |
| 2 | PostgreSQL y Flyway | 1 |
| 3 | Datos de producción (medicamentos) | 1, 2 |
| 4 | Docker y Docker Compose | 1, 2, 3 |
| 5 | Integración continua (CI) | 1, 4 (job de imagen); 2 (job de migraciones) |
| 6 | Configuración de producción del frontend | 1 |
| 7 | Aprovisionamiento en Render y Vercel | 4, 6 |
| 8 | Entrega continua (CD) | 5, 7 |
| — | **Fuera del roadmap:** fase de pruebas de frontend/e2e y fase de seguridad | — |
| 9 | Primer despliegue a producción | 8, seguridad implementada y, si se decide, e2e en el CI |
| 10 | Operación y endurecimiento | 9 |

Las fases 1 a 8 pueden completarse antes de la seguridad. Las fases 5 y 6 pueden avanzar en paralelo una vez cumplidas sus dependencias.

## 6. Detalle por fase

### Fase 1. Profiles y configuración del backend

**Dependencias:** ninguna.

**Desarrollar o configurar:**
- Separar la configuración en `application.properties` (común), `application-dev.properties`, `application-test.properties` y `application-prod.properties`.
- `dev` y `test`: H2, consola H2 y log SQL permitidos solo en `dev`.
- `prod`: sin consola H2, sin log SQL, `ddl-auto=validate`, Swagger desactivado (o protegido tras la seguridad), seed de ejemplo desactivado.
- Parametrizar por variables de entorno en `prod`: conexión a la base, `app.cors.allowed-origins` y `server.port=${PORT:8080}`.
- Limitar `DataLoader` a los profiles `dev` y `test` (`@Profile`).
- Añadir Actuator con el endpoint `/actuator/health` para los health checks.

**Criterio de cierre:** la aplicación arranca con cada profile y el comportamiento de `dev` no cambia respecto al actual.

### Fase 2. PostgreSQL y Flyway

**Dependencias:** Fase 1.

**Desarrollar o configurar:**
- Añadir el driver de PostgreSQL y las dependencias de Flyway al `pom.xml`.
- Crear `src/main/resources/db/migration/V1__baseline.sql` a partir del esquema generado por las entidades JPA.
- En `prod`: Flyway activo y `ddl-auto=validate`. En `dev` y `test`: Flyway desactivado y esquema generado por Hibernate.
- Usar variables separadas (host, puerto, base, usuario, contraseña) para construir la URL JDBC. Render entrega `DATABASE_URL` con formato `postgresql://`, no JDBC.
- Definir la convención de migraciones: toda modificación de entidades que cambie el esquema incluye su migración en el mismo PR.

**Nota:** la migración base debe generarse cuando el modelo de entidades esté estable. La fase de seguridad puede añadir migraciones posteriores (por ejemplo, tokens de refresco).

**Criterio de cierre:** contra un Postgres local vacío, el backend con profile `prod` aplica las migraciones y valida el esquema sin errores.

### Fase 3. Datos de producción (medicamentos)

**Dependencias:** Fases 1 y 2.

**Desarrollar o configurar:**
- Mantener `ExcelDrugImportRunner` solo en `prod` (y en los profiles de desarrollo que lo necesiten).
- Hacer la importación **idempotente**: debe verificarse si `DrugImportService.importFromExcel` ya lo es. Si no, definir una clave natural de medicamento y actualizar o ignorar los existentes. Un reinicio no puede duplicar datos ni sobrescribir el stock real.
- Definir el comportamiento ante fallo de la importación (hoy solo se registra el error y el arranque continúa).
- Documentar cómo actualizar el catálogo de medicamentos en el futuro.

**Criterio de cierre:** arrancar dos veces seguidas contra la misma base vacía deja la misma cantidad de medicamentos y ningún dato de ejemplo.

### Fase 4. Docker y Docker Compose

**Dependencias:** Fases 1, 2 y 3.

**Desarrollar o configurar:**
- `Dockerfile` del backend, multi-stage: build con JDK 21 y Maven Wrapper, runtime con JRE 21, usuario no root, `SPRING_PROFILES_ACTIVE=prod` por defecto.
- `.dockerignore` (excluye `target/`, `node_modules/`, `*.mv.db`, `.git`).
- `docker-compose.yml` para uso local: PostgreSQL con volumen y healthcheck, y el backend en profile `prod` apuntando a esa base. Sirve para probar el perfil de producción en la máquina local.
- `.env.example` con las variables necesarias (sin valores reales). El `.env` ya está ignorado por Git.
- Dockerfile del frontend: **no es necesario** porque Vercel compila Angular por su cuenta. Solo se crearía si se decide autoalojar.

**Criterio de cierre:** `docker compose up` levanta Postgres y el backend, las migraciones se aplican y `/actuator/health` responde correctamente.

### Fase 5. Integración continua (CI)

**Dependencias:** Fase 1 (profile `test`). Fase 2 para el job de migraciones. Fase 4 para el job de imagen.

**Desarrollar o configurar** (workflows en `.github/workflows/`, ejecutados en PRs hacia `desarrollo` y `main`):
- **Backend:** `mvn verify` con Java 21 y caché de Maven.
- **Migraciones:** job con un Postgres como service container que ejecuta Flyway y `ddl-auto=validate`.
- **Frontend:** `npm ci`, build de producción y tests unitarios.
- **Imagen:** construcción de la imagen Docker del backend. En `main`, publicación en GHCR etiquetada con el SHA del commit.
- **E2E:** job reservado hasta que se defina la herramienta (decisión pendiente). Los e2e correrán contra backend con profile `test` y frontend levantados en el job.
- **Protección de `main`:** PR obligatorio, checks del CI obligatorios y sin push directo.

**Criterio de cierre:** un PR hacia `main` no puede mezclarse si falla algún check.

### Fase 6. Configuración de producción del frontend

**Dependencias:** Fase 1 (variable de CORS definida).

**Desarrollar o configurar:**
- Reemplazar `apiBaseUrl` en `environment.production.ts` por la URL pública del backend en Render. Como Angular la fija en tiempo de build, el nombre del servicio de Render debe estar decidido de antemano.
- Añadir `vercel.json` con la reescritura de SPA (todas las rutas a `index.html`) y la desactivación del despliegue automático desde Git (`git.deploymentEnabled: false`), para que el despliegue lo ejecute únicamente el CD.
- Revisar los presupuestos de tamaño del build (`angular.json`) frente al resultado real.

**Criterio de cierre:** el build de producción genera un artefacto que apunta a la URL esperada y las rutas profundas funcionan con la reescritura.

### Fase 7. Aprovisionamiento en Render y Vercel

**Dependencias:** Fases 4 y 6.

**Desarrollar o configurar:**
- **Render:** crear el servicio web del backend, la instancia de PostgreSQL gestionado y las variables de entorno (profile `prod`, conexión, `app.cors.allowed-origins` con el subdominio de Vercel, y el secreto JWT cuando exista). Configurar el health check (`/actuator/health`) y el despliegue por imagen o por Dockerfile según lo verificado.
- **Vercel:** crear el proyecto del frontend con la integración Git desactivada.
- **GitHub:** crear los secretos para el CD (Deploy Hook de Render, token y IDs de proyecto de Vercel, credenciales de GHCR si el registro es privado).
- Fijar los nombres de ambos servicios, de los que dependen las URLs y el CORS.

**Criterio de cierre:** ambos servicios existen y están configurados, sin tráfico público ni despliegue automático.

### Fase 8. Entrega continua (CD)

**Dependencias:** Fases 5 y 7.

**Desarrollar o configurar:**
- Workflow de CD separado del CI, con disparador **solo manual** (`workflow_dispatch`) hasta la Fase 9.
- Backend: invocar el Deploy Hook de Render apuntando a la imagen publicada por el CI para el commit de `main`.
- Frontend: `vercel build` y `vercel deploy --prebuilt --prod` con el token de Vercel.
- Smoke test posterior al despliegue contra `/actuator/health`, con reintentos y espera ampliada por posibles arranques en frío.
- Documentar el procedimiento de rollback (redesplegar la imagen del SHA anterior).

**Criterio de cierre:** el workflow existe, está validado en sus pasos y no se ejecuta automáticamente.

### Fase 9. Primer despliegue a producción

**Dependencias:** Fase 8, seguridad implementada y, si se decide, e2e integrado en el CI.

**Condiciones previas (checklist):**
- Spring Security, BCrypt y JWT implementados; ningún endpoint sensible queda sin autenticar.
- Secreto JWT y credenciales definidos en las variables de la plataforma.
- Swagger desactivado o protegido; consola H2 inexistente en `prod`.
- Pruebas de backend (y e2e, si aplica) en verde en el CI.
- Plan de pago de Postgres contratado o, en su defecto, aceptado el riesgo de la caducidad.

**Acciones:**
- Mezclar `desarrollo` en `main` por PR.
- Activar el disparador automático del CD sobre `main` (o ejecutarlo manualmente la primera vez).
- Verificar manualmente el flujo completo: login, alta de mascota, tratamientos y lectura del catálogo de medicamentos.

### Fase 10. Operación y endurecimiento

**Dependencias:** Fase 9.

**Desarrollar o configurar:**
- Política de backups de la base y verificación de restauración.
- Revisión de logs y alertas básicas de disponibilidad.
- Estrategia de versiones/etiquetas para `main` y procedimiento de rollback probado.
- Reevaluar la necesidad de staging, previews o dominio propio.

## 7. Notas de integración con la fase de seguridad

- La seguridad no está en este roadmap, pero condiciona las fases 7 y 9 (secreto JWT, CORS con cabecera `Authorization`).
- Se recomienda enviar el token como cabecera `Authorization` en lugar de cookies: Vercel y Render pertenecen a sitios distintos y las cookies entre sitios añaden restricciones.
- El login actual del frontend guarda el usuario en `sessionStorage`. La integración de JWT modificará el servicio de autenticación, los guards y los interceptores, y con ello los e2e que se hayan escrito antes.
- Cualquier cambio de esquema introducido por la seguridad se entrega como nueva migración de Flyway.
