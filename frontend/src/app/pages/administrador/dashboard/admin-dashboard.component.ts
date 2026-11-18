import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideDownload, LucideFileUp, LucidePlus } from '@lucide/angular';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TagModule } from 'primeng/tag';
import { BehaviorSubject, of, switchMap } from 'rxjs';

import {
  PERIOD_OPTIONS,
  STOCK_MINIMUM,
  compactCurrency,
  formatPeriodRange,
  inventoryAlerts,
  limitList,
  monthLabels,
  parsePeriodParam,
  periodQueryParams,
  relativeWidths,
  share,
  trendOf,
} from '../../../core/admin-dashboard/admin-dashboard';
import { drugStatus } from '../../../core/drugs/drugs-list';
import { identityHue } from '../../../core/identity/identity';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { AdministratorDashboardResponse } from '../../../models/administrator-dashboard.model';
import { DashboardPeriod, DashboardPeriodId } from '../../../models/demo/dashboard-period.model';
import { Drug } from '../../../models/drug.model';
import { AdministratorDashboardService } from '../../../services/administrator-dashboard.service';
import { DEMO_NOW } from '../../../services/demo/demo-latency';
import { DemoDashboardPeriodsService } from '../../../services/demo/demo-dashboard-periods.service';
import { DrugService } from '../../../services/drug.service';
import { formatCurrency, formatUnits } from '../../shared/drugs/drug-stock';
import { BlockComponent } from '../../shared/block/block.component';
import { CardComponent } from '../../shared/card/card.component';
import { DemoTagComponent } from '../../shared/demo-tag/demo-tag.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { KpiComponent } from '../../shared/kpi/kpi.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { ShellSidebarDirective } from '../../shared/shell/shell-sidebar.directive';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';
import { StockMeterComponent } from '../../shared/stock-meter/stock-meter.component';
import { RatioRingComponent } from './ratio-ring.component';
import { SalesChartComponent } from './sales-chart.component';

const LOADING = { status: 'loading' } as const;
const TOP_TREATMENTS = 5;
const INVENTORY_ROWS = 4;
const DEMO_SUMMARY = 'Función demo';

/**
 * Panorama del administrador (11.6): todas las métricas del API (ventas, ganancia, tratamientos,
 * tratamientos por medicamento, más vendidos, equipo y mascotas) como elementos de primera fila, más las
 * demo de alrededor (variación por periodo, serie de ventas y ocupación). El periodo vive en `?periodo=`;
 * en «Todo» las ventas y la ganancia son los totales reales. El inventario usa la regla real de existencias.
 */
@Component({
  selector: 'app-admin-dashboard',
  imports: [
    FormsModule,
    NgTemplateOutlet,
    RouterLink,
    ButtonModule,
    SelectButtonModule,
    TagModule,
    LucideDownload,
    LucideFileUp,
    LucidePlus,
    BlockComponent,
    CardComponent,
    DemoTagComponent,
    EmptyStateComponent,
    InlineErrorComponent,
    KpiComponent,
    PageHeaderComponent,
    RatioRingComponent,
    SalesChartComponent,
    ShellSidebarDirective,
    SkeletonComponent,
    StickerComponent,
    StockMeterComponent,
  ],
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent {
  private readonly dashboardService = inject(AdministratorDashboardService);
  private readonly periodsService = inject(DemoDashboardPeriodsService);
  private readonly drugService = inject(DrugService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly messages = inject(MessageService);
  private readonly clock = inject(DEMO_NOW);

  private readonly summaryRetry = new BehaviorSubject<void>(undefined);
  private readonly drugsRetry = new BehaviorSubject<void>(undefined);

  protected readonly summary = toSignal(
    this.summaryRetry.pipe(
      switchMap(() => this.dashboardService.getSummary().pipe(toViewState<AdministratorDashboardResponse>())),
    ),
    { initialValue: LOADING as ViewState<AdministratorDashboardResponse> },
  );

  // Los periodos salen de los totales reales: se piden cuando llega el resumen.
  protected readonly periods = toSignal(
    toObservable(this.summary).pipe(
      switchMap((state) =>
        state.status === 'ready'
          ? this.periodsService.getPeriodsFor(state.data).pipe(toViewState<DashboardPeriod[]>())
          : of<ViewState<DashboardPeriod[]>>(state),
      ),
    ),
    { initialValue: LOADING as ViewState<DashboardPeriod[]> },
  );

  protected readonly drugs = toSignal(
    this.drugsRetry.pipe(switchMap(() => this.drugService.getAll().pipe(toViewState<Drug[]>()))),
    { initialValue: LOADING as ViewState<Drug[]> },
  );

  // El resumen real y los periodos demo van juntos: cargan, fallan y reintentan como una sola fuente.
  protected readonly loading = computed(
    () => this.summary().status === 'loading' || this.periods().status === 'loading',
  );
  protected readonly failed = computed(
    () => this.summary().status === 'error' || this.periods().status === 'error',
  );
  protected readonly summarySkeleton = skeletonVisible(this.loading);
  protected readonly drugsSkeleton = skeletonVisible(computed(() => this.drugs().status === 'loading'));

  private readonly queryParams = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  protected readonly periodId = computed(() => parsePeriodParam(this.queryParams().get('periodo')));
  // Datos reales y periodo elegido, cuando ambos están listos.
  protected readonly view = computed(() => {
    const summary = this.summary();
    const periods = this.periods();
    if (summary.status !== 'ready' || periods.status !== 'ready') {
      return null;
    }
    const period = periods.data.find((p) => p.id === this.periodId()) ?? periods.data[0];
    return { data: summary.data, period };
  });
  // Ventas del mes en curso para la barra lateral: siempre el periodo «Mes», sea cual sea el elegido.
  protected readonly monthPeriod = computed(() => {
    const state = this.periods();
    return state.status === 'ready' ? (state.data.find((p) => p.id === 'month') ?? null) : null;
  });
  // Solo «Mes», «Trimestre» y «Año» son fracciones demo de los totales; «Todo» son los totales reales.
  protected readonly periodIsDemo = computed(() => this.periodId() !== 'all');

  protected readonly periodOptions = PERIOD_OPTIONS;
  protected readonly skeletonRows = [0, 1, 2, 3];
  protected readonly stockMinimum = STOCK_MINIMUM;
  protected readonly sidebarMonths = monthLabels(this.clock());

  protected readonly medicationsExpanded = signal(false);

  protected readonly inventory = computed(() => {
    const state = this.drugs();
    return state.status === 'ready' ? inventoryAlerts(state.data, INVENTORY_ROWS) : null;
  });

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatUnits = formatUnits;
  protected readonly compactCurrency = compactCurrency;
  protected readonly drugStatus = drugStatus;
  protected readonly hueOf = identityHue;
  protected readonly share = share;
  protected readonly trendOf = trendOf;

  protected setPeriod(id: DashboardPeriodId): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: periodQueryParams(id), replaceUrl: true });
  }

  protected retrySummary(): void {
    this.summaryRetry.next();
  }

  protected retryDrugs(): void {
    this.drugsRetry.next();
  }

  // «Exportar» es demo (S3): avisa en vez de generar un archivo.
  protected exportDemo(): void {
    this.messages.clear();
    this.messages.add({
      severity: 'info',
      summary: DEMO_SUMMARY,
      detail: 'Exportar el informe llegará con la versión final.',
      life: 4000,
    });
  }

  protected rangeLabel(summary: AdministratorDashboardResponse): string {
    return formatPeriodRange(summary);
  }

  // Tratamientos por medicamento del último mes: de más a menos, con las barras sobre el mayor.
  protected medications(summary: AdministratorDashboardResponse) {
    const sorted = [...summary.treatmentsByMedicationLastMonth].sort(
      (a, b) => b.treatmentCount - a.treatmentCount || a.medicationName.localeCompare(b.medicationName, 'es'),
    );
    const widths = relativeWidths(sorted.map((m) => m.treatmentCount));
    const rows = sorted.map((medication, i) => ({ medication, width: widths[i] }));
    return limitList(rows, TOP_TREATMENTS, this.medicationsExpanded());
  }

  protected topMedications(summary: AdministratorDashboardResponse) {
    const top = summary.topThreeMedicationsByUnitsSold;
    const widths = relativeWidths(top.map((m) => m.unitsSold));
    return top.map((medication, i) => ({ medication, width: widths[i] }));
  }

  protected inactivePets(summary: AdministratorDashboardResponse): number {
    return Math.max(0, summary.totalPets - summary.activePets);
  }

  protected totalVeterinarians(summary: AdministratorDashboardResponse): number {
    return summary.activeVeterinarians + summary.inactiveVeterinarians;
  }

  // Barras de ocupación: la más alta destacada.
  protected occupancyBars(period: DashboardPeriod) {
    const { labels, occupancy } = period.series;
    const peak = Math.max(...occupancy);
    return labels.map((label, i) => ({ label, value: occupancy[i], peak: occupancy[i] === peak }));
  }
}
