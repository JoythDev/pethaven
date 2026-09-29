import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PetService } from '../pet.service';

@Component({
  selector: 'app-pet-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pet-list.component.html'
})
export class PetListComponent {
  // Traemos la lista de mascotas directo del service, así el HTML
  // se actualiza solo cada vez que agregamos o borramos alguna
  pets;

  constructor(private petService: PetService) {
    this.pets = this.petService.petsList;
  }

  eliminar(id: number, nombre: string): void {
    const confirmado = confirm(`¿Eliminar a ${nombre}? Esta acción no se puede deshacer.`);
    if (confirmado) {
      this.petService.delete(id);
    }
  }
}
