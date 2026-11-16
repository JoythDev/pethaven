import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { stockAlerts, stockStatuses } from '../../core/demo/stock-minimum';
import { DrugStockInfo, StockAlerts } from '../../models/demo/stock-minimum.model';
import { DrugService } from '../drug.service';
import { demoLatency } from './demo-latency';

// Demo (S3): el API aún no guarda un mínimo de existencias por medicamento. Cada medicamento real
// recibe un mínimo estable calculado desde su id; las unidades disponibles sí son reales.
// Las pantallas que ya tienen la lista de medicamentos usan las funciones de core/demo/stock-minimum.
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
