import { Routes } from '@angular/router';
import { LandingComponent } from './pages/landing/landing.component';
import { PortalPlaceholderComponent } from './pages/portal-placeholder/portal-placeholder.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: PortalPlaceholderComponent },
  { path: 'vet/login', component: PortalPlaceholderComponent },

  // Panel del veterinario: layout propio e independiente de la landing.
  {
    path: 'veterinario',
    loadComponent: () =>
      import('./pages/veterinario/dashboard-layout/dashboard-layout.component').then(
        (m) => m.DashboardLayoutComponent
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/veterinario/home/home.component').then((m) => m.DashboardHomeComponent),
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

  // Placeholders de los portales que aún no existen.
  {
    path: 'cliente',
    loadComponent: () =>
      import('./pages/shared/coming-soon/coming-soon.component').then((m) => m.ComingSoonComponent),
    data: {
      title: 'Portal del cliente',
      description: 'El portal del cliente está en preparación.',
    },
  },
  {
    path: 'administrador',
    loadComponent: () =>
      import('./pages/shared/coming-soon/coming-soon.component').then((m) => m.ComingSoonComponent),
    data: {
      title: 'Portal del administrador',
      description: 'El portal del administrador está en preparación.',
    },
  },

  // Compatibilidad: la ruta anterior del panel redirige al panel del veterinario.
  { path: 'dashboard', redirectTo: 'veterinario', pathMatch: 'prefix' },

  { path: '**', redirectTo: '' }
];
