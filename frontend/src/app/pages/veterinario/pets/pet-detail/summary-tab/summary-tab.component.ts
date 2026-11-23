import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TagModule } from 'primeng/tag';

import { ViewState } from '../../../../../core/http/view-state';
import { identityHue } from '../../../../../core/identity/identity';
import { PetAreaConfig, VET_PET_AREA } from '../../../../../core/pets-list/pet-area';
import {
  TAB_PARAM,
  TREATMENT_PARAM,
  ageLabel,
  evolutionSeverity,
  formatKg,
  sortNewestFirst,
  vaccineSeverity,
  visitSummary,
} from '../../../../../core/pet-detail/pet-detail';
import { CLOCK } from '../../../../../core/time/clock';
import { skeletonVisible } from '../../../../../core/ui/skeleton-timing';
import { MIN_CURVE_POINTS, weightCurve } from '../../../../../core/weight-curve/weight-curve';
import { VaccineCard } from '../../../../../models/demo/vaccine.model';
import { Pet, Species } from '../../../../../models/pet.model';
import { Treatment, evolutionLabel } from '../../../../../models/treatment.model';
import { WEIGHT_RANGES, WeightPoint, WeightRange } from '../../../../../models/weight.model';
import { CardComponent } from '../../../../shared/card/card.component';
import { DefinitionItem, DefinitionListComponent } from '../../../../shared/definition-list/definition-list.component';
import { DemoTagComponent } from '../../../../shared/demo-tag/demo-tag.component';
import { EmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../../../shared/inline-error/inline-error.component';
import { SkeletonComponent } from '../../../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../../../shared/sticker/sticker.component';
import { WeightChartComponent } from '../../../../shared/weight-chart/weight-chart.component';

const UPCOMING_VACCINES = 3;

/**
 * Pestaña «Resumen» (11.2): curva de peso (demo), último tratamiento real, datos de la mascota,
 * próximas vacunas (demo) y visitas. Cada fuente carga y falla por separado; quien la usa atiende
 * «Reintentar» de cada una.
 */
@Component({
  selector: 'app-summary-tab',
  imports: [
    DatePipe,
    RouterLink,
    FormsModule,
    ButtonModule,
    SelectButtonModule,
    TagModule,
    CardComponent,
    DefinitionListComponent,
    DemoTagComponent,
    EmptyStateComponent,
    InlineErrorComponent,
    SkeletonComponent,
    StickerComponent,
    WeightChartComponent,
  ],
  templateUrl: './summary-tab.component.html',
})
export class SummaryTabComponent {
  readonly pet = input.required<Pet>();
  /** Área que monta la ficha: decide si se nombra al dueño (y si es un enlace) y cómo se llama la pestaña de tratamientos. */
  readonly area = input<PetAreaConfig>(VET_PET_AREA);
  readonly treatments = input.required<ViewState<Treatment[]>>();
  readonly weight = input.required<ViewState<WeightPoint[]>>();
  readonly vaccines = input.required<ViewState<VaccineCard>>();
  readonly retryTreatments = output<void>();
  readonly retryWeight = output<void>();
  readonly retryVaccines = output<void>();

  protected readonly evolutionLabel = evolutionLabel;
  protected readonly evolutionSeverity = evolutionSeverity;
  protected readonly vaccineSeverity = vaccineSeverity;
  protected readonly historyParams = (treatment: Treatment) => ({ [TAB_PARAM]: this.area().history.param, [TREATMENT_PARAM]: treatment.id });
  protected readonly vaccinesParams = { [TAB_PARAM]: 'vacunas' };

  protected readonly weightRanges = WEIGHT_RANGES;
  protected readonly weightRange = signal<WeightRange>(6);
  private readonly now = inject(CLOCK);
  // El segmento elige el tramo; null si en él hay muy pocos pesajes para una curva.
  protected readonly visibleWeight = computed(() => {
    const state = this.weight();
    return state.status === 'ready' ? weightCurve(state.data, this.weightRange(), this.now()) : null;
  });
  protected readonly weightEmptyText = computed(
    () => `La curva aparece con ${MIN_CURVE_POINTS} pesajes en los últimos ${this.weightRange()} meses. Peso actual: ${formatKg(this.pet().weight)}.`,
  );

  protected readonly weightSkeleton = skeletonVisible(computed(() => this.weight().status === 'loading'));
  protected readonly treatmentsSkeleton = skeletonVisible(computed(() => this.treatments().status === 'loading'));
  protected readonly vaccinesSkeleton = skeletonVisible(computed(() => this.vaccines().status === 'loading'));

  protected readonly facts = computed<DefinitionItem[]>(() => {
    const pet = this.pet();
    return [
      { term: 'Especie', value: pet.species === Species.CAT ? 'Gato' : 'Perro' },
      { term: 'Raza', value: pet.breed },
      { term: 'Edad', value: ageLabel(pet.age) },
      { term: 'Peso actual', value: formatKg(pet.weight) },
      { term: 'Diagnóstico actual', value: pet.disease ?? 'Ninguno' },
    ];
  });

  protected readonly sorted = computed(() => {
    const state = this.treatments();
    return state.status === 'ready' ? sortNewestFirst(state.data) : [];
  });
  protected readonly latest = computed(() => this.sorted()[0] ?? null);
  protected readonly visits = computed(() => visitSummary(this.sorted()));
  protected readonly upcoming = computed(() => {
    const state = this.vaccines();
    return state.status === 'ready'
      ? [...state.data.vaccines].sort((a, b) => a.daysUntilDue - b.daysUntilDue).slice(0, UPCOMING_VACCINES)
      : [];
  });

  protected hue(id: number): string {
    return identityHue(id);
  }

  protected units(units: number): string {
    return units === 1 ? '1 unidad' : `${units} unidades`;
  }
}
