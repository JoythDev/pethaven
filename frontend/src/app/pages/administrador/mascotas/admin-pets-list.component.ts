import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { debounceTime, map } from 'rxjs';
import { Pet } from '../../../models/pet.model';
import { PetService } from '../../../services/pet.service';
import { NotificationService } from '../../../core/notifications/notification.service';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { normalizeText } from '../../../core/text/normalize-text';
import { onPhotoError as handlePhotoError } from '../../../core/ui/photo-fallback';

@Component({
  selector: 'app-admin-pets-list',
  imports: [
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
  ],
  templateUrl: './admin-pets-list.component.html',
})
export class AdminPetsListComponent implements OnInit {
  private readonly petService = inject(PetService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
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

  // Las mascotas no se eliminan: se desactivan y conservan su historial.
  cambiarEstado(pet: Pet): void {
    const desactivar = pet.active;
    this.confirmationService.confirm({
      header: desactivar ? 'Desactivar mascota' : 'Activar mascota',
      message: desactivar
        ? `¿Seguro que quieres desactivar a ${pet.name}? Queda inactiva y conserva su historial.`
        : `¿Seguro que quieres activar de nuevo a ${pet.name}?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: desactivar ? 'Desactivar' : 'Activar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: desactivar ? 'p-button-danger' : undefined,
      rejectButtonStyleClass: 'p-button-text',
      accept: () =>
        this.petService
          .setActive(pet.id, !desactivar)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (updated) => {
              this.state.update((s) =>
                s.status === 'ready'
                  ? { status: 'ready', data: s.data.map((p) => (p.id === updated.id ? updated : p)) }
                  : s,
              );
              this.notifications.success(
                updated.active ? 'Mascota activada' : 'Mascota desactivada',
                `${updated.name} ahora está ${updated.active ? 'activa' : 'inactiva'}.`,
              );
            },
            error: (error: unknown) =>
              this.notifications.error('No se pudo cambiar el estado de la mascota', error),
          }),
    });
  }
}
