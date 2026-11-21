import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { YearGroup, evolutionSeverity } from '../../../../../core/pet-detail/pet-detail';
import { TagSeverity } from '../../../../../core/status/pet-status';
import { Treatment, evolutionLabel } from '../../../../../models/treatment.model';

const MARKER: Record<TagSeverity, string> = {
  success: 'bg-success',
  info: 'bg-info',
  warn: 'bg-warning',
  danger: 'bg-danger',
  secondary: 'bg-line-strong',
};

/**
 * Línea de tiempo de tratamientos (10.16): eventos agrupados por año, cada uno con fecha, diagnóstico,
 * veterinario y etiqueta de evolución. Cada evento es un botón con `aria-pressed`; quien la usa decide
 * qué hay seleccionado y cuántos eventos entrega, y atiende «Mostrar N más».
 */
@Component({
  selector: 'app-treatment-timeline',
  imports: [DatePipe, ButtonModule, TagModule],
  templateUrl: './treatment-timeline.component.html',
  host: { class: 'block' },
})
export class TreatmentTimelineComponent {
  readonly groups = input.required<YearGroup[]>();
  readonly selectedId = input<number | null>(null);
  /** Etiqueta del botón de ampliar; sin ella no se muestra. */
  readonly toggleText = input<string | null>(null);
  readonly expanded = input(false);
  readonly selectTreatment = output<Treatment>();
  readonly toggle = output<void>();

  protected readonly evolutionLabel = evolutionLabel;
  protected readonly evolutionSeverity = evolutionSeverity;

  protected marker(treatment: Treatment): string {
    return MARKER[evolutionSeverity(treatment.evolution)];
  }

  protected drugCount(treatment: Treatment): string {
    const count = treatment.drugs.length;
    return count === 0 ? 'Sin medicamentos' : count === 1 ? '1 medicamento' : `${count} medicamentos`;
  }
}
