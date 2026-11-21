import { Injectable, inject } from '@angular/core';
import { Observable, defer, map, of } from 'rxjs';
import { generateDashboardPeriods } from '../../core/demo/dashboard-periods';
import { DashboardPeriod, DashboardTotals } from '../../models/demo/dashboard-period.model';
import { AdministratorDashboardService } from '../administrator-dashboard.service';
import { DEMO_NOW, demoLatency } from './demo-latency';

// Demo (S32): el API solo devuelve totales históricos. En «Todo», ventas y ganancia son esos
// totales reales; los demás periodos son fracciones estables de ellos y la ocupación es demo.
@Injectable({ providedIn: 'root' })
export class DemoDashboardPeriodsService {
  private readonly dashboardService = inject(AdministratorDashboardService);
  private readonly now = inject(DEMO_NOW);

  // Pide los totales reales al API.
  getPeriods(): Observable<DashboardPeriod[]> {
    return this.dashboardService.getSummary().pipe(
      map((summary) => generateDashboardPeriods(summary, this.now())),
      demoLatency(),
    );
  }

  // Para páginas que ya tienen la respuesta del panorama.
  getPeriodsFor(totals: DashboardTotals): Observable<DashboardPeriod[]> {
    return defer(() => of(generateDashboardPeriods(totals, this.now()))).pipe(demoLatency());
  }
}
