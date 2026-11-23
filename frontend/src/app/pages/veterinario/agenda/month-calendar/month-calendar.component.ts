import { Component, ElementRef, Injector, afterNextRender, computed, inject, input, output, signal } from '@angular/core';

import { addDays, dayKey, isSameDay, isSunday } from '../../../../core/time/calendar';
import { DayCount } from '../../../../models/appointment.model';
import { dayCellLabel, monthGrid } from '../../../../core/vet-agenda/vet-agenda';

interface CalendarCell {
  key: string;
  date: Date;
  day: number;
  label: string;
  today: boolean;
  selected: boolean;
  hasAppointments: boolean;
  classes: string;
}

const CELL_CLASS =
  'ph-focus relative h-9 rounded-field text-[13px] leading-[18px] font-bold tabular-nums transition-colors ' +
  'duration-[180ms] ease-out-strong [--ph-focus-offset:1px] motion-reduce:transition-none ';

const WEEKDAYS = ['L', 'M', 'X', 'J', 'V', 'S', 'D'] as const;

// Salto de teclado: flechas por día y semana, inicio y fin de la semana; RePág y AvPág cambian de mes.
const KEY_STEP: Record<string, (date: Date) => Date> = {
  ArrowLeft: (d) => addDays(d, -1),
  ArrowRight: (d) => addDays(d, 1),
  ArrowUp: (d) => addDays(d, -7),
  ArrowDown: (d) => addDays(d, 7),
  Home: (d) => addDays(d, -((d.getDay() + 6) % 7)),
  End: (d) => addDays(d, 6 - ((d.getDay() + 6) % 7)),
  PageUp: (d) => new Date(d.getFullYear(), d.getMonth() - 1, Math.min(d.getDate(), 28)),
  PageDown: (d) => new Date(d.getFullYear(), d.getMonth() + 1, Math.min(d.getDate(), 28)),
};

/**
 * Calendario del mes (10.17): celdas de 36 px con el lunes primero, hoy con anillo, día elegido en
 * `accent`, punto en los días con citas y días cerrados atenuados. Un solo botón entra en el orden
 * de tabulación y las flechas mueven el foco (patrón de foco itinerante); Intro o Espacio eligen.
 * El mes visible lo decide quien lo usa: al salir de él con el teclado emite `monthChange`.
 */
@Component({
  selector: 'app-month-calendar',
  templateUrl: './month-calendar.component.html',
  host: { class: 'block' },
})
export class MonthCalendarComponent {
  /** Cualquier fecha del mes que se muestra. */
  readonly month = input.required<Date>();
  readonly selected = input.required<Date>();
  readonly today = input.required<Date>();
  /** Citas por día del mes; null mientras cargan o si fallaron (el calendario sigue usable). */
  readonly counts = input<DayCount[] | null>(null);
  readonly monthLabel = input.required<string>();

  readonly daySelect = output<Date>();
  readonly monthChange = output<Date>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  protected readonly weekdays = WEEKDAYS;
  // Día que tiene el foco por teclado; null hasta que se use una flecha.
  private readonly active = signal<Date | null>(null);

  private readonly byKey = computed(() => new Map((this.counts() ?? []).map((c) => [c.dateKey, c])));

  protected readonly weeks = computed<(CalendarCell | null)[][]>(() =>
    monthGrid(this.month()).map((week) =>
      week.map((date) => {
        if (!date) {
          return null;
        }
        const key = dayKey(date);
        const count = this.byKey().get(key) ?? null;
        const today = isSameDay(date, this.today());
        const selected = isSameDay(date, this.selected());
        const closed = count?.closed ?? isSunday(date);
        return {
          key,
          date,
          day: date.getDate(),
          label: dayCellLabel(date, count),
          today,
          selected,
          hasAppointments: (count?.count ?? 0) > 0,
          classes:
            CELL_CLASS +
            (selected ? 'bg-accent text-on-accent' : closed ? 'text-ink-muted opacity-55 hover:bg-sunken' : 'hover:bg-sunken') +
            (today ? ' shadow-[inset_0_0_0_1.5px_var(--color-accent)]' : ''),
        };
      }),
    ),
  );

  // Único botón con tabindex 0: el del foco por teclado si sigue en este mes; si no, el elegido o el primero.
  protected readonly tabStop = computed(() => {
    const month = this.month();
    const inMonth = (date: Date | null): date is Date =>
      !!date && date.getFullYear() === month.getFullYear() && date.getMonth() === month.getMonth();
    const target = [this.active(), this.selected()].find(inMonth) ?? new Date(month.getFullYear(), month.getMonth(), 1);
    return dayKey(target);
  });

  protected onKeydown(event: KeyboardEvent, cell: CalendarCell): void {
    const step = KEY_STEP[event.key];
    if (!step || event.altKey || event.ctrlKey || event.metaKey) {
      return;
    }
    event.preventDefault();
    const target = step(cell.date);
    this.active.set(target);
    const month = this.month();
    if (target.getFullYear() !== month.getFullYear() || target.getMonth() !== month.getMonth()) {
      this.monthChange.emit(target);
    }
    // El botón nuevo puede ser de otro mes: se enfoca cuando ya se pintó.
    afterNextRender(() => this.focusDay(dayKey(target)), { injector: this.injector });
  }

  private focusDay(key: string): void {
    this.host.nativeElement.querySelector<HTMLButtonElement>(`[data-day="${key}"]`)?.focus();
  }
}
