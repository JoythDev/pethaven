import { Component, DestroyRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { Table, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import {
  EMPTY,
  Subject,
  catchError,
  concatMap,
  debounceTime,
  distinctUntilChanged,
  map,
  merge,
  of,
  share,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { Pet, Species } from '../../../../models/pet.model';
import { Treatment } from '../../../../models/treatment.model';
import { AuthService } from '../../../../services/auth.service';
import { PetService } from '../../../../services/pet.service';
import { VeterinarianService } from '../../../../services/veterinarian.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { normalizeText } from '../../../../core/text/normalize-text';
import { onPhotoError as handlePhotoError } from '../../../../core/ui/photo-fallback';
import { PetStatusTagComponent } from '../components/pet-status-tag/pet-status-tag.component';

interface SpeciesOption {
  label: string;
  value: Species;
}

type PatientFilter = 'all' | 'mine';

interface PatientFilterOption {
  label: string;
  value: PatientFilter;
}

@Component({
  selector: 'app-pets-list',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TableModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    SelectModule,
    PetStatusTagComponent,
  ],
  templateUrl: './pets-list.component.html',
})
export class PetsListComponent implements OnInit {
  private readonly petService = inject(PetService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly table = viewChild<Table>('dt');

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly speciesControl = new FormControl<Species | null>(null);
  readonly patientFilterControl = new FormControl<PatientFilter>('all', { nonNullable: true });

  readonly speciesOptions: SpeciesOption[] = [
    { label: 'Perro', value: Species.DOG },
    { label: 'Gato', value: Species.CAT },
  ];

  readonly patientFilterOptions: PatientFilterOption[] = [
    { label: 'Todas las mascotas', value: 'all' },
    { label: 'Mis pacientes', value: 'mine' },
  ];

  // Cada emisión pide al API la lista de mascotas otra vez (carga inicial y recargas).
  private readonly refresh$ = new Subject<void>();

  // Cada emisión es una mascota a la que se le debe invertir su estado activo.
  private readonly toggleActive$ = new Subject<Pet>();

  // Últimas mascotas cargadas. Se conservan durante una recarga para que la tabla no se vacíe.
  private readonly lastPets = signal<Pet[]>([]);

  // IDs de las mascotas que ya atendió el veterinario autenticado.
  private readonly patientIds = signal<Set<number> | null>(null);
  readonly patientFilterLoading = signal(false);
  readonly patientFilterError = signal<string | null>(null);

  // Estado de la lista. Cada refresh$ pide las mascotas otra vez y switchMap cancela la petición
  // anterior si llega otra. toViewState va dentro de switchMap para que cada petición empiece en
  // 'loading' y termine en 'ready' o 'error'. tap guarda los datos listos y vacía la lista si falla.
  // Aquí no se llama al API: eso ocurre al emitir refresh$.
  readonly pets = toSignal(
    this.refresh$.pipe(
      switchMap(() =>
        this.petService.getAll().pipe(
          toViewState<Pet[]>(),
          tap((state) => {
            if (state.status === 'ready') {
              this.lastPets.set(state.data);
            } else if (state.status === 'error') {
              this.lastPets.set([]);
            }
          }),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as ViewState<Pet[]> },
  );

  // Espera 250 ms sin teclear, normaliza el texto y descarta repeticiones antes de filtrar.
  // share() evita que el reset de página y el filtro creen cada uno su propia cadena de debounce.
  private readonly searchTerm$ = this.searchControl.valueChanges.pipe(
    debounceTime(250),
    map((term) => normalizeText(term)),
    distinctUntilChanged(),
    share(),
  );

  private readonly species$ = this.speciesControl.valueChanges;
  private readonly patientFilter$ = this.patientFilterControl.valueChanges.pipe(
    distinctUntilChanged(),
    share(),
  );

  // Último texto buscado (normalizado), última especie elegida y pacientes para filtrar las mascotas,
  // como signals para el filtro.
  readonly searchTerm = toSignal(this.searchTerm$.pipe(startWith('')), { initialValue: '' });
  readonly species = toSignal(this.species$.pipe(startWith(null)), { initialValue: null });
  readonly patientFilter = toSignal(this.patientFilter$.pipe(startWith<PatientFilter>('all')), {
    initialValue: 'all' as PatientFilter,
  });

  // Lista visible: últimas mascotas cargadas filtradas por búsqueda, especie y pacientes del veterinario.
  // No depende del estado de carga, así la tabla no se vacía mientras recarga.
  readonly filteredPets = computed<Pet[]>(() => {
    const term = this.searchTerm();
    const species = this.species();
    const patientFilter = this.patientFilter();
    const patientIds = this.patientIds();
    return this.lastPets().filter((pet) => this.matches(pet, term, species, patientFilter, patientIds));
  });

  // Ícono de respaldo si la foto no carga. Se expone con este nombre para el binding de la plantilla.
  readonly onPhotoError = handlePhotoError;

  ngOnInit(): void {
    // Llama a goToFirstPage cuando cambia cualquiera de los filtros.
    merge(this.searchTerm$, this.species$, this.patientFilter$)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.goToFirstPage());

    // Obtener las mascotas tratadas por el veterinario autenticado.
    this.patientFilter$
      .pipe(
        startWith<PatientFilter>('all'),
        switchMap((filter) => {
          if (filter === 'all') {
            this.patientFilterLoading.set(false);
            this.patientFilterError.set(null);
            return of<Set<number> | null>(null);
          }

          const user = this.authService.user();
          if (!user || user.role !== 'VETERINARIAN') {
            this.patientFilterError.set('Inicia sesión como veterinario para ver tus pacientes.');
            return of(new Set<number>());
          }

          this.patientFilterLoading.set(true);
          this.patientFilterError.set(null);
          return this.veterinarianService.getTreatmentsByVeterinarianId(user.id).pipe(
            map((treatments: Treatment[]) => new Set(treatments.map((treatment) => treatment.petId))),
            catchError((error: unknown) => {
              this.notifications.error('No se pudieron cargar tus pacientes', error);
              this.patientFilterError.set('No se pudieron cargar tus pacientes.');
              return of(new Set<number>());
            }),
            tap(() => this.patientFilterLoading.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((ids) => this.patientIds.set(ids));

    // concatMap atiende los cambios de estado de uno en uno y en el orden en que se pidieron.
    // Cuando termina con éxito recarga la lista; si falla avisa y sigue escuchando.
    this.toggleActive$
      .pipe(
        concatMap((pet) =>
          this.petService.setActive(pet.id, !pet.active).pipe(
            tap((updated) =>
              this.notifications.success(
                updated.active ? 'Mascota activada' : 'Mascota desactivada',
                `${updated.name} ahora está ${updated.active ? 'activa' : 'inactiva'}.`,
              ),
            ),
            catchError((error: unknown) => {
              this.notifications.error('No se pudo cambiar el estado de la mascota', error);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.refresh$.next());

    // Los pipelines ya están conectados; ahora sí se dispara la carga inicial.
    this.refresh$.next();
  }

  nueva(): void {
    this.router.navigate(['/veterinario/pets/add']);
  }

  ver(pet: Pet): void {
    this.router.navigate(['/veterinario/pets', pet.id]);
  }

  editar(pet: Pet): void {
    this.router.navigate(['/veterinario/pets/update', pet.id]);
  }

  // Pide confirmación y, si el usuario acepta, envía la mascota a toggleActive$.
  // El API no permite borrar mascotas: solo se activan o desactivan y conservan su historial.
  cambiarEstado(pet: Pet): void {
    const desactivar = pet.active;
    this.confirmationService.confirm({
      header: desactivar ? 'Desactivar mascota' : 'Activar mascota',
      message: desactivar
        ? `¿Seguro que quieres desactivar a ${pet.name}? Las mascotas no se eliminan: quedan inactivas y conservan su historial.`
        : `¿Seguro que quieres activar de nuevo a ${pet.name}?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: desactivar ? 'Desactivar' : 'Activar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: desactivar ? 'p-button-danger' : undefined,
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.toggleActive$.next(pet),
    });
  }

  // Aplica pacientes, especie y búsqueda sobre la misma lista.
  private matches(
    pet: Pet,
    term: string,
    species: Species | null,
    patientFilter: PatientFilter,
    patientIds: Set<number> | null,
  ): boolean {
    if (patientFilter === 'mine' && !patientIds?.has(pet.id)) {
      return false;
    }
    if (species && pet.species !== species) {
      return false;
    }
    if (!term) {
      return true;
    }
    return normalizeText(pet.name).includes(term) || normalizeText(pet.breed).includes(term);
  }

  // Lleva la tabla de PrimeNG a la primera página.
  private goToFirstPage(): void {
    const table = this.table();
    if (table) {
      table.first = 0;
    }
  }
}
