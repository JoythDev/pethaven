import { Routes } from '@angular/router';
import { LandingComponent } from './pages/landing/landing.component';
import { LoginComponent } from './pages/auth/login/login.component';
import { ClienteEntryComponent } from './pages/cliente/cliente-entry/cliente-entry.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent, data: { kind: 'owner' } },
  { path: 'vet/login', component: LoginComponent, data: { kind: 'vet' } },
  { path: 'admin/login', component: LoginComponent, data: { kind: 'admin' } },

  // Panel del veterinario: layout propio sobre el shell compartido.
  {
    path: 'veterinario',
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
        path: 'perfil',
        loadComponent: () =>
          import('./pages/veterinario/perfil/perfil.component').then(
            (m) => m.VeterinarioPerfilComponent
          ),
      },
      {
        path: 'pets',
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
        path: 'owners',
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
    ],
  },

  // El login del dueño navega aquí: resuelve el id del usuario y entra a sus mascotas.
  { path: 'cliente', component: ClienteEntryComponent },

  // Portal del cliente: acceso directo por id, sin guard.
  {
    path: 'cliente/:ownerId',
    loadComponent: () =>
      import('./pages/cliente/cliente-layout/cliente-layout.component').then(
        (m) => m.ClienteLayoutComponent
      ),
    children: [
      { path: '', redirectTo: 'mascotas', pathMatch: 'full' },
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
      {
        path: 'perfil',
        loadComponent: () =>
          import('./pages/cliente/perfil/perfil.component').then((m) => m.PerfilComponent),
      },
    ],
  },

  // Panel del administrador: gestiona veterinarios y mascotas (se desactivan, no se borran).
  {
    path: 'administrador',
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
    ],
  },

  // Compatibilidad: la ruta anterior del panel redirige al panel del veterinario.
  { path: 'dashboard', redirectTo: 'veterinario', pathMatch: 'prefix' },

  { path: '**', redirectTo: '' }
];
