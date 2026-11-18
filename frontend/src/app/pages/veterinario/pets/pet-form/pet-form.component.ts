import { Component, DestroyRef, ElementRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideChevronLeft, LucideCircleCheck } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import {
  BehaviorSubject,
  EMPTY,
  Observable,
  Subject,
  catchError,
  combineLatest,
  concatMap,
  debounceTime,
  distinctUntilChanged,
  exhaustMap,
  finalize,
  map,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { applyServerErrors, getErrorMessage } from '../../../../core/http/http-error';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { identityHue } from '../../../../core/identity/identity';
import { HasUnsavedChanges } from '../../../../core/forms/unsaved-changes.guard';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { parseOwnerParam } from '../../../../core/owners/owner-form';
import { NOT_FOUND, PetLoad, formatKg, toPetLoad } from '../../../../core/pet-detail/pet-detail';
import {
  PHOTO_DEBOUNCE_MS,
  PetFormField,
  backTarget,
  fieldError,
  normalizePhotoUrl,
  previewLine,
  previewName,
  previewPhotoUrl,
} from '../../../../core/pet-form/pet-form';
import { petStatus } from '../../../../core/status/pet-status';
import { coatKitFor, speciesSticker } from '../../../../core/stickers/stickers';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { Owner } from '../../../../models/owner.model';
import { PET_AGE_MAX, PET_WEIGHT_MAX, PET_WEIGHT_MIN, Pet, PetRequest, Species } from '../../../../models/pet.model';
import { AuthService } from '../../../../services/auth.service';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { BlockComponent } from '../../../shared/block/block.component';
import { EmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../../shared/inline-error/inline-error.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../../shared/sticker/sticker.component';
import { FormFieldComponent } from '../../../shared/form-field/form-field.component';

interface SpeciesOption {
  label: string;
  value: Species;
}

interface OwnerOption {
  label: string;
  value: number;
}

// Datos que viajan del formulario al flujo de guardado.
interface PetSubmission {
  request: PetRequest;
  active: boolean;
}

type FormView = 'loading' | 'form' | 'not-found' | 'error';
type PhotoState = { url: string; status: 'loaded' | 'failed' };

// Orden de los campos en pantalla y id del elemento que recibe el foco (los ids los usa Selenium).
const FIELD_ORDER: readonly { field: PetFormField; id: string }[] = [
  { field: 'name', id: 'name' },
  { field: 'species', id: 'species' },
  { field: 'breed', id: 'breed' },
  { field: 'age', id: 'age' },
  { field: 'weight', id: 'weight' },
  { field: 'ownerId', id: 'owner' },
  { field: 'photoUrl', id: 'photoUrl' },
];

/**
 * Formulario de mascota (11.4, 11.11, 11.13). Más de seis campos: página con el formulario a la
 * izquierda y una vista previa fija a la derecha (arriba en pantallas estrechas). Valida al salir
 * del campo y al enviar, nunca por tecla; al enviar enfoca el primer campo inválido. El botón de
 * guardar siempre está activo. «Mascota activa» es un campo más del formulario (se guarda con él).
 */
@Component({
  selector: 'app-pets-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    TagModule,
    ToggleSwitchModule,
    LucideChevronLeft,
    LucideCircleCheck,
    BlockComponent,
    EmptyStateComponent,
    FormFieldComponent,
    InlineErrorComponent,
    PageHeaderComponent,
    SkeletonComponent,
    StickerComponent,
  ],
  templateUrl: './pet-form.component.html',
})
export class PetsFormComponent implements OnInit, HasUnsavedChanges {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly ownerService = inject(OwnerService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly auth = inject(AuthService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly speciesOptions: SpeciesOption[] = [
    { label: 'Perro', value: Species.DOG },
    { label: 'Gato', value: Species.CAT },
  ];

  readonly limits = { weightMin: PET_WEIGHT_MIN, weightMax: PET_WEIGHT_MAX, ageMax: PET_AGE_MAX };

  // Con valor, el formulario edita a esa mascota; con null, registra una nueva.
  private readonly initialRawId = this.route.snapshot.paramMap.get('id');
  readonly editId = signal<number | null>(this.initialRawId === null ? null : parseRouteId(this.initialRawId));

  // Mascota en edición: null al crear. Un id inválido es «no encontrada», sin pedirla al API.
  readonly petLoad = signal<PetLoad | null>(
    this.initialRawId === null ? null : this.editId() === null ? NOT_FOUND : { status: 'loading' },
  );
  private readonly petRetry = new BehaviorSubject<void>(undefined);
  readonly loadedPet = computed<Pet | null>(() => {
    const load = this.petLoad();
    return load?.status === 'ready' ? load.data : null;
  });

  // Estado de la lista de dueños para el selector: cargando, con datos o con error.
  readonly ownerOptions = signal<ViewState<Owner[]>>({ status: 'loading' });
  private readonly ownersRetry = new BehaviorSubject<void>(undefined);
  private readonly ownersSettled = signal(false);
  private ownerPanelOpen = false;

  // Dueño que llegó en `?dueno=` (desde «Registrar mascota»); solo cuenta si existe en la lista.
  private readonly ownerParam = parseOwnerParam(this.route.snapshot.queryParamMap.get('dueno'));
  private readonly preselectedOwnerId = signal<number | null>(null);

  readonly ownerSelectOptions = computed<OwnerOption[]>(() => {
    const state = this.ownerOptions();
    if (state.status !== 'ready') {
      return [];
    }
    return state.data.map((owner) => ({ label: `${owner.name} · ${owner.document}`, value: owner.id }));
  });

  readonly saving = signal(false);

  // Diagnóstico actual de la mascota en edición; el botón de alta solo aparece si tiene uno.
  readonly currentDisease = signal<string | null>(null);
  readonly discharging = signal(false);

  // Cada emisión es un envío válido del formulario que debe guardarse en el API.
  private readonly submit$ = new Subject<PetSubmission>();

  // Pattern /\S/ rechaza textos solo con espacios; /^\d+$/ exige edad entera.
  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(60)]],
    species: [Species.DOG, Validators.required],
    breed: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(60)]],
    age: [0 as number | null, [Validators.required, Validators.min(0), Validators.max(PET_AGE_MAX), Validators.pattern(/^\d+$/)]],
    weight: [null as number | null, [Validators.required, Validators.min(PET_WEIGHT_MIN), Validators.max(PET_WEIGHT_MAX)]],
    // Opcional: sin foto, la mascota se ve con el sticker de su especie (11.13).
    photoUrl: ['', [Validators.maxLength(500), Validators.pattern(/^https?:\/\/.+/)]],
    ownerId: [null as number | null, Validators.required],
    active: [true],
  });

  // Mensajes de error visibles por campo: aparecen al salir del campo o al enviar, y solo se
  // actualizan (o desaparecen) mientras la persona corrige; un campo nuevo nunca se marca al teclear.
  protected readonly shown = signal<Partial<Record<PetFormField, string>>>({});

  private readonly value = toSignal(
    this.form.valueChanges.pipe(
      map(() => this.form.getRawValue()),
      startWith(this.form.getRawValue()),
    ),
    { initialValue: this.form.getRawValue() },
  );

  // La URL se evalúa tras una pausa para que la vista previa no parpadee en cada tecla.
  private readonly debouncedPhoto = toSignal(
    this.form.controls.photoUrl.valueChanges.pipe(
      map(normalizePhotoUrl),
      debounceTime(PHOTO_DEBOUNCE_MS),
      distinctUntilChanged(),
    ),
    { initialValue: '' },
  );
  protected readonly previewUrl = computed(() => previewPhotoUrl(this.debouncedPhoto()));
  private readonly photoState = signal<PhotoState | null>(null);
  protected readonly photoLoaded = computed(() => {
    const state = this.photoState();
    return !!state && state.url === this.previewUrl() && state.status === 'loaded';
  });
  protected readonly photoFailed = computed(() => {
    const state = this.photoState();
    return !!state && state.url === this.previewUrl() && state.status === 'failed';
  });

  protected readonly view = computed<FormView>(() => {
    const load = this.petLoad();
    if (load === null) return 'form';
    if (load.status === 'not-found' || load.status === 'error') return load.status;
    if (load.status === 'loading') return 'loading';
    return this.ownerOptions().status === 'loading' && !this.ownersSettled() ? 'loading' : 'form';
  });
  protected readonly skeleton = skeletonVisible(computed(() => this.view() === 'loading'));
  protected readonly petErrorMessage = computed(() => {
    const load = this.petLoad();
    return load?.status === 'error' ? load.message : '';
  });

  // A dónde vuelven el enlace «atrás» y «Cancelar»; se lee ahora, mientras dura la navegación.
  private readonly previousUrl =
    this.router.getCurrentNavigation()?.previousNavigation?.finalUrl?.toString() ?? null;
  protected readonly back = computed(() => {
    // Sin ficha a la que volver (id desconocido), el enlace lleva a la lista.
    const target = backTarget(
      this.previousUrl,
      this.view() === 'not-found' ? null : this.editId(),
      this.preselectedOwnerId(),
    );
    return { ...target, tree: this.router.parseUrl(target.url) };
  });

  protected readonly heading = computed(() => {
    if (this.editId() === null) return 'Nueva mascota';
    const name = this.loadedPet()?.name;
    return name ? `Editar ${name}` : 'Editar mascota';
  });
  protected readonly subtitle = computed(() =>
    this.view() === 'not-found' || this.view() === 'error'
      ? null
      : this.editId() === null
      ? 'Registra a un nuevo paciente. Todos los campos son obligatorios, salvo la foto.'
      : 'Cambia los datos que haga falta y guarda. Todos los campos son obligatorios, salvo la foto.',
  );

  // Vista previa (11.13): en edición usa el tono de la mascota; al crear, un tono fijo, porque el
  // id real no se conoce hasta guardar y un tono que cambiara al guardar confundiría.
  protected readonly previewHue = computed(() => (this.editId() === null ? 'honey' : identityHue(this.editId()!)));
  protected readonly previewSticker = computed(() => speciesSticker(this.value().species));
  protected readonly previewKit = computed(() => coatKitFor(this.editId() ?? 0));
  protected readonly previewTitle = computed(() => previewName(this.value().name));
  // La edad por defecto (0) no se muestra en la vista previa hasta que la persona la confirma.
  private readonly ageConfirmed = signal(false);
  protected readonly previewDetail = computed(() => {
    const { species, breed, age, weight } = this.value();
    return previewLine({ species, breed, age: this.ageConfirmed() || age !== 0 ? age : null, weight });
  });
  protected readonly previewOwner = computed(() => {
    const ownerId = this.value().ownerId;
    if (ownerId === null) return 'Dueño sin elegir';
    const state = this.ownerOptions();
    const owner = state.status === 'ready' ? state.data.find((o) => o.id === ownerId) : undefined;
    const name = owner?.name ?? (this.loadedPet()?.ownerId === ownerId ? this.loadedPet()!.ownerName : null);
    return name ? `Dueño: ${name}` : 'Dueño sin elegir';
  });
  protected readonly previewStatus = computed(() =>
    petStatus({ active: this.value().active, disease: this.currentDisease() }),
  );
  protected readonly photoHint = computed(() =>
    this.photoFailed()
      ? 'No pudimos cargar esa imagen; revisa el enlace.'
      : 'Pega el enlace de una imagen y mira cómo queda en la vista previa.',
  );
  protected readonly previewAlt = computed(() => {
    const name = this.value().name.trim();
    return name ? `Foto de ${name}` : 'Vista previa de la foto';
  });

  ngOnInit(): void {
    // Las suscripciones se crean aquí y no en el constructor: así no hay lógica al construir
    // el componente y takeUntilDestroyed corta todo al destruirlo.

    // Carga los dueños para el selector. Si falla, el selector queda deshabilitado con el error y «Reintentar».
    this.ownersRetry
      .pipe(
        switchMap(() => this.ownerService.getAll().pipe(toViewState<Owner[]>())),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => {
        this.ownerOptions.set(state);
        if (state.status !== 'loading') {
          this.ownersSettled.set(true);
        }
        if (state.status === 'ready') {
          this.preselectOwner(state.data);
        }
      });

    // Un error o el cambio de un valor ya mostrado se re-evalúa al teclear; un error nuevo, no.
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.refreshShownErrors());

    // exhaustMap ignora los envíos que lleguen mientras uno sigue en curso, así un doble clic
    // en Guardar no crea la mascota dos veces. Al terminar avisa y navega a la ficha.
    this.submit$
      .pipe(
        exhaustMap(({ request, active }) => this.save(request, active)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((pet) => {
        const editando = this.editId() !== null;
        this.notifications.success(
          editando ? 'Mascota actualizada' : 'Mascota registrada',
          `${pet.name} se ${editando ? 'actualizó' : 'registró'} correctamente.`,
        );
        this.router.navigate(['/veterinario/mascotas', pet.id]);
      });

    // Lee el :id de la URL. Sin id es un alta. Con id, lo valida antes de llamar al API;
    // si es inválido o la mascota no existe, la página lo dice en lugar de mostrar un formulario vacío.
    combineLatest([this.route.paramMap.pipe(map((params) => params.get('id'))), this.petRetry])
      .pipe(
        switchMap(([rawId]) => {
          if (rawId === null) {
            this.editId.set(null);
            return of<PetLoad | null>(null);
          }
          const id = parseRouteId(rawId);
          this.editId.set(id);
          return id === null ? of<PetLoad | null>(NOT_FOUND) : this.petService.getById(id).pipe(toPetLoad());
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((load) => {
        this.petLoad.set(load);
        if (load?.status === 'ready') {
          this.fill(load.data);
        }
      });
  }

  // Hay cambios sin guardar mientras el formulario esté sucio; guardar lo deja limpio.
  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  retryPet(): void {
    this.petRetry.next();
  }

  retryOwners(): void {
    this.ownersRetry.next();
  }

  protected onOwnerShow(): void {
    this.ownerPanelOpen = true;
  }

  protected onOwnerHide(): void {
    this.ownerPanelOpen = false;
    this.validateField('ownerId');
  }

  // Salir del selector con el panel cerrado valida; con el panel abierto el foco pasa al filtro.
  protected onOwnerBlur(): void {
    if (!this.ownerPanelOpen) {
      this.validateField('ownerId');
    }
  }

  protected onPhotoLoad(url: string): void {
    this.photoState.set({ url, status: 'loaded' });
  }

  protected onPhotoError(url: string): void {
    this.photoState.set({ url, status: 'failed' });
  }

  // Marca el mensaje de un campo al salir de él.
  protected validateField(field: PetFormField): void {
    if (field === 'age') {
      this.ageConfirmed.set(true);
    }
    this.setShown(field, fieldError(field, this.form.controls[field].errors, this.limits));
  }

  // La mascota ya mejoró y no necesita medicamentos: primero guarda el formulario (así el alta
  // queda con el peso actual) y luego la deja estable; el alta queda en su historia clínica a
  // nombre del veterinario de la sesión. Si el guardado falla, save() avisa y no se da el alta.
  darDeAlta(): void {
    const id = this.editId();
    const user = this.auth.user();
    if (id === null) return;
    if (user?.role !== 'VETERINARIAN') {
      this.notifications.warn('Inicia sesión como veterinario para dar de alta.', '');
      return;
    }
    if (this.form.invalid) {
      this.showAllErrors();
      this.notifications.warn('Revisa los campos marcados antes de dar de alta.', '');
      return;
    }
    const submission = this.buildSubmission();
    if (!submission) return;
    this.confirmationService.confirm({
      header: 'Dar de alta',
      message: `Se guardarán los cambios del formulario (peso: ${formatKg(submission.request.weight)}) y la mascota dejará de estar en tratamiento por «${this.currentDisease()}». Quedará estable y el alta se registra en su historia clínica.`,
      acceptLabel: 'Dar de alta',
      rejectLabel: 'Cancelar',
      rejectButtonProps: { outlined: true, severity: 'secondary' },
      accept: () => {
        this.discharging.set(true);
        this.save(submission.request, submission.active)
          .pipe(
            concatMap(() => this.petService.discharge(id, user.id)),
            finalize(() => this.discharging.set(false)),
            takeUntilDestroyed(this.destroyRef),
          )
          .subscribe({
            next: (discharge) => {
              this.notifications.success('Alta registrada', `${discharge.petName} quedó estable.`);
              this.router.navigate(['/veterinario/mascotas', id]);
            },
            error: (error: unknown) => this.notifications.error('No se pudo dar de alta', error),
          });
      },
    });
  }

  // Valida el formulario, arma el cuerpo que espera el API y lo envía a submit$. El botón no se
  // deshabilita (11.4): si hay errores, los muestra y enfoca el primer campo inválido.
  guardar(): void {
    if (this.form.invalid) {
      this.showAllErrors();
      this.focusFirstInvalid();
      return;
    }

    const submission = this.buildSubmission();
    if (submission) {
      this.submit$.next(submission);
    }
  }

  private buildSubmission(): PetSubmission | null {
    const value = this.form.getRawValue();
    if (value.ownerId === null || value.weight === null || value.age === null) {
      return null;
    }
    return {
      request: {
        name: value.name.trim(),
        ownerId: value.ownerId,
        species: value.species,
        breed: value.breed.trim(),
        age: value.age,
        weight: value.weight,
        photoUrl: value.photoUrl.trim() || null,
      },
      active: value.active,
    };
  }

  // Lleva la mascota cargada al formulario sin marcarlo como modificado.
  private fill(pet: Pet): void {
    this.currentDisease.set(pet.disease);
    this.ageConfirmed.set(true);
    this.form.patchValue({
      name: pet.name,
      species: pet.species,
      breed: pet.breed,
      age: pet.age,
      weight: pet.weight,
      photoUrl: pet.photoUrl ?? '',
      ownerId: pet.ownerId,
      active: pet.active,
    });
    this.form.markAsPristine();
    this.shown.set({});
  }

  // Al crear con `?dueno=` deja a ese dueño elegido, sin marcar el formulario como modificado.
  private preselectOwner(owners: readonly Owner[]): void {
    const id = this.ownerParam;
    if (
      id === null ||
      this.editId() !== null ||
      this.preselectedOwnerId() !== null ||
      this.form.controls.ownerId.value !== null ||
      !owners.some((owner) => owner.id === id)
    ) {
      return;
    }
    this.form.controls.ownerId.setValue(id);
    this.form.markAsPristine();
    this.preselectedOwnerId.set(id);
  }

  private setShown(field: PetFormField, message: string | null): void {
    this.shown.update((current) => {
      const next = { ...current };
      if (message) {
        next[field] = message;
      } else {
        delete next[field];
      }
      return next;
    });
  }

  private refreshShownErrors(): void {
    const current = this.shown();
    for (const field of Object.keys(current) as PetFormField[]) {
      this.validateField(field);
    }
  }

  private showAllErrors(): void {
    for (const { field } of FIELD_ORDER) {
      this.validateField(field);
    }
  }

  private focusFirstInvalid(): void {
    const first = FIELD_ORDER.find(({ field }) => this.form.controls[field].invalid);
    if (first) {
      this.host.nativeElement.querySelector<HTMLElement>(`#${first.id}`)?.focus();
    }
  }

  // Crea o actualiza la mascota según haya o no un id en la URL. concatMap espera a que
  // el guardado termine y recién entonces sincroniza el estado activo (el API lo maneja en
  // un endpoint aparte). Si ese segundo paso falla, la mascota ya quedó guardada: avisa y
  // continúa con ella en vez de perderla. Si falla el guardado, termina sin emitir: un 400
  // por campo va a cada control; otros errores muestran su mensaje.
  private save(request: PetRequest, active: boolean): Observable<Pet> {
    this.saving.set(true);

    const id = this.editId();
    const saved$ = id === null ? this.petService.create(request) : this.petService.update(id, request);

    return saved$.pipe(
      // Ya está guardado: el formulario queda limpio y salir no pide confirmación.
      tap(() => this.form.markAsPristine()),
      concatMap((pet) =>
        pet.active === active
          ? of(pet)
          : this.petService.setActive(pet.id, active).pipe(
              catchError((error: unknown) => {
                this.notifications.warn(
                  'La mascota se guardó, pero no se pudo cambiar su estado',
                  getErrorMessage(error),
                );
                return of(pet);
              }),
            ),
      ),
      catchError((error: unknown) => {
        if (applyServerErrors(this.form, error)) {
          this.showAllErrors();
          this.focusFirstInvalid();
          this.notifications.warn('Revisa los campos marcados.', '');
        } else {
          this.notifications.error('No se pudo guardar la mascota', error);
        }
        return EMPTY;
      }),
      finalize(() => this.saving.set(false)),
    );
  }
}
