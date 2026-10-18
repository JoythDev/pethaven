import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';
import { map, of, switchMap } from 'rxjs';
import { Drug } from '../../../../models/drug.model';
import { Pet } from '../../../../models/pet.model';
import { Treatment } from '../../../../models/treatment.model';
import { AuthService } from '../../../../services/auth.service';
import { DrugService } from '../../../../services/drug.service';
import { PetService } from '../../../../services/pet.service';
import { TreatmentService } from '../../../../services/treatment.service';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { PetStatusTagComponent } from '../components/pet-status-tag/pet-status-tag.component';
import { TreatmentHistoryComponent } from '../components/treatment-history/treatment-history.component';

@Component({
  selector: 'app-pets-detail',
  imports: [RouterLink, ButtonModule, MessageModule, TagModule, PetStatusTagComponent, TreatmentHistoryComponent],
  templateUrl: './pet-detail.component.html',
})
export class PetsDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly treatmentService = inject(TreatmentService);
  private readonly drugService = inject(DrugService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<ViewState<Pet>>({ status: 'loading' });
  readonly treatmentsState = signal<ViewState<Treatment[]>>({ status: 'loading' });
  readonly drugsState = signal<ViewState<Drug[]>>({ status: 'loading' });

  // Indica si se puede registrar un tratamiento y, si no, el motivo que se muestra al veterinario.
  // Mientras algo carga no se habilita el botón pero tampoco se muestra ningún motivo.
  readonly treatmentAvailability = computed<{ allowed: boolean; reason: string | null }>(() => {
    const pet = this.state();
    const drugs = this.drugsState();

    if (this.auth.user()?.role !== 'VETERINARIAN') {
      return { allowed: false, reason: 'Inicia sesión como veterinario para registrar tratamientos.' };
    }
    if (pet.status === 'ready' && !pet.data.active) {
      return { allowed: false, reason: 'La mascota está inactiva: no se le pueden registrar tratamientos.' };
    }
    if (drugs.status === 'loading' || pet.status !== 'ready') {
      return { allowed: false, reason: null };
    }
    if (drugs.status === 'error') {
      return { allowed: false, reason: 'No se pudo verificar el inventario de medicamentos.' };
    }
    if (drugs.data.length === 0) {
      return {
        allowed: false,
        reason: 'No hay medicamentos disponibles en el inventario: no se pueden registrar tratamientos.',
      };
    }
    return { allowed: true, reason: null };
  });

  // Lee el :id de la URL y llama a PetService.getById. Un id inválido termina en error sin
  // llamar al API. switchMap descarta la consulta anterior si el id cambia antes de que
  // responda, y toViewState emite "cargando" al empezar cada consulta, luego "listo" o "error".
  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        map((params) => parseRouteId(params.get('id'))),
        switchMap((id) =>
          id === null
            ? of<ViewState<Pet>>({ status: 'error', message: 'La mascota solicitada no existe.' })
            : this.petService.getById(id).pipe(toViewState<Pet>()),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => this.state.set(state));

    // Carga el historial de tratamientos de la mascota consultada.
    this.route.paramMap
      .pipe(
        map((params) => parseRouteId(params.get('id'))),
        switchMap((id) =>
          id === null
            ? of<ViewState<Treatment[]>>({ status: 'error', message: 'La mascota solicitada no existe.' })
            : this.treatmentService.getByPetId(id).pipe(toViewState<Treatment[]>()),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => this.treatmentsState.set(state));

    // Consulta los medicamentos con stock para saber si se puede registrar un tratamiento.
    this.drugService
      .getAvailable()
      .pipe(toViewState<Drug[]>(), takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => this.drugsState.set(state));
  }

  registrarTratamiento(id: number): void {
    this.router.navigate(['/veterinario/pets', id, 'treatments', 'add']);
  }

  editar(id: number): void {
    this.router.navigate(['/veterinario/pets/update', id]);
  }

  volver(): void {
    this.router.navigate(['/veterinario/pets']);
  }
}
