import { Injectable, inject } from '@angular/core';
import { Observable, defer, of } from 'rxjs';
import { tipForDate } from '../../core/demo/daily-tip';
import { DailyTip } from '../../models/demo/daily-tip.model';
import { DEMO_NOW, demoLatency } from './demo-latency';

// Demo (S22): colección fija de consejos; el del día se elige por fecha, así que es el mismo
// durante todo el día y cambia al siguiente.
@Injectable({ providedIn: 'root' })
export class DemoDailyTipService {
  private readonly now = inject(DEMO_NOW);

  getTip(): Observable<DailyTip> {
    return defer(() => of(tipForDate(this.now()))).pipe(demoLatency());
  }
}
