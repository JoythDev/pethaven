import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  distinctUntilChanged,
  exhaustMap,
  finalize,
  forkJoin,
  map,
  of,
  switchMap,
} from 'rxjs';
import { Drug } from '../../../../models/drug.model';
import { PET_WEIGHT_MAX, PET_WEIGHT_MIN, Pet } from '../../../../models/pet.model';
import { EVOLUTION_LABELS, Evolution, Treatment, TreatmentRequest } from '../../../../models/treatment.model';
import { Veterinarian } from '../../../../models/veterinarian.model';
import { AuthService } from '../../../../services/auth.service';
import { DrugService } from '../../../../services/drug.service';
import { PetService } from '../../../../services/pet.service';
import { TreatmentService } from '../../../../services/treatment.service';
import { VeterinarianService } from '../../../../services/veterinarian.service';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { PetStatusTagComponent } from '../components/pet-status-tag/pet-status-tag.component';

// Datos que necesita la pantalla y que se piden juntos al API. El veterinario es quien
// inició sesión; es null si no hay una sesión de veterinario válida.
interface TreatmentFormData {
  pet: Pet;
  drugs: Drug[];
  veterinarian: Veterinarian | null;
}

interface DrugOption {
  id: number;
  label: string;
}

const currencyFormat = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

@Component({
  selector: 'app-treatment-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputNumberModule,
    InputTextModule,
    MessageModule,
    SelectModule,
    TextareaModule,
    PetStatusTagComponent,
  ],
  templateUrl: './treatment-form.component.html',
})
export class TreatmentFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly drugService = inject(DrugService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly treatmentService = inject(TreatmentService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<ViewState<TreatmentFormData>>({ status: 'loading' });
  readonly saving = signal(false);
  readonly weightLimits = { min: PET_WEIGHT_MIN, max: PET_WEIGHT_MAX };

  // RECOVERED queda fuera: dejar estable a la mascota es el alta, no un tratamiento.
  readonly evolutionOptions = (['STABLE', 'IMPROVING', 'WORSENING'] as const).map((value) => ({
    value,
    label: EVOLUTION_LABELS[value],
  }));

  // Si la mascota llega estable es un ingreso: no hay con qué comparar, así que no se pide evolución.
  readonly isAdmission = computed(() => {
    const state = this.state();
    return state.status === 'ready' && state.data.pet.disease === null;
  });

  // Cada emisión es un envío válido del formulario que debe registrarse en el API.
  private readonly submit$ = new Subject<TreatmentRequest>();

  // Arranca con una línea de medicamento vacía; el veterinario puede agregar o quitar más.
  readonly form = this.formBuilder.group({
    weight: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(PET_WEIGHT_MIN),
      Validators.max(PET_WEIGHT_MAX),
    ]),
    diagnosis: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)]],
    evolution: this.formBuilder.control<Evolution | null>(null, Validators.required),
    notes: ['', Validators.maxLength(1000)],
    drugs: this.formBuilder.array([this.createLine()], Validators.minLength(1)),
  });

  // Medicamento elegido en cada línea (null si aún no eligió), en el orden del formulario.
  // Solo emite cuando cambia la selección, no cuando cambian las unidades.
  private readonly selectedDrugIds = toSignal(
    this.form.controls.drugs.valueChanges.pipe(
      map((lines) => lines.map((line) => line.drugId ?? null)),
      distinctUntilChanged((a, b) => a.length === b.length && a.every((id, i) => id === b[i])),
    ),
    { initialValue: [null] as (number | null)[] },
  );

  private readonly drugsById = computed(() => {
    const state = this.state();
    return new Map((state.status === 'ready' ? state.data.drugs : []).map((drug) => [drug.id, drug]));
  });

  private readonly allDrugOptions = computed<DrugOption[]>(() =>
    [...this.drugsById().values()].map((drug) => ({
      id: drug.id,
      label: `${drug.name} · ${drug.unitsAvailable} disp. · ${currencyFormat.format(drug.salePrice)}`,
    })),
  );

  // Opciones de cada línea: todos los medicamentos con stock salvo los elegidos en las otras líneas,
  // para que no se pueda repetir uno. Se recalcula solo cuando cambia alguna selección.
  readonly drugOptionsByLine = computed<DrugOption[][]>(() => {
    const selected = this.selectedDrugIds();
    const all = this.allDrugOptions();
    return selected.map((own, index) => {
      const takenByOthers = new Set(selected.filter((id, i) => i !== index && id !== null));
      return all.filter((option) => option.id === own || !takenByOthers.has(option.id));
    });
  });

  // Motivo por el que no se puede registrar el tratamiento; null si el formulario puede mostrarse.
  readonly blockedReason = computed<string | null>(() => {
    const state = this.state();
    if (state.status !== 'ready') {
      return null;
    }
    if (!state.data.pet.active) {
      return `${state.data.pet.name} está inactiva: no se le pueden registrar tratamientos.`;
    }
    if (state.data.drugs.length === 0) {
      return 'No hay medicamentos disponibles en el inventario: no se pueden registrar tratamientos.';
    }
    if (state.data.veterinarian === null) {
      return 'Inicia sesión como veterinario para registrar tratamientos.';
    }
    if (!state.data.veterinarian.active) {
      return 'Tu cuenta de veterinario está desactivada: no puedes registrar atenciones.';
    }
    return null;
  });

  ngOnInit(): void {
    // Las suscripciones se crean aquí y no en el constructor: así no hay lógica al construir
    // el componente y takeUntilDestroyed corta todo al destruirlo.

    // exhaustMap ignora los envíos que lleguen mientras uno sigue en curso, así un doble clic
    // en Guardar no registra el tratamiento (y descuenta el stock) dos veces.
    this.submit$
      .pipe(
        exhaustMap((request) => this.save(request)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((treatment) => {
        this.notifications.success(
          'Tratamiento registrado',
          `Se registró el tratamiento de ${treatment.petName} con ${treatment.drugs.length} medicamento(s).`,
        );
        this.router.navigate(['/veterinario/pets', treatment.petId]);
      });

    // Lee el :id de la URL y pide en paralelo la mascota, los medicamentos con stock y el
    // veterinario de la sesión. Un id inválido termina en error sin llamar al API. switchMap
    // descarta la consulta anterior si el id cambia antes de que responda.
    this.route.paramMap
      .pipe(
        map((params) => parseRouteId(params.get('id'))),
        switchMap((id) =>
          id === null
            ? of<ViewState<TreatmentFormData>>({ status: 'error', message: 'La mascota solicitada no existe.' })
            : forkJoin({
                pet: this.petService.getById(id),
                drugs: this.drugService.getAvailable(),
                veterinarian: this.loadSessionVeterinarian(),
              }).pipe(toViewState<TreatmentFormData>()),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => {
        this.state.set(state);
        // Parte del peso y el diagnóstico actuales; el veterinario los corrige si cambiaron.
        // En el ingreso la evolución se deshabilita para que no sea obligatoria.
        if (state.status === 'ready') {
          this.form.controls.weight.setValue(state.data.pet.weight);
          this.form.controls.diagnosis.setValue(state.data.pet.disease ?? '');
          if (this.isAdmission()) {
            this.form.controls.evolution.disable();
          } else {
            this.form.controls.evolution.enable();
          }
        }
      });

    // "Sin cambios" significa que sigue con el mismo diagnóstico: se fija el actual y se bloquea.
    this.form.controls.evolution.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((evolution) => {
        const diagnosis = this.form.controls.diagnosis;
        const state = this.state();
        if (evolution === 'STABLE' && state.status === 'ready') {
          diagnosis.setValue(state.data.pet.disease ?? '');
          diagnosis.disable();
        } else {
          diagnosis.enable();
        }
      });
  }

  agregarMedicamento(): void {
    this.form.controls.drugs.push(this.createLine());
  }

  quitarMedicamento(index: number): void {
    this.form.controls.drugs.removeAt(index);
  }

  // Unidades máximas que se pueden administrar del medicamento elegido en una línea.
  stockOf(drugId: number | null): number | null {
    return drugId === null ? null : (this.drugsById().get(drugId)?.unitsAvailable ?? null);
  }

  // Valida el formulario, arma el cuerpo que espera el API y lo envía a submit$.
  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const state = this.state();
    const { drugs, weight, diagnosis, evolution, notes } = this.form.getRawValue();
    if (state.status !== 'ready' || state.data.veterinarian === null || weight === null) {
      return;
    }

    this.submit$.next({
      petId: state.data.pet.id,
      veterinarianId: state.data.veterinarian.id,
      date: null,
      weight,
      diagnosis: diagnosis.trim(),
      evolution: this.isAdmission() ? null : evolution,
      notes: notes.trim() || null,
      drugs: drugs.flatMap((line) => (line.drugId === null ? [] : [{ drugId: line.drugId, units: line.units }])),
    });
  }

  cancelar(petId: number): void {
    this.router.navigate(['/veterinario/pets', petId]);
  }

  // Registra el tratamiento en el API. Si falla (stock agotado, mascota inactiva, etc.) avisa con
  // el mensaje del API y termina sin emitir, dejando el formulario como estaba para corregirlo.
  private save(request: TreatmentRequest) {
    this.saving.set(true);

    return this.treatmentService.create(request).pipe(
      catchError((error: unknown) => {
        this.notifications.error('No se pudo registrar el tratamiento', error);
        return EMPTY;
      }),
      finalize(() => this.saving.set(false)),
    );
  }

  // Pide al API el veterinario que inició sesión: el tratamiento siempre queda a su nombre.
  // Sin sesión de veterinario, o si el API no lo encuentra, devuelve null y el formulario
  // se bloquea con el motivo en vez de fallar toda la pantalla.
  private loadSessionVeterinarian(): Observable<Veterinarian | null> {
    const user = this.authService.user();
    if (user?.role !== 'VETERINARIAN') {
      return of(null);
    }

    return this.veterinarianService.getById(user.id).pipe(catchError(() => of(null)));
  }

  private createLine() {
    return this.formBuilder.group(
      {
        drugId: this.formBuilder.control<number | null>(null, Validators.required),
        units: this.formBuilder.control(1, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]),
      },
      { validators: (line) => this.exceedsStock(line) },
    );
  }

  // Marca la línea si pide más unidades de las que hay en inventario del medicamento elegido.
  private exceedsStock(line: AbstractControl): ValidationErrors | null {
    const { drugId, units } = line.value as { drugId: number | null; units: number };
    const available = this.stockOf(drugId);
    return available !== null && units > available ? { exceedsStock: { available } } : null;
  }
}
