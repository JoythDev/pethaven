import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { stockAlerts, stockStatuses } from '../../core/demo/stock-minimum';
import { DrugStockInfo, StockAlerts } from '../../models/demo/stock-minimum.model';
import { DrugService } from '../drug.service';
import { demoLatency } from './demo-latency';

// Existencias con la regla real (L61): 0 agotado, 1–5 stock bajo, 6 o más disponible. El API aún no guarda un
// mínimo configurable por medicamento (pendiente de backend); mientras tanto el mínimo es el mismo para todos
// y las unidades disponibles son reales. Las pantallas que ya tienen la lista de medicamentos usan las
// funciones de core/demo/stock-minimum.
@Injectable({ providedIn: 'root' })
export class DemoStockMinimumService {
  private readonly drugService = inject(DrugService);

  getStatuses(): Observable<DrugStockInfo[]> {
    return this.drugService.getAll().pipe(map(stockStatuses), demoLatency());
  }

  getAlerts(): Observable<StockAlerts> {
    return this.drugService.getAll().pipe(map(stockAlerts), demoLatency());
  }
}
