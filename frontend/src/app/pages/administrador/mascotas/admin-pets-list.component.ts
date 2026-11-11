import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { debounceTime, map } from 'rxjs';
import { Pet } from '../../../models/pet.model';
import { PetService } from '../../../services/pet.service';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { normalizeText } from '../../../core/text/normalize-text';
import { onPhotoError as handlePhotoError } from '../../../core/ui/photo-fallback';
import { PageBannerComponent } from '../../shared/page-banner/page-banner.component';

// Listado de solo lectura: el administrador consulta las mascotas; activarlas o desactivarlas es tarea
// del veterinario.
@Component({
  selector: 'app-admin-pets-list',
  imports: [
    PageBannerComponent,
    ReactiveFormsModule,
    TableModule,
    TagModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
  ],
  templateUrl: './admin-pets-list.component.html',
})
export class AdminPetsListComponent implements OnInit {
  private readonly petService = inject(PetService);
  private readonly destroyRef = inject(DestroyRef);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly state = signal<ViewState<Pet[]>>({ status: 'loading' });

  private readonly searchTerm = toSignal(
    this.searchControl.valueChanges.pipe(debounceTime(250), map(normalizeText)),
    { initialValue: '' },
  );

  readonly filteredPets = computed(() => {
    const state = this.state();
    const term = this.searchTerm();
    if (state.status !== 'ready') return [];
    return state.data.filter(
      (pet) =>
        !term ||
        normalizeText(pet.name).includes(term) ||
        normalizeText(pet.breed).includes(term) ||
        normalizeText(pet.ownerName).includes(term),
    );
  });

  readonly onPhotoError = handlePhotoError;

  ngOnInit(): void {
    this.petService
      .getAll()
      .pipe(toViewState(), takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => this.state.set(state));
  }
}
