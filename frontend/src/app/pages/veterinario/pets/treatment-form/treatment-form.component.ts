import { DOCUMENT } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideMinus, LucidePlus, LucideTrash2 } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import {
  BehaviorSubject,
  EMPTY,
  Observable,
  catchError,
  combineLatest,
  exhaustMap,
  finalize,
  forkJoin,
  map,
  of,
  startWith,
  switchMap,
  Subject,
} from 'rxjs';
import { Drug } from '../../../../models/drug.model';
import { PET_WEIGHT_MAX, PET_WEIGHT_MIN, Pet, Species } from '../../../../models/pet.model';
import { EVOLUTION_LABELS, Evolution, TreatmentRequest } from '../../../../models/treatment.model';
import { Veterinarian } from '../../../../models/veterinarian.model';
import { AuthService } from '../../../../services/auth.service';
import { DrugService } from '../../../../services/drug.service';
import { PetService } from '../../../../services/pet.service';
import { TreatmentService } from '../../../../services/treatment.service';
import { VeterinarianService } from '../../../../services/veterinarian.service';
import { getErrorMessage } from '../../../../core/http/http-error';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState } from '../../../../core/http/view-state';
import { identityHue } from '../../../../core/identity/identity';
import { HasUnsavedChanges } from '../../../../core/forms/unsaved-changes.guard';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { ageLabel, formatKg } from '../../../../core/pet-detail/pet-detail';
import { petStatus } from '../../../../core/status/pet-status';
import {
  DIAGNOSIS_MAX,
  FieldKey,
  NOTES_MAX,
  buildPrescription,
  evolutionRule,
  fieldError,
  requestEvolution,
} from '../../../../core/treatment-form/treatment-form';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { AvatarComponent } from '../../../shared/avatar/avatar.component';
import { BlockComponent } from '../../../shared/block/block.component';
import { CardComponent } from '../../../shared/card/card.component';
import { DefinitionListComponent } from '../../../shared/definition-list/definition-list.component';
import { LOW_STOCK_THRESHOLD, formatCurrency } from '../../../shared/drugs/drug-stock';
import { EmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../../shared/inline-error/inline-error.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../../shared/sticker/sticker.component';
import { StockMeterComponent } from '../../../shared/stock-meter/stock-meter.component';
import { FormFieldComponent } from '../../../shared/form-field/form-field.component';

// Datos que necesita la pantalla y que se piden juntos al API. El veterinario es quien
// inició sesión; es null si no hay una sesión de veterinario válida.
interface TreatmentFormData {
  pet: Pet;
  drugs: Drug[];
  veterinarian: Veterinarian | null;
}

// La mascota se pide junto al resto, pero un 404 es «no existe» y no un fallo que se pueda reintentar.
type FormLoad = ViewState<TreatmentFormData> | { status: 'not-found' };

interface DrugOption {
  id: number;
  label: string;
}

interface BlockedState {
  heading: string;
  description: string;
  sticker: 'paw' | 'pill' | 'stethoscope';
}

type DrugLine = FormGroup<{
  drugId: FormControl<number | null>;
  units: FormControl<number | null>;
}>;

const LOADING: FormLoad = { status: 'loading' };

/**
 * Registro de un tratamiento (11.4) en dos bloques: «Consulta» (fecha, peso, evolución, diagnóstico y
 * observaciones) y «Medicamentos» (líneas con existencias en vivo, precio de venta, subtotal y total de la
 * receta, 11.10). A la derecha, una tarjeta fija con el resumen de la mascota; por debajo de 960 px queda arriba.
 * Valida al salir del campo y al enviar (el botón siempre está activo) y enfoca el primer campo inválido.
 */
@Component({
  selector: 'app-treatment-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputNumberModule,
    InputTextModule,
    SelectModule,
    SelectButtonModule,
    TagModule,
    TextareaModule,
    TooltipModule,
    LucideMinus,
    LucidePlus,
    LucideTrash2,
    AvatarComponent,
    BlockComponent,
    CardComponent,
    DefinitionListComponent,
    EmptyStateComponent,
    FormFieldComponent,
    InlineErrorComponent,
    PageHeaderComponent,
    SkeletonComponent,
    StickerComponent,
    StockMeterComponent,
  ],
  templateUrl: './treatment-form.component.html',
})
export class TreatmentFormComponent implements OnInit, HasUnsavedChanges {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly petService = inject(PetService);
  private readonly drugService = inject(DrugService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly treatmentService = inject(TreatmentService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = signal<FormLoad>(LOADING);
  protected readonly saving = signal(false);
  // Motivo del último intento fallido de registrar; se muestra junto a las acciones (11.8).
  protected readonly saveError = signal<string | null>(null);
  // Tras guardar, la navegación a la ficha no debe pedir confirmación.
  private readonly saved = signal(false);
  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));

  protected readonly weightLimits = { min: PET_WEIGHT_MIN, max: PET_WEIGHT_MAX };
  protected readonly diagnosisMax = DIAGNOSIS_MAX;
  protected readonly notesMax = NOTES_MAX;
  protected readonly stockMinimum = LOW_STOCK_THRESHOLD;
  protected readonly formatCurrency = formatCurrency;
  protected readonly formatKg = formatKg;

  // RECOVERED queda fuera: dejar estable a la mascota es el alta, no un tratamiento.
  protected readonly evolutionOptions = (['IMPROVING', 'STABLE', 'WORSENING'] as const).map((value) => ({
    value,
    label: EVOLUTION_LABELS[value],
  }));

  // Cada emisión es un envío válido del formulario que debe registrarse en el API.
  private readonly submit$ = new Subject<TreatmentRequest>();
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  // Arranca con una línea de medicamento vacía; el veterinario puede agregar o quitar más.
  protected readonly form = this.formBuilder.group({
    weight: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(PET_WEIGHT_MIN),
      Validators.max(PET_WEIGHT_MAX),
    ]),
    diagnosis: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(DIAGNOSIS_MAX)]],
    evolution: this.formBuilder.control<Evolution | null>(null, Validators.required),
    notes: ['', Validators.maxLength(NOTES_MAX)],
    drugs: this.formBuilder.array([this.createLine()], Validators.minLength(1)),
  });

  // Valores de las líneas; emite al agregar, quitar o cambiar cualquier medicamento o unidad.
  private readonly lineValues = toSignal(
    this.form.controls.drugs.valueChanges.pipe(map(() => this.form.controls.drugs.getRawValue())),
    { initialValue: this.form.controls.drugs.getRawValue() },
  );

  private readonly drugsById = computed(() => {
    const state = this.state();
    return new Map((state.status === 'ready' ? state.data.drugs : []).map((drug) => [drug.id, drug]));
  });

  // Medicamento elegido en cada línea (null si aún no eligió). Solo cambia cuando cambia la selección.
  private readonly selectedDrugIds = computed(() => this.lineValues().map((line) => line.drugId ?? null), {
    equal: (a, b) => a.length === b.length && a.every((id, i) => id === b[i]),
  });

  private readonly allDrugOptions = computed<DrugOption[]>(() =>
    [...this.drugsById().values()].map((drug) => ({
      id: drug.id,
      label: `${drug.name} · ${drug.unitsAvailable} disp. · ${formatCurrency(drug.salePrice)}`,
    })),
  );

  // Opciones de cada línea: todos los medicamentos con stock salvo los elegidos en las otras líneas,
  // para que no se pueda repetir uno. Se recalcula solo cuando cambia alguna selección.
  protected readonly drugOptionsByLine = computed<DrugOption[][]>(() => {
    const selected = this.selectedDrugIds();
    const all = this.allDrugOptions();
    return selected.map((own, index) => {
      const takenByOthers = new Set(selected.filter((id, i) => i !== index && id !== null));
      return all.filter((option) => option.id === own || !takenByOthers.has(option.id));
    });
  });

  // Subtotales, existencias restantes y total de la receta, con el precio de venta (nunca el de compra).
  protected readonly prescription = computed(() => buildPrescription(this.lineValues(), this.drugsById()));

  // Con ingreso no se pide evolución: no hay con qué comparar.
  protected readonly isAdmission = computed(() => {
    const state = this.state();
    return state.status === 'ready' && evolutionRule(state.data.pet.disease, null).admission;
  });

  // Motivo por el que no se puede registrar el tratamiento; null si el formulario puede mostrarse.
  protected readonly blocked = computed<BlockedState | null>(() => {
    const state = this.state();
    if (state.status !== 'ready') {
      return null;
    }
    const { pet, drugs, veterinarian } = state.data;
    if (!pet.active) {
      return {
        heading: `${pet.name} está inactiva`,
        description: 'Actívala desde su ficha para registrarle tratamientos.',
        sticker: 'paw',
      };
    }
    if (drugs.length === 0) {
      return {
        heading: 'No hay medicamentos disponibles',
        description: 'Cuando el inventario tenga existencias podrás registrar tratamientos.',
        sticker: 'pill',
      };
    }
    if (veterinarian === null) {
      return {
        heading: 'Solo veterinarios registran tratamientos',
        description: 'Inicia sesión con una cuenta de veterinario para continuar.',
        sticker: 'stethoscope',
      };
    }
    if (!veterinarian.active) {
      return {
        heading: 'Tu cuenta está desactivada',
        description: 'Pide a un administrador que la active para registrar atenciones.',
        sticker: 'stethoscope',
      };
    }
    return null;
  });

  protected readonly backLink = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? `/veterinario/mascotas/${state.data.pet.id}` : '/veterinario/mascotas';
  });
  protected readonly backLabel = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? `Ficha de ${state.data.pet.name}` : 'Mascotas';
  });
  protected readonly subtitle = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? `Consulta de ${state.data.pet.name} · ${state.data.pet.ownerName}` : null;
  });

  ngOnInit(): void {
    // Las suscripciones se crean aquí y no en el constructor: así no hay lógica al construir
    // el componente y takeUntilDestroyed corta todo al destruirlo.

    // exhaustMap ignora los envíos que lleguen mientras uno sigue en curso, así un doble clic
    // en Registrar no registra el tratamiento (y descuenta el stock) dos veces.
    this.submit$
      .pipe(
        exhaustMap((request) => this.save(request)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((treatment) => {
        this.saved.set(true);
        this.notifications.success(
          'Tratamiento registrado',
          `Se registró el tratamiento de ${treatment.petName} con ${treatment.drugs.length} medicamento(s).`,
        );
        this.router.navigate(['/veterinario/mascotas', treatment.petId]);
      });

    // Lee el :id de la URL y pide en paralelo la mascota, los medicamentos con stock y el
    // veterinario de la sesión. Un id inválido es «no existe» sin llamar al API. switchMap
    // descarta la consulta anterior si el id cambia antes de que responda.
    combineLatest([this.route.paramMap.pipe(map((params) => parseRouteId(params.get('id')))), this.reload$])
      .pipe(
        switchMap(([id]) => this.load(id)),
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

    // «Estable» significa que sigue con el mismo diagnóstico: se fija el actual y se bloquea.
    this.form.controls.evolution.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((evolution) => {
        const diagnosis = this.form.controls.diagnosis;
        const state = this.state();
        const current = state.status === 'ready' ? state.data.pet.disease : null;
        if (state.status === 'ready' && evolutionRule(current, evolution).diagnosisLocked) {
          diagnosis.setValue(current ?? '');
          diagnosis.disable();
        } else {
          diagnosis.enable();
        }
      });
  }

  /** Hay cambios sin guardar mientras el formulario esté sucio y no se haya registrado (11.4). */
  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.saved();
  }

  protected agregarMedicamento(): void {
    this.form.controls.drugs.push(this.createLine());
    this.form.markAsDirty();
  }

  protected quitarMedicamento(index: number): void {
    this.form.controls.drugs.removeAt(index);
    this.form.markAsDirty();
  }

  protected retry(): void {
    this.reload$.next();
  }

  // Unidades máximas que se pueden administrar del medicamento elegido en una línea.
  protected stockOf(drugId: number | null): number | null {
    return drugId === null ? null : (this.drugsById().get(drugId)?.unitsAvailable ?? null);
  }

  // Error visible de un campo: solo después de salir de él o de intentar enviar (touched).
  protected errorOf(field: FieldKey, control: AbstractControl, extra?: ValidationErrors | null): string | null {
    return control.touched ? fieldError(field, { ...control.errors, ...extra }) : null;
  }

  protected unitsError(line: DrugLine): string | null {
    return this.errorOf('units', line.controls.units, line.errors);
  }

  // El CSS global de campos va sin capa y pisa el borde inválido de PrimeNG: se fuerza con `!` (10.2).
  protected invalidClass(error: string | null): string {
    return error ? 'border-danger!' : '';
  }

  protected describedBy(fieldId: string, error: string | null, hasHint: boolean): string | undefined {
    return error ? `${fieldId}-error` : hasHint ? `${fieldId}-hint` : undefined;
  }

  protected speciesLabel(pet: Pet): string {
    return pet.species === Species.CAT ? 'Gato' : 'Perro';
  }

  protected hueOf(pet: Pet) {
    return identityHue(pet.id);
  }

  protected statusOf(pet: Pet) {
    return petStatus(pet);
  }

  protected summaryItems(data: TreatmentFormData) {
    return [
      { term: 'Peso actual', value: formatKg(data.pet.weight) },
      { term: 'Último diagnóstico', value: data.pet.disease ?? 'Ninguno' },
      { term: 'Dueño', value: data.pet.ownerName },
      { term: 'Registra', value: data.veterinarian?.name ?? null },
    ];
  }

  protected ageLabel = ageLabel;

  // Valida el formulario, arma el cuerpo que espera el API y lo envía a submit$.
  protected guardar(): void {
    this.saveError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.focusFirstInvalid();
      return;
    }

    const state = this.state();
    const { drugs, weight, diagnosis, evolution, notes } = this.form.getRawValue();
    if (state.status !== 'ready' || state.data.veterinarian === null || weight === null) {
      return;
    }

    const rule = evolutionRule(state.data.pet.disease, evolution);
    this.submit$.next({
      petId: state.data.pet.id,
      veterinarianId: state.data.veterinarian.id,
      // Sin fecha: el API registra la consulta con la de hoy.
      date: null,
      weight,
      diagnosis: diagnosis.trim(),
      evolution: requestEvolution(rule, evolution),
      notes: notes.trim() || null,
      drugs: drugs.flatMap((line) =>
        line.drugId === null || line.units === null ? [] : [{ drugId: line.drugId, units: line.units }],
      ),
    });
  }

  protected cancelar(petId: number): void {
    this.router.navigate(['/veterinario/mascotas', petId]);
  }

  // Pide los datos de la pantalla; un 404 de la mascota se distingue de un fallo reintentable.
  private load(id: number | null): Observable<FormLoad> {
    if (id === null) {
      return of<FormLoad>({ status: 'not-found' });
    }
    return forkJoin({
      pet: this.petService.getById(id),
      drugs: this.drugService.getAvailable(),
      veterinarian: this.loadSessionVeterinarian(),
    }).pipe(
      map((data): FormLoad => ({ status: 'ready', data })),
      catchError((error: unknown) =>
        of<FormLoad>(
          error instanceof HttpErrorResponse && error.status === 404
            ? { status: 'not-found' }
            : { status: 'error', message: getErrorMessage(error) },
        ),
      ),
      startWith<FormLoad>(LOADING),
    );
  }

  // Registra el tratamiento en el API. Si falla (stock agotado, mascota inactiva, etc.) avisa con
  // el mensaje del API y termina sin emitir, dejando el formulario como estaba para corregirlo.
  private save(request: TreatmentRequest) {
    this.saving.set(true);

    return this.treatmentService.create(request).pipe(
      catchError((error: unknown) => {
        this.saveError.set(getErrorMessage(error, 'No se pudo registrar el tratamiento.'));
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

  private createLine(): DrugLine {
    return this.formBuilder.group(
      {
        drugId: this.formBuilder.control<number | null>(null, Validators.required),
        units: this.formBuilder.control<number | null>(1, [
          Validators.required,
          Validators.min(1),
          Validators.pattern(/^\d+$/),
        ]),
      },
      { validators: (line) => this.exceedsStock(line) },
    );
  }

  // Marca la línea si pide más unidades de las que hay en inventario del medicamento elegido.
  private exceedsStock(line: AbstractControl): ValidationErrors | null {
    const { drugId, units } = line.value as { drugId: number | null; units: number | null };
    const available = this.stockOf(drugId);
    return available !== null && (units ?? 0) > available ? { exceedsStock: { available } } : null;
  }

  // Lleva el foco al primer campo inválido, en el orden en que aparecen en pantalla.
  private focusFirstInvalid(): void {
    const { weight, evolution, diagnosis, notes, drugs } = this.form.controls;
    const ids: string[] = [];
    if (weight.invalid) ids.push('weight');
    if (evolution.enabled && evolution.invalid) ids.push('evolution');
    if (diagnosis.enabled && diagnosis.invalid) ids.push('diagnosis');
    if (notes.invalid) ids.push('notes');
    drugs.controls.forEach((line, index) => {
      if (line.controls.drugId.invalid) ids.push(`drug-${index}`);
      else if (line.controls.units.invalid || line.invalid) ids.push(`units-${index}`);
    });

    const host = ids[0] ? this.document.getElementById(ids[0]) : null;
    const target = host?.matches('input, textarea, button, [tabindex]')
      ? host
      : host?.querySelector<HTMLElement>('input, button, textarea, [tabindex]:not([tabindex="-1"])');
    target?.focus();
  }
}
