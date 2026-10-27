import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { toViewState, ViewState } from '../../../core/http/view-state';
import { AdministratorDashboardResponse } from '../../../models/administrator-dashboard.model';
import { AdministratorDashboardService } from '../../../services/administrator-dashboard.service';

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
  imports: [ButtonModule, TableModule],
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent implements OnInit {
  private readonly dashboardService = inject(AdministratorDashboardService);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<ViewState<AdministratorDashboardResponse>>({ status: 'loading' });

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
