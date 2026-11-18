import { inject } from '@angular/core';
import { Route, Router, Routes } from '@angular/router';
import { LandingComponent } from './pages/landing/landing.component';
import { ClienteEntryComponent } from './pages/cliente/cliente-entry/cliente-entry.component';
import { ADMIN_DRUG_AREA, DRUG_AREA_DATA_KEY, VET_DRUG_AREA } from './pages/shared/drugs/drug-area';
import { ADMIN_PET_AREA, PET_AREA_DATA_KEY } from './core/pets-list/pet-area';
import { guestGuard, ownerPortalGuard, roleGuard } from './core/auth/auth.guard';
import { unsavedChangesGuard } from './core/forms/unsaved-changes.guard';
import { ownersListMatcher, resourceListMatcher } from './core/owners/owners-route';

// Sección que aún no tiene pantalla: usa el placeholder compartido con título y descripción de la ruta.
const comingSoon = (path: string, title: string, description: string): Route => ({
  path,
  loadComponent: () =>
    import('./pages/shared/coming-soon/coming-soon.component').then((m) => m.ComingSoonComponent),
  data: { title, description },
});

// Pestaña Notificaciones: una sola pantalla para los tres roles; el contenido sale de la sesión.
const notificationsRoute = (): Route => ({
  path: 'notificaciones',
  loadComponent: () =>
    import('./pages/shared/notifications/notifications-page.component').then((m) => m.NotificationsPageComponent),
});

export const routes: Routes = [
  // Solo sin sesión: con sesión, guestGuard lleva al portal del rol.
  { path: '', component: LandingComponent, canActivate: [guestGuard] },
  // Carga diferida: así el acceso (con Lucide y los campos de PrimeNG) no entra en el paquete inicial.
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/auth/login/login.component').then((m) => m.LoginComponent),
  },

  // Panel del veterinario: layout propio sobre el shell compartido.
  {
    path: 'veterinario',
    canActivate: [roleGuard('VETERINARIAN')],
    loadComponent: () =>
      import('./pages/veterinario/layout/veterinario-layout.component').then(
        (m) => m.VeterinarioLayoutComponent
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/veterinario/home/home.component').then((m) => m.DashboardHomeComponent),
      },
      {
        path: 'agenda',
        loadComponent: () =>
          import('./pages/veterinario/agenda/agenda.component').then((m) => m.AgendaComponent),
      },
      notificationsRoute(),
      {
        path: 'perfil',
        loadComponent: () =>
          import('./pages/veterinario/perfil/perfil.component').then(
            (m) => m.VeterinarioPerfilComponent
          ),
        canDeactivate: [unsavedChangesGuard],
      },
      // Compatibilidad: las rutas anteriores en inglés redirigen a las nuevas conservando el resto de la URL.
      { path: 'pets', redirectTo: 'mascotas', pathMatch: 'prefix' },
      { path: 'owners', redirectTo: 'duenos', pathMatch: 'prefix' },
      { path: 'drugs', redirectTo: 'medicamentos', pathMatch: 'prefix' },
      {
        path: 'mascotas',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/veterinario/pets/pets-list/pets-list.component').then(
                (m) => m.PetsListComponent
              ),
          },
          {
            path: 'add',
            loadComponent: () =>
              import('./pages/veterinario/pets/pet-form/pet-form.component').then(
                (m) => m.PetsFormComponent
              ),
            canDeactivate: [unsavedChangesGuard],
          },
          {
            path: 'update/:id',
            loadComponent: () =>
              import('./pages/veterinario/pets/pet-form/pet-form.component').then(
                (m) => m.PetsFormComponent
              ),
            canDeactivate: [unsavedChangesGuard],
          },
          // La historia clínica ahora es una pestaña de la ficha; el enlace anterior sigue funcionando.
          {
            path: ':id/historia',
            redirectTo: ({ params }) =>
              inject(Router).createUrlTree(['/veterinario/mascotas', params['id']], {
                queryParams: { pestana: 'historia' },
              }),
          },
          {
            path: ':id/treatments/add',
            loadComponent: () =>
              import('./pages/veterinario/pets/treatment-form/treatment-form.component').then(
                (m) => m.TreatmentFormComponent
              ),
            canDeactivate: [unsavedChangesGuard],
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/veterinario/pets/pet-detail/pet-detail.component').then(
                (m) => m.PetsDetailComponent
              ),
          },
        ],
      },
      {
        path: 'duenos',
        children: [
          {
            // Lista y diálogo de alta o edición: «», «add» y «update/:id» son una sola ruta, así la
            // lista no se recrea al abrir o cerrar el diálogo.
            matcher: ownersListMatcher,
            loadComponent: () =>
              import('./pages/veterinario/owners/owners-list/owners-list.component').then(
                (m) => m.OwnersListComponent
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/veterinario/owners/owner-detail/owner-detail.component').then(
                (m) => m.OwnersDetailComponent
              ),
          },
        ],
      },
      {
        path: 'medicamentos',
        // Solo consulta: el veterinario no crea, edita ni elimina medicamentos.
        data: { [DRUG_AREA_DATA_KEY]: VET_DRUG_AREA },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/shared/drugs/drugs-list/drugs-list.component').then((m) => m.DrugsListComponent),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/shared/drugs/drug-detail/drug-detail.component').then((m) => m.DrugDetailComponent),
          },
        ],
      },
    ],
  },

  // El login del dueño navega aquí: resuelve el id del usuario y entra a sus mascotas.
  { path: 'cliente', component: ClienteEntryComponent },

  // Portal del cliente: solo el propio dueño (su :ownerId) con sesión de rol OWNER.
  {
    path: 'cliente/:ownerId',
    canActivate: [ownerPortalGuard],
    loadComponent: () =>
      import('./pages/cliente/cliente-layout/cliente-layout.component').then(
        (m) => m.ClienteLayoutComponent
      ),
    children: [
      comingSoon('', 'Inicio', 'Aquí verás el resumen de tus mascotas, tu próxima cita y el consejo del día.'),
      {
        path: 'mascotas',
        loadComponent: () =>
          import('./pages/cliente/mascotas/mascotas.component').then((m) => m.MascotasComponent),
      },
      {
        path: 'mascotas/:petId',
        loadComponent: () =>
          import('./pages/cliente/mascota-detalle/mascota-detalle.component').then(
            (m) => m.MascotaDetalleComponent
          ),
      },
      comingSoon('citas', 'Citas', 'Aquí podrás agendar y revisar las citas de tus mascotas.'),
      notificationsRoute(),
      {
        path: 'perfil',
        loadComponent: () =>
          import('./pages/cliente/perfil/perfil.component').then((m) => m.PerfilComponent),
      },
    ],
  },

  // Panel del administrador: gestiona veterinarios y medicamentos, y consulta las mascotas.
  {
    path: 'administrador',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () =>
      import('./pages/administrador/layout/admin-layout.component').then((m) => m.AdminLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/administrador/dashboard/admin-dashboard.component').then(
            (m) => m.AdminDashboardComponent
          ),
      },
      {
        path: 'veterinarios',
        children: [
          {
            // Lista y diálogo de alta o edición: «», «add» y «update/:id» son una sola ruta, así la
            // lista no se recrea al abrir o cerrar el diálogo.
            matcher: resourceListMatcher,
            loadComponent: () =>
              import('./pages/administrador/veterinarios/vets-list/vets-list.component').then(
                (m) => m.VetsListComponent
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/administrador/veterinarios/vet-detail/vet-detail.component').then(
                (m) => m.VetDetailComponent
              ),
          },
        ],
      },
      {
        path: 'mascotas',
        // Solo consulta: la lista y la ficha del veterinario, con el área del administrador.
        data: { [PET_AREA_DATA_KEY]: ADMIN_PET_AREA },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/veterinario/pets/pets-list/pets-list.component').then(
                (m) => m.PetsListComponent
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/veterinario/pets/pet-detail/pet-detail.component').then(
                (m) => m.PetsDetailComponent
              ),
          },
        ],
      },
      notificationsRoute(),
      comingSoon('perfil', 'Perfil', 'Aquí podrás consultar los datos de tu cuenta de administrador.'),
      {
        path: 'medicamentos',
        // Gestión completa: listado, detalle, alta, edición, eliminación e importación desde Excel.
        data: { [DRUG_AREA_DATA_KEY]: ADMIN_DRUG_AREA },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/shared/drugs/drugs-list/drugs-list.component').then((m) => m.DrugsListComponent),
          },
          {
            path: 'add',
            loadComponent: () =>
              import('./pages/administrador/medicamentos/drug-form/drug-form.component').then(
                (m) => m.DrugFormComponent
              ),
          },
          {
            path: 'update/:id',
            loadComponent: () =>
              import('./pages/administrador/medicamentos/drug-form/drug-form.component').then(
                (m) => m.DrugFormComponent
              ),
          },
          {
            path: 'import',
            loadComponent: () =>
              import('./pages/administrador/medicamentos/drug-import/drug-import.component').then(
                (m) => m.DrugImportComponent
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/shared/drugs/drug-detail/drug-detail.component').then((m) => m.DrugDetailComponent),
          },
        ],
      },
    ],
  },

  // Compatibilidad: la ruta anterior del panel redirige al panel del veterinario.
  { path: 'dashboard', redirectTo: 'veterinario', pathMatch: 'prefix' },

  { path: '**', redirectTo: '' }
];
