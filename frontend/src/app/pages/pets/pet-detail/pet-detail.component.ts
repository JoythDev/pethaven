import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PetService } from '../../../services/pet.service';
import { Pet } from '../../../models/pet.model';

@Component({
  selector: 'app-pet-detail',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pet-detail.component.html'
})
export class PetDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly petService = inject(PetService);

  pet: Pet | undefined;

  ngOnInit(): void {
    // Tomamos el id que viene en la URL (ej. /pets/3) y buscamos esa mascota
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.pet = this.petService.getById(id);
  }
}
