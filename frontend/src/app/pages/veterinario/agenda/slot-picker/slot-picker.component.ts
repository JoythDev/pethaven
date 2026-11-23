import { Component, computed, input, output } from '@angular/core';

import { BookableDay, dayLabel, slotOptions } from '../../../../core/client-appointments/client-appointments';
import { Clinic } from '../../../../models/clinic.model';

const PICK_CLASS =
  'ph-focus flex min-w-0 cursor-pointer flex-col items-center gap-1 rounded-box border-[1.5px] border-line bg-card text-center font-bold text-ink ' +
  'transition-colors duration-[180ms] ease-out-strong hover:bg-sunken motion-reduce:transition-none ' +
  'aria-pressed:border-accent aria-pressed:bg-accent-soft aria-pressed:shadow-[inset_0_0_0_1px_var(--color-accent)] ' +
  'disabled:cursor-not-allowed disabled:opacity-40 disabled:line-through disabled:hover:bg-card';

/**
 * Día y hora de una cita del veterinario (diálogos «Nueva cita» y «Reprogramar»): días como fichas y,
 * debajo, todas las franjas del día según el horario de la clínica; las que el veterinario no tiene
 * libres quedan deshabilitadas, nunca ocultas (11.5). Solo presenta: quien lo usa pide la disponibilidad.
 */
@Component({
  selector: 'app-slot-picker',
  templateUrl: './slot-picker.component.html',
  host: { class: 'block' },
})
export class SlotPickerComponent {
  readonly days = input.required<BookableDay[]>();
  readonly clinic = input.required<Pick<Clinic, 'hours' | 'slotMinutes'>>();
  readonly day = input<string | null>(null);
  readonly time = input<string | null>(null);

  readonly dayChange = output<string>();
  readonly timeChange = output<string>();

  protected readonly pickClass = PICK_CLASS;
  protected readonly dayLabel = dayLabel;

  protected readonly selectedDay = computed(() => this.days().find((d) => d.key === this.day()) ?? null);
  protected readonly slots = computed(() => {
    const day = this.selectedDay();
    return day ? slotOptions(day, this.clinic()) : [];
  });
  protected readonly dayFull = computed(() => this.slots().every((slot) => slot.disabled));
}
