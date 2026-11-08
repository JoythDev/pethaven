import { Routes } from '@angular/router';
import { LandingComponent } from './pages/landing/landing.component';
import { LoginComponent } from './pages/auth/login/login.component';
import { ClienteEntryComponent } from './pages/cliente/cliente-entry/cliente-entry.component';
import { ADMIN_DRUG_AREA, DRUG_AREA_DATA_KEY, VET_DRUG_AREA } from './pages/shared/drugs/drug-area';
import { ownerPortalGuard, roleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent, data: { kind: 'owner' } },
  { path: 'vet/login', component: LoginComponent, data: { kind: 'vet' } },
  { path: 'admin/login', component: LoginComponent, data: { kind: 'admin' } },

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

  // Panel del administrador: gestiona veterinarios, mascotas (se desactivan, no se borran) y medicamentos.
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
