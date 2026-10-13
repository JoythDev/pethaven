import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { Pet } from '../../../../models/pet.model';
import { PetService } from '../../../../services/pet.service';
import { getErrorMessage } from '../../../../utils/http-error.util';
import { PetStatusTagComponent } from '../components/pet-status-tag/pet-status-tag.component';

// Estado que pinta la plantilla: cargando, mascota encontrada o mensaje de error.
interface PetDetailState {
  loading: boolean;
  pet: Pet | null;
  error: string | null;
}

const LOADING_STATE: PetDetailState = { loading: true, pet: null, error: null };

@Component({
  selector: 'app-pets-detail',
  imports: [RouterLink, ButtonModule, TagModule, PetStatusTagComponent],
  templateUrl: './pet-detail.component.html',
})
export class PetsDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);

  // Lee el :id de la URL y llama a PetService.getById. switchMap descarta la consulta
  // anterior si el id cambia antes de que responda. startWith emite el estado "cargando"
  // al empezar cada consulta, y catchError convierte una falla (404, red caída) en un
  // estado con mensaje en vez de romper el flujo.
  readonly state = toSignal(
    this.route.paramMap.pipe(
      map((params) => Number(params.get('id'))),
      switchMap((id) =>
        this.petService.getById(id).pipe(
          map((pet): PetDetailState => ({ loading: false, pet, error: null })),
          catchError((error: unknown) =>
            of<PetDetailState>({ loading: false, pet: null, error: getErrorMessage(error) }),
          ),
          startWith(LOADING_STATE),
        ),
      ),
    ),
    { initialValue: LOADING_STATE },
  );

  editar(id: number): void {
    this.router.navigate(['/dashboard/pets/update', id]);
  }

  volver(): void {
    this.router.navigate(['/dashboard/pets']);
  }
}
