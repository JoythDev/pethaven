<div align="center">

<img src="docs/design/PetHaven-Logo.png" alt="PetHaven" width="320"/>

# PetHaven

**Hospitalización canina y felina con amor de hogar — porque son familia**

[![Java](https://img.shields.io/badge/Java-21-3E2C23?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19-DD0031?logo=angular&logoColor=white)](https://angular.dev)
[![PrimeNG](https://img.shields.io/badge/PrimeNG-19-06B6D4?logo=primeng&logoColor=white)](https://primeng.org)
[![RxJS](https://img.shields.io/badge/RxJS-7.8-B7178C?logo=reactivex&logoColor=white)](https://rxjs.dev)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-legacy_en_retiro-005F0F?logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org)
[![Database](https://img.shields.io/badge/Base_de_datos-H2-2563EB)](https://www.h2database.com)
[![Maven](https://img.shields.io/badge/Maven-Wrapper-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org)
[![License](https://img.shields.io/badge/Licencia-MIT-8A9A5B)](LICENSE)

<img src="docs/screenshots/landing.png" alt="Landing page de PetHaven" width="100%"/>

</div>

---

## Sobre PetHaven

PetHaven es una clínica veterinaria especializada en la **hospitalización de perros y gatos** que necesitan reposo y tratamiento dentro de sus instalaciones. Sus pacientes se quedan varios días bajo cuidado constante, mientras los veterinarios administran tratamientos hasta su recuperación.

Hasta ahora, buena parte de la operación vivía en papel y en hojas de Excel: el inventario de drogas, las atenciones médicas y el seguimiento de cada paciente. Este proyecto digitaliza ese negocio con una plataforma web que conecta a **tres tipos de usuario**:

| Rol | Qué puede hacer |
|---|---|
| **Cliente** (dueño) | Iniciar sesión, ver sus mascotas hospitalizadas en formato de tarjeta y consultar el detalle de cada una |
| **Veterinario** | Registrar dueños y mascotas, llevar el historial de atenciones y administrar tratamientos *(en desarrollo)* |
| **Administrador** | Gestionar el equipo de veterinarios y analizar los KPIs del negocio: mascotas en tratamiento, atenciones realizadas, ventas y ganancias *(en desarrollo)* |

**El flujo del negocio:**

1. El dueño llega por primera vez y deja sus datos personales (cédula, nombre, correo, celular).
2. El veterinario registra a la mascota: nombre, raza, edad, peso, enfermedad y foto.
3. La mascota queda hospitalizada mientras recibe tratamientos.
4. Al recuperarse y ser recogida, la mascota pasa a estado **inactivo** — su historial nunca se borra.

**En migración:** el proyecto avanza hacia un **monorepo** con el backend Spring Boot como API REST y un frontend **Angular + PrimeNG + RxJS** que reemplazará a las vistas Thymeleaf. El detalle está en [Estado de la migración](#estado-de-la-migración).

---

## Estado de la migración

El objetivo es dejar atrás el renderizado en el servidor: un solo frontend Angular (SPA) consumiendo una API REST del backend.

| Pieza | Hoy | Objetivo |
|---|---|---|
| Backend (`src/`) | Spring MVC + vistas Thymeleaf | API REST consumida por la SPA |
| Frontend (`frontend/`) | Demo funcional (mascotas y dueños) con datos en memoria | Único frontend de la aplicación |

**Fases:**

- [x] **Demo funcional** — landing portada a Angular y dashboard con los módulos de mascotas y dueños (listado, detalle, registro y edición) sobre datos de prueba en memoria; incluye activar/desactivar mascotas, eliminación de dueños en cascada y vínculos entre fichas.
- [ ] **API REST** — exponer los recursos del backend y conectarlos desde Angular con `HttpClient` + RxJS.
- [ ] **Módulos restantes** — veterinarios, medicamentos, tratamientos y KPIs en el dashboard.
- [ ] **Retiro de Thymeleaf** — cuando la SPA cubra toda la operación, las vistas de servidor salen del repositorio.

<div align="center">
  <img src="docs/screenshots/dashboard-pets.png" alt="Módulo de mascotas del dashboard Angular" width="100%"/>
  <br/>
  <sub>Módulo de mascotas — listado con búsqueda, filtro por especie y paginación (Angular + PrimeNG)</sub>
  <br/><br/>
  <img src="docs/screenshots/dashboard-pet-form.png" alt="Formulario de registro de mascota" width="100%"/>
  <br/>
  <sub>Registro de una mascota — formulario del demo</sub>
</div>

---

## Funcionalidades

### Implementadas — Entrega 1 (Features 1 a 7)

- [x] **Feature 01 — Diseño**: nombre, logo, paleta de colores y mockups navegables diferenciados por tipo de usuario
- [x] **Feature 02 — Repositorio**: GitHub público con ramas de `desarrollo` y `main` (producción)
- [x] **Feature 03 — Base de datos**: entidades JPA, repositorios Spring Data y datos de prueba que simulan el funcionamiento real
- [x] **Feature 04 — Landing page**: sitio promocional de la clínica con navegación hacia los portales
- [x] **Feature 05 — Portal cliente**: inicio de sesión y consulta de mascotas en tarjetas con su detalle
- [x] **Feature 06 — CRUD de dueños**: crear, consultar, actualizar y eliminar (con eliminación en cascada de sus mascotas)
- [x] **Feature 07 — CRUD de mascotas**: crear, consultar, actualizar y activar/desactivar

### Frontend Angular (demo en desarrollo)

- [x] Landing page portada a Angular, fiel a la versión Thymeleaf (secciones + directivas de animación)
- [x] Dashboard con layout propio (sidebar y topbar) e inicio
- [x] Módulo de mascotas: listado con búsqueda, filtro por especie y paginación (PrimeNG Table)
- [x] Detalle, registro y edición de mascotas, con activar/desactivar
- [x] Módulo de dueños: listado con búsqueda por nombre o documento y paginación; ficha con sus mascotas; registro, edición y eliminación en cascada
- [x] Vínculo mascota ↔ dueño: selector de dueño en el formulario de mascota, columna «Dueño» en el listado y navegación entre fichas
- [ ] Conexión con el backend vía API REST (`HttpClient` + RxJS)
- [ ] Módulos restantes: veterinarios, medicamentos, tratamientos y KPIs

> El demo funciona con datos de prueba en memoria; no requiere el backend encendido. Ver [Estado de la migración](#estado-de-la-migración).

### Roadmap

**Pendientes de la Entrega 1**

| Funcionalidad | Detalle |
|---|---|
| Búsqueda de mascotas | Buscar una mascota por nombre y mostrar su información junto con la de su dueño (AC21) |

**Entrega 2 (Features 8 a 11)**

| Funcionalidad | Detalle |
|---|---|
| Portal de administración | Registrar, actualizar y activar/desactivar veterinarios (Feature 08) |
| Dashboard de KPIs | Mascotas en tratamiento, atenciones, ventas, ganancias y top de tratamientos más vendidos (Feature 09) |
| Carga de medicamentos | Importar el inventario de drogas desde un archivo al iniciar el programa (Feature 09) |
| Tratamientos | Administrar una droga a una mascota y descontar el inventario disponible (Feature 10) |
| Historial médico | Consultar la lista de tratamientos dados a cada mascota (Feature 11) |
| SPA en Angular | En progreso — demo funcional (landing + mascotas + dueños); pendiente integración con la API REST y módulos restantes |

**Entrega 3**

| Funcionalidad | Detalle |
|---|---|
| Control de acceso por roles | Restringir cada funcionalidad según el rol del usuario (Feature 13) |
| Pruebas del cliente web | Al menos 2 casos de uso automatizados |
| Pruebas de backend | 25 pruebas automatizadas que cargan y limpian sus propios datos de prueba |

---

## Stack tecnológico

### Backend

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1 — Web MVC, Data JPA, DevTools |
| Objetivo | API REST para el frontend Angular |
| Base de datos | H2 en modo archivo, con consola de administración web |
| Utilidades | Lombok |
| Build | Maven Wrapper |

### Frontend (`frontend/`)

| Capa | Tecnología |
|---|---|
| Framework | Angular 19 — componentes standalone y lazy loading |
| UI | PrimeNG 19 con tema propio de PetHaven + Tailwind CSS 4 |
| Reactividad | RxJS 7.8 + signals |
| Lenguaje | TypeScript 5.7 |
| Tooling | Angular CLI + npm — requiere Node `^18.19.1 \|\| ^20.11.1 \|\| >=22` |

Thymeleaf queda como **legacy, en retiro**: se mantiene solo mientras la SPA termina de cubrir la operación.

---

## Estructura del proyecto

```
pethaven/
├── docs/
│   ├── architecture/              # Diagramas de dominio y entidad-relación
│   ├── design/                    # Logo e identidad de marca
│   ├── screenshots/               # Capturas para este README
│   └── Enunciado_Veterinaria.pdf  # Definición del negocio y requerimientos
│
├── frontend/                      # Frontend Angular 19
│   ├── public/                    # Imágenes, íconos y videos
│   └── src/
│       ├── app/
│       │   ├── models/            # Interfaces de dominio (mascota, dueño, droga…)
│       │   ├── pages/
│       │   │   ├── landing/       # Landing page (secciones + directivas de animación)
│       │   │   ├── dashboard/     # Panel: layout, inicio y módulos de mascotas y dueños
│       │   │   └── portal-placeholder/  # Placeholders de /login y /vet/login
│       │   ├── services/          # Servicios (mascotas y dueños sobre datos en memoria)
│       │   ├── theme/             # Preset PrimeNG de la marca
│       │   ├── app.config.ts      # Providers, locale es-CO y tema
│       │   └── app.routes.ts      # Rutas de la SPA
│       ├── styles.css             # Tailwind CSS y estilos globales
│       └── main.ts                # Punto de entrada
│
├── src/main/java/io/github/pethaven/   # Backend Spring Boot
│   ├── controller/                # Controladores MVC (rutas web)
│   ├── service/                   # Lógica de negocio
│   ├── repository/                # Repositorios Spring Data JPA
│   ├── entity/                    # Entidades JPA: Owner, Pet, Species, Treatment, TreatmentDrug, Drug, Veterinarian
│   ├── exception/                 # Manejo global de errores
│   ├── DataLoader.java            # Datos de prueba cargados al arrancar
│   └── PethavenBackendApplication.java
│
├── src/main/resources/
│   ├── templates/                 # Vistas Thymeleaf (legacy, en retiro)
│   ├── static/                    # CSS, JS, imágenes y videos (legacy)
│   └── application.properties     # Configuración (datasource, JPA, H2)
│
└── pom.xml
```

---

## Primeros pasos

### Requisitos previos

- **JDK 21** o superior — backend
- **Node.js** `^18.19.1 || ^20.11.1 || >=22` — frontend
- **Git** (para clonar el repositorio)
- No necesitas Maven instalado: el proyecto incluye el *Maven Wrapper*

### Backend

```bash
# 1. Clona el repositorio
git clone https://github.com/JoythDev/pethaven.git
cd pethaven

# 2. Arranca la aplicación
./mvnw spring-boot:run        # en Windows: .\mvnw.cmd spring-boot:run
```

La base de datos H2 se crea y puebla automáticamente con datos de prueba al primer arranque. La aplicación Thymeleaf (legacy) queda en **<http://localhost:8080>**.

### Frontend (demo)

```bash
cd frontend
npm install
npm start
```

La SPA queda en **<http://localhost:4200>**. Por ahora usa datos de prueba en memoria: no necesita el backend encendido (la integración con la API REST está en camino — ver [Estado de la migración](#estado-de-la-migración)).

### Credenciales de prueba

Puedes iniciar sesión como cliente con cualquiera de los dueños precargados:

| Correo | Contraseña |
|---|---|
| `john.doe@example.com` | `password123` |

### Consola de H2

La base de datos expone una consola web para explorar tablas y datos:

| Parámetro | Valor |
|---|---|
| URL | `http://localhost:8080/h2` |
| JDBC URL | `jdbc:h2:file:./pethaven-database` |
| Usuario | `sa` |
| Contraseña | *(vacía)* |

---

## Rutas disponibles

### Backend — Thymeleaf (legacy, `http://localhost:8080`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Landing page de la clínica |
| `GET` | `/login` | Formulario de acceso del cliente |
| `POST` | `/login` | Autenticación del cliente |
| `GET` | `/pets` | Listado de todas las mascotas |
| `GET` | `/pets/{id}` | Detalle de una mascota |
| `GET` / `POST` | `/pets/add` | Registrar una nueva mascota |
| `GET` / `POST` | `/pets/update/{id}` | Actualizar los datos de una mascota |
| `POST` | `/pets/toggle/{id}` | Activar / desactivar una mascota |
| `GET` | `/owners` | Listado de dueños |
| `GET` | `/owners/{id}` | Detalle de un dueño y sus mascotas |
| `GET` / `POST` | `/owners/add` | Registrar un nuevo dueño |
| `GET` / `POST` | `/owners/update/{id}` | Actualizar los datos de un dueño |
| `GET` | `/owners/delete/{id}` | Eliminar un dueño (y sus mascotas, en cascada) |
| `GET` | `/h2` | Consola de administración de H2 |

### Frontend — SPA Angular (`http://localhost:4200`)

| Ruta | Descripción |
|---|---|
| `/` | Landing page |
| `/login` | Acceso de clientes (placeholder) |
| `/vet/login` | Acceso de veterinarios (placeholder) |
| `/dashboard` | Inicio del panel |
| `/dashboard/pets` | Listado de mascotas |
| `/dashboard/pets/add` | Registrar una mascota |
| `/dashboard/pets/update/:id` | Editar una mascota |
| `/dashboard/pets/:id` | Detalle de una mascota |
| `/dashboard/owners` | Listado de dueños |
| `/dashboard/owners/add` | Registrar un dueño |
| `/dashboard/owners/update/:id` | Editar un dueño |
| `/dashboard/owners/:id` | Detalle de un dueño y sus mascotas |

---

## Estrategia de ramas

El repositorio sigue el flujo definido en el enunciado del proyecto:

```
main (producción)          ← lo que ve y califica el cliente
 └── desarrollo            ← integración de los cambios del equipo
      └── feature/*        ← una rama por funcionalidad
```

Los cambios se integran primero en `desarrollo`; solo cuando todo está funcional se promueven a `main`.

---

## Documentación

<details>
<summary><b>Identidad de marca</b></summary>
<br/>
<img src="docs/design/Brand-Identity.png" alt="Identidad de marca PetHaven" width="600"/>
</details>

<details>
<summary><b>Diagrama entidad-relación</b></summary>
<br/>
<img src="docs/architecture/Diagrama-Entidad-Relacion.png" alt="Diagrama entidad-relación" width="600"/>
</details>

<details>
<summary><b>Enunciado del proyecto</b></summary>
<br/>
La definición completa del negocio, los requerimientos y las entregas se encuentra en <a href="docs/Enunciado_Veterinaria.pdf">docs/Enunciado_Veterinaria.pdf</a>.
</details>

---

## Equipo

| Integrante      | GitHub                                              |
|-----------------|-----------------------------------------------------|
| Ángel García    | [DeluxeMovie](https://github.com/DeluxeMovie)       |
| Juliana Pacheco | [JuliiPa](https://github.com/JuliiPa)               |
| Manuel Rincon   | [ManuelRincon90](https://github.com/ManuelRincon90) |
| Nicolás Joya    | [JoythDev](https://github.com/JoythDev)             |

---

## Licencia

Este proyecto se distribuye bajo la [Licencia MIT](LICENSE).
