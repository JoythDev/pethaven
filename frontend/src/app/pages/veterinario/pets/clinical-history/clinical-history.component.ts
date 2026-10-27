import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { catchError, firstValueFrom, forkJoin, map, of, switchMap } from 'rxjs';
import { Owner } from '../../../../models/owner.model';
import { Pet } from '../../../../models/pet.model';
import { Treatment, evolutionLabel } from '../../../../models/treatment.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { TreatmentService } from '../../../../services/treatment.service';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { downloadClinicalHistoryPdf } from './clinical-history-pdf';

// Consulta con la variación de peso respecto a la anterior (null en la primera).
export type Visit = Treatment & { weightDelta: number | null };

export interface ClinicalHistory {
  pet: Pet;
  owner: Owner;
  visits: Visit[];
}

// Historia clínica de una mascota: datos, dueño y consultas en orden cronológico.
// Se arma con los datos actuales del API cada vez que se abre, y se descarga como PDF con jsPDF.
@Component({
  selector: 'app-clinical-history',
  imports: [RouterLink, ButtonModule, DatePipe, DecimalPipe],
  templateUrl: './clinical-history.component.html',
})
export class ClinicalHistoryComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly petService = inject(PetService);
  private readonly ownerService = inject(OwnerService);
  private readonly treatmentService = inject(TreatmentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<ViewState<ClinicalHistory>>({ status: 'loading' });
  readonly evolutionLabel = evolutionLabel;
  readonly generatedAt = new Date();
  readonly downloading = signal(false);

  ngOnInit(): void {
    const id = parseRouteId(this.route.snapshot.paramMap.get('id'));
    const history$ =
      id === null
        ? of<ViewState<ClinicalHistory>>({ status: 'error', message: 'La mascota solicitada no existe.' })
        : this.petService.getById(id).pipe(
            switchMap((pet) =>
              forkJoin({
                owner: this.ownerService.getById(pet.ownerId),
                treatments: this.treatmentService.getByPetId(pet.id),
              }).pipe(map(({ owner, treatments }) => ({ pet, owner, visits: toVisits(treatments) }))),
            ),
            toViewState<ClinicalHistory>(),
          );

    history$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((state) => this.state.set(state));
  }

  // Sin foto (o si no se pudo descargar) el PDF se genera igual, solo que sin imagen.
  async descargarPdf(history: ClinicalHistory): Promise<void> {
    this.downloading.set(true);
    try {
      const photo = await firstValueFrom(this.petService.getPhoto(history.pet.id).pipe(catchError(() => of(null))));
      await downloadClinicalHistoryPdf(history, photo);
    } finally {
      this.downloading.set(false);
    }
  }
}

// Ordena de la más antigua a la más reciente (fechas ISO) y calcula el cambio de peso.
const toVisits = (treatments: Treatment[]): Visit[] =>
  [...treatments]
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((t, i, sorted) => ({ ...t, weightDelta: i === 0 ? null : t.weight - sorted[i - 1].weight }));
