import { Injectable, inject } from '@angular/core';
import { Observable, defer, map, of } from 'rxjs';
import { generateWeightCurve } from '../../core/demo/weight-curve';
import { WeightCurve } from '../../models/demo/weight-curve.model';
import { Pet } from '../../models/pet.model';
import { DEMO_NOW, demoLatency } from './demo-latency';

// Demo (S3): el API solo guarda el peso actual. La curva de los últimos meses se genera
// a partir de ese peso real y termina en él.
@Injectable({ providedIn: 'root' })
export class DemoWeightCurveService {
  private readonly now = inject(DEMO_NOW);

  getCurve(pet: Pet): Observable<WeightCurve> {
    return defer(() => of(generateWeightCurve(pet, this.now()))).pipe(demoLatency());
  }
}
