import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { TooltipModule } from 'primeng/tooltip';

import { DayTimeline, TimelineSegment } from '../../../core/day-timeline/day-timeline';
import { AvatarComponent } from '../avatar/avatar.component';
import { BlockComponent } from '../block/block.component';
import { DemoTagComponent } from '../demo-tag/demo-tag.component';
import { StickerComponent } from '../sticker/sticker.component';

/**
 * «Resumen del día» (11.6) de la fecha dada: línea de tiempo con franjas de 30 min, marcas de hora
 * e indicador de «ahora»; debajo, la cita que sigue (hoy), la primera (día futuro) o el aviso de
 * día terminado, y tres cifras. Cada franja es enfocable y lleva un tooltip de
 * PrimeNG. Solo presenta: el estado de carga y de error lo pone quien la usa.
 */
@Component({
  selector: 'app-day-summary',
  imports: [DatePipe, TooltipModule, AvatarComponent, BlockComponent, DemoTagComponent, StickerComponent],
  templateUrl: './day-summary.component.html',
  styleUrl: './day-summary.component.css',
  host: { class: 'block' },
})
export class DaySummaryComponent {
  readonly timeline = input.required<DayTimeline>();
  readonly date = input.required<Date>();

  protected readonly slotCount = computed(() => this.timeline().segments.reduce((sum, s) => sum + s.span, 0));
  protected readonly figures = computed(() => {
    const { total, urgent, free } = this.timeline().figures;
    return [
      { value: total, label: total === 1 ? 'cita' : 'citas' },
      { value: urgent, label: urgent === 1 ? 'urgencia' : 'urgencias' },
      { value: free, label: free === 1 ? 'libre' : 'libres' },
    ];
  });

  protected segmentClass(segment: TimelineSegment): string {
    const kind = segment.free ? (segment.past ? 'seg-free seg-past' : 'seg-free') : `hue-${segment.hue}`;
    return `seg ph-focus ${kind}${segment.completed ? ' seg-done' : ''}`;
  }
}
