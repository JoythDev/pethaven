import { Injectable, inject, signal } from '@angular/core';
import { Observable, defer, map, of } from 'rxjs';
import { carePlanProgress, generateCarePlan, latestActiveTreatment } from '../../core/demo/care-plan';
import { CareItem, CareProgress, CareTreatment } from '../../models/demo/care-plan.model';
import { Pet } from '../../models/pet.model';
import { ClienteTreatmentService } from '../treatment-cliente.service';
import { DEMO_NOW, demoLatency } from './demo-latency';

// Demo (S3): el plan de cuidado diario no existe en el API. Se arma con el último tratamiento
// real de la mascota; lo que se marca como hecho vive solo en memoria y solo vale para ese día.
@Injectable({ providedIn: 'root' })
export class DemoCarePlanService {
  private readonly treatmentService = inject(ClienteTreatmentService);
  private readonly now = inject(DEMO_NOW);

  private readonly doneState = signal<ReadonlySet<string>>(new Set());
  // Ids de los cuidados marcados como hechos en esta sesión.
  readonly doneIds = this.doneState.asReadonly();

  getPlan(pet: Pet): Observable<CareItem[]> {
    return defer(() => {
      const needsTreatment = pet.active && Boolean(pet.disease?.trim());
      const treatment$ = needsTreatment
        ? this.treatmentService.getByPetId(pet.id).pipe(map((list) => latestActiveTreatment(list)))
        : of(null);
      return treatment$.pipe(
        map((treatment) => generateCarePlan(pet, treatment, this.now())),
        demoLatency(),
      );
    });
  }

  // Plan de una mascota cuyos tratamientos ya se conocen, sin pedir nada ni simular espera.
  planFrom(pet: Pet, treatments: CareTreatment[]): CareItem[] {
    return generateCarePlan(pet, latestActiveTreatment(treatments), this.now());
  }

  isDone(itemId: string): boolean {
    return this.doneState().has(itemId);
  }

  setDone(itemId: string, done: boolean): void {
    this.doneState.update((ids) => {
      const next = new Set(ids);
      if (done) {
        next.add(itemId);
      } else {
        next.delete(itemId);
      }
      return next;
    });
  }

  toggle(itemId: string): void {
    this.setDone(itemId, !this.isDone(itemId));
  }

  progress(items: CareItem[]): CareProgress {
    return carePlanProgress(items, this.doneState());
  }
}
