import { Routes } from '@angular/router';
import { LandingComponent } from './pages/landing/landing.component';
import { PortalPlaceholderComponent } from './pages/portal-placeholder/portal-placeholder.component';
import { PetListComponent } from './pages/pets/pet-list/pet-list.component';
import { PetDetailComponent } from './pages/pets/pet-detail/pet-detail.component';
import { PetFormComponent } from './pages/pets/pet-form/pet-form.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: PortalPlaceholderComponent },
  { path: 'vet/login', component: PortalPlaceholderComponent },

  // Las rutas específicas van antes que 'pets/:id' para que Angular no
  // interprete 'add' o 'update' como si fueran un id de mascota.
  { path: 'pets', component: PetListComponent },
  { path: 'pets/add', component: PetFormComponent },
  { path: 'pets/update/:id', component: PetFormComponent },
  { path: 'pets/:id', component: PetDetailComponent },

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
    ],
  },

  { path: '**', redirectTo: '' }
];
