import { Routes } from '@angular/router';
import { PetListComponent } from './pages/pets/pet-list/pet-list.component';
import { PetDetailComponent } from './pages/pets/pet-detail/pet-detail.component';
import { PetFormComponent } from './pages/pets/pet-form/pet-form.component';

export const routes: Routes = [
  // Las rutas específicas van antes que 'pets/:id' para que Angular no
  // interprete 'add' o 'update' como si fueran un id de mascota.
  { path: 'pets', component: PetListComponent },
  { path: 'pets/add', component: PetFormComponent },
  { path: 'pets/update/:id', component: PetFormComponent },
  { path: 'pets/:id', component: PetDetailComponent }
];
