import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Pet } from '../../../../../models/pet.model';

// Subcomponente local de pet-list: pinta una fila de la tabla y le avisa
// al padre cuándo se quiere borrar (el padre decide si confirma o no).
// El selector va como atributo (tr[app-pet-row]) para que el componente
// se adjunte a un <tr> real y no rompa la semántica de la tabla.
@Component({
  selector: 'tr[app-pet-row]',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pet-row.component.html',
  styleUrl: './pet-row.component.css'
})
export class PetRowComponent {
  pet = input.required<Pet>();
  deletePet = output<Pet>();

  onDeleteClick(): void {
    this.deletePet.emit(this.pet());
  }
}
