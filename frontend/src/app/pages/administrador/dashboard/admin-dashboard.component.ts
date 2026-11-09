import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { toViewState, ViewState } from '../../../core/http/view-state';
import { greetingFor } from '../../../core/ui/greeting';
import {
  AdministratorDashboardResponse,
  TopMedicationByUnitsSold,
} from '../../../models/administrator-dashboard.model';
import { AuthService } from '../../../services/auth.service';
import { AdministratorDashboardService } from '../../../services/administrator-dashboard.service';
import { PageBannerComponent } from '../../shared/page-banner/page-banner.component';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';

const currencyFormatter = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

const countFormatter = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 });
const periodStartFormatter = new Intl.DateTimeFormat('es-CO', {
  day: 'numeric',
  month: 'short',
  timeZone: 'UTC',
});
const periodEndFormatter = new Intl.DateTimeFormat('es-CO', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
  timeZone: 'UTC',
});

@Component({
  selector: 'app-admin-dashboard',
  imports: [PageBannerComponent, StatCardComponent, ButtonModule, TableModule],
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent implements OnInit {
  private readonly dashboardService = inject(AdministratorDashboardService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<ViewState<AdministratorDashboardResponse>>({ status: 'loading' });

  // Saludo con el nombre del administrador que inició sesión; se calcula en cada ciclo de detección.
  greeting(): string {
    return greetingFor(this.auth.user()?.name);
  }

  ngOnInit(): void {
    this.loadSummary();
  }

  retry(): void {
    this.loadSummary();
  }

  formatCount(value: number): string {
    return countFormatter.format(value);
  }

  formatCurrency(value: number): string {
    return currencyFormatter.format(value);
  }

  // Color de la fila del ranking: honey, sage y rosa para el primer, segundo y tercer puesto.
  rankRowClass(index: number): string {
    return ['bg-honey/45', 'bg-sage/45', 'bg-secondary/70'][index] ?? 'bg-background';
  }

  // Relleno de la barra del ranking: un degradado con corte duro, proporcional al medicamento con más unidades.
  barFill(units: number, ranking: TopMedicationByUnitsSold[]): string {
    const max = Math.max(...ranking.map((medication) => medication.unitsSold), 0);
    const width = max > 0 ? Math.max((units / max) * 100, 4) : 0;
    return `linear-gradient(to right, rgb(38 26 16 / 0.7) ${width}%, transparent ${width}%)`;
  }

  formatPeriod(summary: AdministratorDashboardResponse): string {
    const start = parseApiDate(summary.monthlyPeriodStart);
    const endExclusive = parseApiDate(summary.monthlyPeriodEndExclusive);

    if (!start || !endExclusive) {
      return `${summary.monthlyPeriodStart} – ${summary.monthlyPeriodEndExclusive} · ${summary.monthlyPeriodTimeZone}`;
    }

    endExclusive.setUTCDate(endExclusive.getUTCDate() - 1);
    return `${periodStartFormatter.format(start)} – ${periodEndFormatter.format(endExclusive)} · ${summary.monthlyPeriodTimeZone}`;
  }

  private loadSummary(): void {
    this.state.set({ status: 'loading' });
    this.dashboardService
      .getSummary()
      .pipe(toViewState(), takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => this.state.set(state));
  }
}

function parseApiDate(value: string): Date | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return null;

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(Date.UTC(year, month - 1, day));

  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    return null;
  }

  return date;
}
