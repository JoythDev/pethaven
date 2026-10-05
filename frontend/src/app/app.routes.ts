import { Routes } from '@angular/router';
import { LandingComponent } from './pages/landing/landing.component';
import { PortalPlaceholderComponent } from './pages/portal-placeholder/portal-placeholder.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: PortalPlaceholderComponent },
  { path: 'vet/login', component: PortalPlaceholderComponent },

  // Panel post-login (dashboard): layout propio e independiente de la landing.
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard-layout/dashboard-layout.component').then(
        (m) => m.DashboardLayoutComponent
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/dashboard/home/home.component').then((m) => m.DashboardHomeComponent),
      },
      {
        path: 'pets',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/dashboard/pets/pets-list/pets-list.component').then(
                (m) => m.PetsListComponent
              ),
          },
          {
            path: 'add',
            loadComponent: () =>
              import('./pages/dashboard/pets/pet-form/pet-form.component').then(
                (m) => m.PetsFormComponent
              ),
          },
          {
            path: 'update/:id',
            loadComponent: () =>
              import('./pages/dashboard/pets/pet-form/pet-form.component').then(
                (m) => m.PetsFormComponent
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/dashboard/pets/pet-detail/pet-detail.component').then(
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
              import('./pages/dashboard/owners/owners-list/owners-list.component').then(
                (m) => m.OwnersListComponent
              ),
          },
          {
            path: 'add',
            loadComponent: () =>
              import('./pages/dashboard/owners/owner-form/owner-form.component').then(
                (m) => m.OwnersFormComponent
              ),
          },
          {
            path: 'update/:id',
            loadComponent: () =>
              import('./pages/dashboard/owners/owner-form/owner-form.component').then(
                (m) => m.OwnersFormComponent
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/dashboard/owners/owner-detail/owner-detail.component').then(
                (m) => m.OwnersDetailComponent
              ),
          },
        ],
      },
    ],
  },

  { path: '**', redirectTo: '' }
];
