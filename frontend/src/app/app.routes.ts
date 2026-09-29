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

  { path: '**', redirectTo: '' }
];
