import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  concatMap,
  exhaustMap,
  finalize,
  map,
  of,
  switchMap,
} from 'rxjs';
import { Owner } from '../../../../models/owner.model';
import { Pet, PetRequest, Species } from '../../../../models/pet.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { applyServerErrors, getErrorMessage } from '../../../../core/http/http-error';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { NotificationService } from '../../../../core/notifications/notification.service';

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

@Component({
  selector: 'app-pets-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    ToggleSwitchModule,
    MessageModule,
  ],
  templateUrl: './pet-form.component.html',
})
export class PetsFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly ownerService = inject(OwnerService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly speciesOptions: SpeciesOption[] = [
    { label: 'Perro', value: Species.DOG },
    { label: 'Gato', value: Species.CAT },
  ];

  // Estado de la lista de dueños para el selector: cargando, con datos o con error.
  readonly ownerOptions = signal<ViewState<Owner[]>>({ status: 'loading' });

  // Opciones del selector, derivadas de la lista de dueños ya cargada.
  readonly ownerSelectOptions = computed<OwnerOption[]>(() => {
    const state = this.ownerOptions();
    if (state.status !== 'ready') {
      return [];
    }
    return state.data.map((owner) => ({ label: `${owner.name} · ${owner.document}`, value: owner.id }));
  });

  // Con valor, el formulario edita a esa mascota; con null, registra una nueva.
  editandoId: number | null = null;

  readonly saving = signal(false);

  // Cada emisión es un envío válido del formulario que debe guardarse en el API.
  private readonly submit$ = new Subject<PetSubmission>();

  // Pattern /\S/ rechaza textos solo con espacios; /^\d+$/ exige edad entera.
  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(60)]],
    species: [Species.DOG, Validators.required],
    breed: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(60)]],
    age: [0, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]],
    weight: [0, [Validators.required, Validators.min(0)]],
    disease: [null as string | null, Validators.maxLength(120)],
    photoUrl: ['', [Validators.required, Validators.maxLength(500), Validators.pattern(/^https?:\/\/.+/)]],
    ownerId: [null as number | null, Validators.required],
    active: [true],
  });

  ngOnInit(): void {
    // Las suscripciones se crean aquí y no en el constructor: así no hay lógica al construir
    // el componente y takeUntilDestroyed corta todo al destruirlo.

    // Carga los dueños para el selector. Si falla, el selector queda deshabilitado con el error.
    this.ownerService
      .getAll()
      .pipe(toViewState<Owner[]>(), takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => this.ownerOptions.set(state));

    // exhaustMap ignora los envíos que lleguen mientras uno sigue en curso, así un doble clic
    // en Guardar no crea la mascota dos veces. Al terminar avisa y navega a la ficha.
    this.submit$
      .pipe(
        exhaustMap(({ request, active }) => this.save(request, active)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((pet) => {
        const editando = this.editandoId !== null;
        this.notifications.success(
          editando ? 'Mascota actualizada' : 'Mascota registrada',
          `${pet.name} se ${editando ? 'actualizó' : 'registró'} correctamente.`,
        );
        this.router.navigate(['/veterinario/pets', pet.id]);
      });

    // Lee el :id de la URL. Sin id es un alta. Con id, lo valida antes de llamar al API;
    // si es inválido o la mascota no existe, avisa y regresa al listado.
    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        switchMap((rawId) => {
          if (rawId === null) {
            this.editandoId = null;
            return EMPTY;
          }

          // Un id inválido se trata como no encontrado; no se envía una petición con NaN.
          const id = parseRouteId(rawId);
          if (id === null) {
            this.handleNotFound(null);
            return EMPTY;
          }

          this.editandoId = id;
          return this.petService.getById(id).pipe(
            catchError((error: unknown) => {
              this.handleNotFound(error);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((pet) =>
        this.form.patchValue({
          name: pet.name,
          species: pet.species,
          breed: pet.breed,
          age: pet.age,
          weight: pet.weight,
          disease: pet.disease,
          photoUrl: pet.photoUrl ?? '',
          ownerId: pet.ownerId,
          active: pet.active,
        }),
      );
  }

  // Valida el formulario, arma el cuerpo que espera el API y lo envía a submit$.
  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    if (value.ownerId === null) {
      return;
    }

    this.submit$.next({
      request: {
        name: value.name.trim(),
        ownerId: value.ownerId,
        species: value.species,
        breed: value.breed.trim(),
        age: value.age,
        weight: value.weight,
        disease: value.disease?.trim() || null,
        photoUrl: value.photoUrl.trim() || null,
      },
      active: value.active,
    });
  }

  cancelar(): void {
    this.router.navigate(['/veterinario/pets']);
  }

  // Mascota inexistente o id inválido: mismo aviso y regreso al listado.
  private handleNotFound(error: unknown): void {
    this.notifications.error('Mascota no encontrada', error);
    this.router.navigate(['/veterinario/pets']);
  }

  // Crea o actualiza la mascota según haya o no un id en la URL. concatMap espera a que
  // el guardado termine y recién entonces sincroniza el estado activo (el API lo maneja en
  // un endpoint aparte). Si ese segundo paso falla, la mascota ya quedó guardada: avisa y
  // continúa con ella en vez de perderla. Si falla el guardado, termina sin emitir: un 400
  // por campo va a cada control; otros errores muestran su mensaje.
  private save(request: PetRequest, active: boolean): Observable<Pet> {
    this.saving.set(true);

    const id = this.editandoId;
    const saved$ = id === null ? this.petService.create(request) : this.petService.update(id, request);

    return saved$.pipe(
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
