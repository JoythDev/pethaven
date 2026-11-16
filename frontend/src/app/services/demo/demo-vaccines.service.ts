import { Injectable, inject } from '@angular/core';
import { Observable, defer, of } from 'rxjs';
import { generateVaccineCard } from '../../core/demo/vaccines';
import { VaccineCard } from '../../models/demo/vaccine.model';
import { Pet } from '../../models/pet.model';
import { DEMO_NOW, demoLatency } from './demo-latency';

// Demo (S3): el API no guarda vacunas. El carné se genera según la especie de la mascota real
// y las fechas se derivan de su id y de la fecha de hoy.
@Injectable({ providedIn: 'root' })
export class DemoVaccinesService {
  private readonly now = inject(DEMO_NOW);

  getCard(pet: Pet): Observable<VaccineCard> {
    return defer(() => of(generateVaccineCard(pet, this.now()))).pipe(demoLatency());
  }
}
