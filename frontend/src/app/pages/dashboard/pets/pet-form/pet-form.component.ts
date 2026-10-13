import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
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
  filter,
  finalize,
  map,
  of,
  switchMap,
  tap,
} from 'rxjs';
import { Pet, PetRequest, Species } from '../../../../models/pet.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { getErrorMessage } from '../../../../utils/http-error.util';

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
    AsyncPipe,
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
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  readonly speciesOptions: SpeciesOption[] = [
    { label: 'Perro', value: Species.DOG },
    { label: 'Gato', value: Species.CAT },
  ];

  // Llama a OwnerService.getAll y convierte cada dueño en una opción del selector.
  // La plantilla lo consume con el pipe async, que se suscribe y se desuscribe solo.
  // Si falla, avisa al usuario y deja el selector vacío.
  readonly ownerOptions$: Observable<OwnerOption[]> = this.ownerService.getAll().pipe(
    map((owners) =>
      owners.map((owner) => ({ label: `${owner.name} · ${owner.document}`, value: owner.id })),
    ),
    catchError((error: unknown) => {
      this.notifyError('No se pudieron cargar los dueños', error);
      return of<OwnerOption[]>([]);
    }),
  );

  // Con valor, el formulario edita a esa mascota; con null, registra una nueva.
  editandoId: number | null = null;

  readonly saving = signal(false);

  // Cada emisión es un envío válido del formulario que debe guardarse en el API.
  private readonly submit$ = new Subject<PetSubmission>();

  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    species: [Species.DOG, Validators.required],
    breed: ['', [Validators.required, Validators.maxLength(60)]],
    age: [0, [Validators.required, Validators.min(0)]],
    weight: [0, [Validators.required, Validators.min(0)]],
    disease: [null as string | null, Validators.maxLength(120)],
    photoUrl: ['', [Validators.required, Validators.maxLength(500), Validators.pattern(/^https?:\/\/.+/)]],
    ownerId: [null as number | null, Validators.required],
    active: [true],
  });

  constructor() {
    // exhaustMap ignora los envíos que lleguen mientras uno sigue en curso, así un doble clic
    // en Guardar no crea la mascota dos veces. Al terminar avisa y navega a la ficha.
    this.submit$
      .pipe(
        exhaustMap(({ request, active }) => this.save(request, active)),
        takeUntilDestroyed(),
      )
      .subscribe((pet) => {
        const editando = this.editandoId !== null;
        this.messageService.add({
          severity: 'success',
          summary: editando ? 'Mascota actualizada' : 'Mascota registrada',
          detail: `${pet.name} se ${editando ? 'actualizó' : 'registró'} correctamente.`,
        });
        this.router.navigate(['/dashboard/pets', pet.id]);
      });
  }

  ngOnInit(): void {
    // Lee el :id de la URL. Si existe, llama a PetService.getById y rellena el formulario
    // con la mascota; si no existe o falla, avisa y regresa al listado.
    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        tap((id) => (this.editandoId = id === null ? null : Number(id))),
        filter((id): id is string => id !== null),
        switchMap((id) =>
          this.petService.getById(Number(id)).pipe(
            catchError((error: unknown) => {
              this.notifyError('Mascota no encontrada', error);
              this.router.navigate(['/dashboard/pets']);
              return EMPTY;
            }),
          ),
        ),
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
    this.router.navigate(['/dashboard/pets']);
  }

  // Crea o actualiza la mascota según haya o no un id en la URL. concatMap espera a que
  // el guardado termine y recién entonces sincroniza el estado activo (el API lo maneja en
  // un endpoint aparte). Si ese segundo paso falla, la mascota ya quedó guardada: avisa y
  // continúa con ella en vez de perderla. Si falla el guardado, avisa y termina sin emitir.
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
                this.notifyError('La mascota se guardó, pero no se pudo cambiar su estado', error);
                return of(pet);
              }),
            ),
      ),
      catchError((error: unknown) => {
        this.notifyError('No se pudo guardar la mascota', error);
        return EMPTY;
      }),
      finalize(() => this.saving.set(false)),
    );
  }

  private notifyError(summary: string, error: unknown): void {
    this.messageService.add({ severity: 'error', summary, detail: getErrorMessage(error) });
  }
}
