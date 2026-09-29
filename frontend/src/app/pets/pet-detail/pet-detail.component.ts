import { Component } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PetService } from '../pet.service';
import { Pet } from '../pet.model';

@Component({
  selector: 'app-pet-detail',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pet-detail.component.html'
})
export class PetDetailComponent {
  pet: Pet | undefined;

  constructor(route: ActivatedRoute, private petService: PetService) {
    // Tomamos el id que viene en la URL (ej. /pets/3) y buscamos esa mascota
    const id = Number(route.snapshot.paramMap.get('id'));
    this.pet = this.petService.getById(id);
  }
}
