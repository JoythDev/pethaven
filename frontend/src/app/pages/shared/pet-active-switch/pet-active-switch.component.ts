import { Component, DestroyRef, computed, inject, input, linkedSignal, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { NotificationService } from '../../../core/notifications/notification.service';
import { petActiveToast } from '../../../core/pets-list/pets-list';
import { Pet } from '../../../models/pet.model';
import { AuthService } from '../../../services/auth.service';
import { PetService } from '../../../services/pet.service';

/**
 * Interruptor «Mascota activa» (11.11). Aplica el cambio al momento con `PetService.setActive`
 * (los interruptores no piden confirmación, 10.3), muestra un estado ocupado, confirma con un aviso
 * y, si falla, devuelve el interruptor a su posición y avisa del error. Emite la mascota
 * actualizada en `changed` para que la lista o el formulario se actualicen sin recargar.
 * Solo lo ven los veterinarios: para otros roles no se dibuja nada.
 */
@Component({
  selector: 'app-pet-active-switch',
  imports: [FormsModule, ToggleSwitchModule],
  template: `
    @if (visible()) {
      <div
        class="relative flex items-start gap-3 py-1 transition-opacity motion-reduce:transition-none"
        [class.opacity-60]="busy()"
        [attr.aria-busy]="busy()"
      >
        <div class="min-w-0 flex-1">
          <!-- La etiqueta cubre toda la fila: el área táctil supera los 44 px sin agrandar el interruptor. -->
          <label
            class="block cursor-pointer text-sm leading-5 font-extrabold text-ink after:absolute after:inset-0 after:content-['']"
            [attr.for]="inputId()"
            >Mascota activa</label
          >
          <p class="text-[12.5px] leading-[18px] text-ink-muted">Las mascotas inactivas no admiten tratamientos nuevos.</p>
        </div>
        <p-toggleswitch
          class="mt-0.5 shrink-0"
          [inputId]="inputId()"
          [ngModel]="checked()"
          [readonly]="busy()"
          (ngModelChange)="onToggle($event)"
        />
      </div>
    }
  `,
  host: { class: 'block' },
})
export class PetActiveSwitchComponent {
  readonly pet = input.required<Pet>();
  /** Mascota con el estado ya guardado en el API. */
  readonly changed = output<Pet>();

  private readonly petService = inject(PetService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly auth = inject(AuthService);

  protected readonly busy = signal(false);
  // Sigue a la mascota; mientras se guarda refleja lo que la persona eligió.
  protected readonly checked = linkedSignal(() => this.pet().active);
  protected readonly inputId = computed(() => `pet-active-${this.pet().id}`);
  protected readonly visible = computed(() => this.auth.user()?.role === 'VETERINARIAN');

  protected onToggle(next: boolean): void {
    const pet = this.pet();
    if (this.busy() || next === pet.active) {
      return;
    }
    this.checked.set(next);
    this.busy.set(true);
    this.petService
      .setActive(pet.id, next)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.busy.set(false);
          this.checked.set(updated.active);
          const { summary, detail } = petActiveToast(updated);
          this.notifications.success(summary, detail);
          this.changed.emit(updated);
        },
        error: (error: unknown) => {
          this.busy.set(false);
          this.checked.set(pet.active);
          this.notifications.error('No se pudo cambiar el estado de la mascota', error);
        },
      });
  }
}
