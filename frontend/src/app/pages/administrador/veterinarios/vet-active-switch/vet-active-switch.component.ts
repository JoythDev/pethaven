import { Component, DestroyRef, computed, inject, input, linkedSignal, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { NotificationService } from '../../../../core/notifications/notification.service';
import { vetActiveToast } from '../../../../core/vets/vets-list';
import { Veterinarian } from '../../../../models/veterinarian.model';
import { VeterinarianService } from '../../../../services/veterinarian.service';

/**
 * Interruptor «Cuenta activa» (11.11). Aplica el cambio al momento con `VeterinarianService.setActive`
 * (los interruptores no piden confirmación, 10.3), muestra un estado ocupado, confirma con un aviso
 * y, si falla, devuelve el interruptor a su posición y avisa del error. Emite el veterinario
 * actualizado en `changed` para que la lista o la ficha se actualicen sin recargar.
 */
@Component({
  selector: 'app-vet-active-switch',
  imports: [FormsModule, ToggleSwitchModule],
  template: `
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
          >Cuenta activa</label
        >
        <p class="text-[12.5px] leading-[18px] text-ink-muted">Si la desactivas, no podrá iniciar sesión.</p>
      </div>
      <p-toggleswitch
        class="mt-0.5 shrink-0"
        [inputId]="inputId()"
        [ngModel]="checked()"
        [readonly]="busy()"
        (ngModelChange)="onToggle($event)"
      />
    </div>
  `,
  host: { class: 'block' },
})
export class VetActiveSwitchComponent {
  readonly vet = input.required<Veterinarian>();
  /** Veterinario con el estado ya guardado en el API. */
  readonly changed = output<Veterinarian>();

  private readonly service = inject(VeterinarianService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly busy = signal(false);
  // Sigue al veterinario; mientras se guarda refleja lo que la persona eligió.
  protected readonly checked = linkedSignal(() => this.vet().active);
  protected readonly inputId = computed(() => `vet-active-${this.vet().id}`);

  protected onToggle(next: boolean): void {
    const vet = this.vet();
    if (this.busy() || next === vet.active) {
      return;
    }
    this.checked.set(next);
    this.busy.set(true);
    this.service
      .setActive(vet.id, next)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.busy.set(false);
          this.checked.set(updated.active);
          const { summary, detail } = vetActiveToast(updated);
          this.notifications.success(summary, detail);
          this.changed.emit(updated);
        },
        error: (error: unknown) => {
          this.busy.set(false);
          this.checked.set(vet.active);
          this.notifications.error('No se pudo cambiar el estado del veterinario', error);
        },
      });
  }
}
