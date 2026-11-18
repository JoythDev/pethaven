import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideDownload, LucidePlus } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { parseRouteId } from '../../../../../core/http/route-id';
import { ViewState } from '../../../../../core/http/view-state';
import { identityHue } from '../../../../../core/identity/identity';
import { PetAreaConfig, VET_PET_AREA } from '../../../../../core/pets-list/pet-area';
import {
  HISTORY_LIMIT,
  TREATMENT_PARAM,
  formatKg,
  groupByYear,
  hiddenCount,
  lineCost,
  lineSubtotal,
  prescriptionCost,
  prescriptionProfit,
  prescriptionTotal,
  resolveSelected,
  sortNewestFirst,
  toggleLabel,
  evolutionSeverity,
  visibleTreatments,
} from '../../../../../core/pet-detail/pet-detail';
import { skeletonVisible } from '../../../../../core/ui/skeleton-timing';
import { Pet } from '../../../../../models/pet.model';
import { Treatment, evolutionLabel } from '../../../../../models/treatment.model';
import { CardComponent } from '../../../../shared/card/card.component';
import { DefinitionListComponent } from '../../../../shared/definition-list/definition-list.component';
import { DetailPanelComponent } from '../../../../shared/detail-panel/detail-panel.component';
import { formatCurrency } from '../../../../shared/drugs/drug-stock';
import { EmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../../../shared/inline-error/inline-error.component';
import { ShellSidebarService } from '../../../../shared/shell/shell-sidebar.service';
import { SkeletonComponent } from '../../../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../../../shared/sticker/sticker.component';
import { TreatmentTimelineComponent } from '../treatment-timeline/treatment-timeline.component';

/**
 * Pestaña «Historia clínica» (11.3, S23, S27): línea de tiempo de tratamientos (8 columnas) y, a la
 * derecha, el detalle del elegido (4 columnas, cajón en pantallas medianas y pequeñas). Los seis más
 * recientes se ven primero y «Mostrar N más» abre el resto. El elegido vive en `?tratamiento=<id>`.
 * El veterinario ve precio de venta, subtotal y total de la receta (11.10), nunca el de compra; el
 * administrador, además, el costo de compra de cada medicamento y de la receta, y la ganancia.
 */
@Component({
  selector: 'app-history-tab',
  imports: [
    DatePipe,
    RouterLink,
    ButtonModule,
    TagModule,
    LucideDownload,
    LucidePlus,
    CardComponent,
    DefinitionListComponent,
    DetailPanelComponent,
    EmptyStateComponent,
    InlineErrorComponent,
    SkeletonComponent,
    StickerComponent,
    TreatmentTimelineComponent,
  ],
  templateUrl: './history-tab.component.html',
})
export class HistoryTabComponent {
  readonly state = input.required<ViewState<Treatment[]>>();
  readonly pet = input.required<Pet>();
  /** Área que monta la ficha: decide la ruta del registro y si se muestran costos. */
  readonly area = input<PetAreaConfig>(VET_PET_AREA);
  /** El veterinario puede registrar un tratamiento a esta mascota (si no, no se ofrece la acción). */
  readonly canRegister = input(false);
  readonly downloading = input(false);
  readonly retry = output<void>();
  readonly download = output<void>();

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly shell = inject(ShellSidebarService);

  private readonly queryParams = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });

  protected readonly formatKg = formatKg;
  protected readonly formatCurrency = formatCurrency;
  protected readonly evolutionLabel = evolutionLabel;
  protected readonly evolutionSeverity = evolutionSeverity;
  protected readonly skeletonRows = [0, 1, 2, 3, 4, 5];

  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));

  private readonly sorted = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? sortNewestFirst(state.data) : [];
  });
  protected readonly selected = computed(() =>
    resolveSelected(this.sorted(), parseRouteId(this.queryParams().get(TREATMENT_PARAM))),
  );

  // «Mostrar N más»: los ocultos cambian solo con la lista; una selección fuera de los seis abre todos.
  private readonly expandedByUser = signal(false);
  private readonly shown = computed(() =>
    visibleTreatments(this.sorted(), this.expandedByUser(), this.selected()?.id ?? null),
  );
  protected readonly groups = computed(() => groupByYear(this.shown()));
  private readonly hidden = computed(() => hiddenCount(this.sorted()));
  protected readonly expanded = computed(() => this.hidden() > 0 && this.shown().length === this.sorted().length);
  protected readonly toggleText = computed(() =>
    this.hidden() > 0 ? toggleLabel(this.hidden(), this.expanded()) : null,
  );

  // Con la columna fija el detalle ocupa 4 columnas; por debajo de 1080 px va en un cajón.
  protected readonly withPanel = computed(() => this.shell.layout() === 'expanded');
  protected readonly panelOpen = signal(false);

  protected readonly detailHue = computed(() => {
    const selected = this.selected();
    return selected ? identityHue(selected.id) : 'honey';
  });

  // Medicamentos del elegido con su subtotal y el total de la receta (precio de venta); en el área
  // del administrador, también el costo de compra de cada línea, el de la receta y la ganancia.
  protected readonly prescription = computed(() => {
    const drugs = this.selected()?.drugs ?? [];
    return {
      rows: drugs.map((drug) => ({
        drug,
        hue: identityHue(drug.drugId),
        subtotal: lineSubtotal(drug),
        cost: lineCost(drug),
      })),
      total: prescriptionTotal(drugs),
      cost: prescriptionCost(drugs),
      profit: prescriptionProfit(drugs),
    };
  });

  protected select(treatment: Treatment): void {
    this.go(treatment.id);
    this.panelOpen.set(true);
  }

  protected toggle(): void {
    if (!this.expanded()) {
      this.expandedByUser.set(true);
      return;
    }
    this.expandedByUser.set(false);
    // Al plegar, una selección que queda oculta vuelve al tratamiento más reciente.
    const index = this.sorted().findIndex((t) => t.id === this.selected()?.id);
    if (index >= HISTORY_LIMIT) {
      this.go(null);
    }
  }

  protected units(units: number): string {
    return units === 1 ? '1 unidad' : `${units} unidades`;
  }

  private go(id: number | null): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { [TREATMENT_PARAM]: id },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
