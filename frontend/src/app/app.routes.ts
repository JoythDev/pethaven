import { Route, Routes } from '@angular/router';
import { LandingComponent } from './pages/landing/landing.component';
import { ClienteEntryComponent } from './pages/cliente/cliente-entry/cliente-entry.component';
import { ADMIN_DRUG_AREA, DRUG_AREA_DATA_KEY, VET_DRUG_AREA } from './pages/shared/drugs/drug-area';
import { guestGuard, ownerPortalGuard, roleGuard } from './core/auth/auth.guard';

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
      comingSoon('agenda', 'Agenda', 'Aquí verás las citas del día y de la semana de tu consulta.'),
      notificationsRoute(),
      {
        path: 'perfil',
        loadComponent: () =>
          import('./pages/veterinario/perfil/perfil.component').then(
            (m) => m.VeterinarioPerfilComponent
          ),
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
          },
          {
            path: 'update/:id',
            loadComponent: () =>
              import('./pages/veterinario/pets/pet-form/pet-form.component').then(
                (m) => m.PetsFormComponent
              ),
          },
          {
            path: ':id/historia',
            loadComponent: () =>
              import('./pages/veterinario/pets/clinical-history/clinical-history.component').then(
                (m) => m.ClinicalHistoryComponent
              ),
          },
          {
            path: ':id/treatments/add',
            loadComponent: () =>
              import('./pages/veterinario/pets/treatment-form/treatment-form.component').then(
                (m) => m.TreatmentFormComponent
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
      {
        path: 'duenos',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/veterinario/owners/owners-list/owners-list.component').then(
                (m) => m.OwnersListComponent
              ),
          },
          {
            path: 'add',
            loadComponent: () =>
              import('./pages/veterinario/owners/owner-form/owner-form.component').then(
                (m) => m.OwnersFormComponent
              ),
          },
          {
            path: 'update/:id',
            loadComponent: () =>
              import('./pages/veterinario/owners/owner-form/owner-form.component').then(
                (m) => m.OwnersFormComponent
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
        loadComponent: () =>
          import('./pages/administrador/veterinarios/vets-list/vets-list.component').then(
            (m) => m.VetsListComponent
          ),
      },
      {
        path: 'veterinarios/add',
        loadComponent: () =>
          import('./pages/administrador/veterinarios/vet-form/vet-form.component').then(
            (m) => m.VetFormComponent
          ),
      },
      {
        path: 'veterinarios/update/:id',
        loadComponent: () =>
          import('./pages/administrador/veterinarios/vet-form/vet-form.component').then(
            (m) => m.VetFormComponent
          ),
      },
      {
        path: 'mascotas',
        loadComponent: () =>
          import('./pages/administrador/mascotas/admin-pets-list.component').then(
            (m) => m.AdminPetsListComponent
          ),
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
