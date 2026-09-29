import { Component, OnInit, Signal, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PetService } from '../../../services/pet.service';
import { Pet } from '../../../models/pet.model';
import { PetRowComponent } from './components/pet-row/pet-row.component';

@Component({
  selector: 'app-pet-list',
  standalone: true,
  imports: [RouterLink, PetRowComponent],
  templateUrl: './pet-list.component.html'
})
export class PetListComponent implements OnInit {
  private readonly petService = inject(PetService);

  // Se asigna en ngOnInit (no en la declaración) para dejar el punto de
  // carga inicial listo para cuando esto pase a ser una petición HTTP real.
  pets!: Signal<Pet[]>;

  ngOnInit(): void {
    this.pets = this.petService.petsList;
  }

  eliminar(pet: Pet): void {
    const confirmado = confirm(`¿Eliminar a ${pet.name}? Esta acción no se puede deshacer.`);
    if (confirmado) {
      this.petService.delete(pet.id);
    }
  }
}
