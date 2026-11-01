import { Component, input } from '@angular/core';
import { TagModule } from 'primeng/tag';

/**
 * Estado de la mascota para el cliente: hospitalizada o en casa. Es propio del
 * área del cliente (el tag del veterinario habla del estado clínico, no de la
 * hospitalización), pero se comparte entre el listado y el detalle.
 */
@Component({
  selector: 'app-cliente-pet-status-tag',
  imports: [TagModule],
  templateUrl: './cliente-pet-status-tag.component.html',
})
export class ClientePetStatusTagComponent {
  /** Indica si la mascota está hospitalizada. */
  readonly active = input.required<boolean>();
}
