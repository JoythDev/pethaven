import { Component, input } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { Pet } from '../../../../../models/pet.model';

/** Tag de estado clínico: verde si está estable, ámbar si tiene diagnóstico. */
@Component({
  selector: 'app-pet-status-tag',
  imports: [TagModule],
  templateUrl: './pet-status-tag.component.html',
})
export class PetStatusTagComponent {
  readonly pet = input.required<Pet>();
}
