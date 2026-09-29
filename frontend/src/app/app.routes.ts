import { Routes } from '@angular/router';
import { PetListComponent } from './pets/pet-list/pet-list.component';
import { PetDetailComponent } from './pets/pet-detail/pet-detail.component';
import { PetFormComponent } from './pets/pet-form/pet-form.component';

export const routes: Routes = [
  { path: 'pets', component: PetListComponent },
  { path: 'pets/add', component: PetFormComponent },
  { path: 'pets/update/:id', component: PetFormComponent },
  { path: 'pets/:id', component: PetDetailComponent }
];
