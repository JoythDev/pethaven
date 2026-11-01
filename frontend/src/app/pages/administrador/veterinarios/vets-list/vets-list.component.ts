import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { debounceTime, map } from 'rxjs';
import { Veterinarian } from '../../../../models/veterinarian.model';
import { VeterinarianService } from '../../../../services/veterinarian.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { normalizeText } from '../../../../core/text/normalize-text';
import { onPhotoError as handlePhotoError } from '../../../../core/ui/photo-fallback';

@Component({
  selector: 'app-vets-list',
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
  templateUrl: './vets-list.component.html',
})
export class VetsListComponent implements OnInit {
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly state = signal<ViewState<Veterinarian[]>>({ status: 'loading' });

  private readonly searchTerm = toSignal(
    this.searchControl.valueChanges.pipe(debounceTime(250), map(normalizeText)),
    { initialValue: '' },
  );

  readonly filteredVets = computed(() => {
    const state = this.state();
    const term = this.searchTerm();
    if (state.status !== 'ready') return [];
    return state.data.filter(
      (vet) =>
        !term ||
        normalizeText(vet.name).includes(term) ||
        normalizeText(vet.document).includes(term) ||
        normalizeText(vet.specialty).includes(term),
    );
  });

  readonly onPhotoError = handlePhotoError;

  ngOnInit(): void {
    this.veterinarianService
      .getAll()
      .pipe(toViewState(), takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => this.state.set(state));
  }

  nuevo(): void {
    this.router.navigate(['/administrador/veterinarios/add']);
  }

  editar(vet: Veterinarian): void {
    this.router.navigate(['/administrador/veterinarios/update', vet.id]);
  }

  // Los veterinarios no se eliminan: se desactivan (vacaciones, incapacidad) y conservan su historial.
  cambiarEstado(vet: Veterinarian): void {
    const desactivar = vet.active;
    this.confirmationService.confirm({
      header: desactivar ? 'Desactivar veterinario' : 'Activar veterinario',
      message: desactivar
        ? `¿Seguro que quieres desactivar a ${vet.name}? No podrá iniciar sesión hasta que lo actives de nuevo.`
        : `¿Seguro que quieres activar de nuevo a ${vet.name}?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: desactivar ? 'Desactivar' : 'Activar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: desactivar ? 'p-button-danger' : undefined,
      rejectButtonStyleClass: 'p-button-text',
      accept: () =>
        this.veterinarianService
          .setActive(vet.id, !desactivar)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (updated) => {
              this.state.update((s) =>
                s.status === 'ready'
                  ? { status: 'ready', data: s.data.map((v) => (v.id === updated.id ? updated : v)) }
                  : s,
              );
              this.notifications.success(
                updated.active ? 'Veterinario activado' : 'Veterinario desactivado',
                `${updated.name} ahora está ${updated.active ? 'activo' : 'inactivo'}.`,
              );
            },
            error: (error: unknown) =>
              this.notifications.error('No se pudo cambiar el estado del veterinario', error),
          }),
    });
  }
}
