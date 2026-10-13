import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
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
  combineLatest,
  concatMap,
  debounceTime,
  distinctUntilChanged,
  finalize,
  map,
  merge,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { Pet, Species } from '../../../../models/pet.model';
import { PetService } from '../../../../services/pet.service';
import { getErrorMessage } from '../../../../utils/http-error.util';
import { PetStatusTagComponent } from '../components/pet-status-tag/pet-status-tag.component';

interface SpeciesOption {
  label: string;
  value: Species;
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
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  private readonly table = viewChild<Table>('dt');

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly speciesControl = new FormControl<Species | null>(null);
  readonly loading = signal(false);

  readonly speciesOptions: SpeciesOption[] = [
    { label: 'Perro', value: Species.DOG },
    { label: 'Gato', value: Species.CAT },
  ];

  // Cada emisión pide al API la lista de mascotas otra vez (carga inicial y recargas).
  private readonly reload$ = new Subject<void>();

  // Cada emisión es una mascota a la que se le debe invertir su estado activo.
  private readonly toggleActive$ = new Subject<Pet>();

  // Llama a PetService.getAll cada vez que reload$ emite. Si llega otra recarga mientras
  // la anterior sigue en curso, switchMap cancela la petición vieja. Ante un error avisa
  // al usuario y emite una lista vacía para que el flujo siga vivo.
  private readonly pets$ = this.reload$.pipe(
    switchMap(() => {
      this.loading.set(true);
      return this.petService.getAll().pipe(
        catchError((error: unknown) => {
          this.notifyError('No se pudieron cargar las mascotas', error);
          return of<Pet[]>([]);
        }),
        finalize(() => this.loading.set(false)),
      );
    }),
  );

  // Espera 250 ms sin teclear, normaliza el texto y descarta repeticiones antes de filtrar.
  private readonly searchTerm$ = this.searchControl.valueChanges.pipe(
    debounceTime(250),
    map((term) => this.normalize(term)),
    distinctUntilChanged(),
  );

  private readonly species$ = this.speciesControl.valueChanges;

  // Combina la última lista del API con el último texto y la última especie elegidos,
  // y expone el resultado filtrado como signal para la tabla.
  readonly filteredPets = toSignal(
    combineLatest([
      this.pets$,
      this.searchTerm$.pipe(startWith('')),
      this.species$.pipe(startWith(null)),
    ]).pipe(map(([pets, term, species]) => pets.filter((pet) => this.matches(pet, term, species)))),
    { initialValue: [] as Pet[] },
  );

  constructor() {
    // Llama a goToFirstPage cuando cambia la búsqueda o la especie para volver a la página 1.
    merge(this.searchTerm$, this.species$)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.goToFirstPage());

    // concatMap atiende los cambios de estado de uno en uno y en el orden en que se pidieron.
    // Cuando termina con éxito recarga la lista; si falla avisa y sigue escuchando.
    this.toggleActive$
      .pipe(
        concatMap((pet) =>
          this.petService.setActive(pet.id, !pet.active).pipe(
            tap((updated) =>
              this.messageService.add({
                severity: 'success',
                summary: updated.active ? 'Mascota activada' : 'Mascota desactivada',
                detail: `${updated.name} ahora está ${updated.active ? 'activa' : 'inactiva'}.`,
              }),
            ),
            catchError((error: unknown) => {
              this.notifyError('No se pudo cambiar el estado de la mascota', error);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.reload$.next());
  }

  ngOnInit(): void {
    // Dispara la carga inicial: reload$ emite y pets$ llama al API.
    this.reload$.next();
  }

  nueva(): void {
    this.router.navigate(['/dashboard/pets/add']);
  }

  ver(pet: Pet): void {
    this.router.navigate(['/dashboard/pets', pet.id]);
  }

  editar(pet: Pet): void {
    this.router.navigate(['/dashboard/pets/update', pet.id]);
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

  onPhotoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (!img.src.endsWith('paw-print-icon.svg')) {
      img.src = '/images/icons/paw-print-icon.svg';
    }
  }

  // Indica si la mascota cumple el filtro de especie y contiene el texto en su nombre o raza.
  private matches(pet: Pet, term: string, species: Species | null): boolean {
    if (species && pet.species !== species) {
      return false;
    }
    if (!term) {
      return true;
    }
    return this.normalize(pet.name).includes(term) || this.normalize(pet.breed).includes(term);
  }

  // Lleva la tabla de PrimeNG a la primera página.
  private goToFirstPage(): void {
    const table = this.table();
    if (table) {
      table.first = 0;
    }
  }

  private notifyError(summary: string, error: unknown): void {
    this.messageService.add({ severity: 'error', summary, detail: getErrorMessage(error) });
  }

  // Pasa a minúsculas y quita tildes para que la búsqueda no distinga acentos.
  private normalize(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '');
  }
}
