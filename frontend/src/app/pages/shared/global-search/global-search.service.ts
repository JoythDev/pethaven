import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';

import { SearchData, SearchEntry, buildSearchEntries } from '../../../core/search/global-search';
import { AuthRole } from '../../../models/auth.model';
import { AuthService } from '../../../services/auth.service';
import { DrugService } from '../../../services/drug.service';
import { OwnerService } from '../../../services/owner.service';
import { PetService } from '../../../services/pet.service';
import { VeterinarianService } from '../../../services/veterinarian.service';

export type SearchLoadStatus = 'idle' | 'loading' | 'ready' | 'error';

// Los datos se piden al enfocar el campo y se reutilizan; pasado este tiempo se piden de nuevo
// (sin vaciar los resultados) para que una mascota recién creada también aparezca.
const MAX_AGE_MS = 60_000;

/**
 * Datos de la búsqueda global (A3) y estado del panel de búsqueda en pantallas estrechas. Reúne lo
 * que el rol ya puede pedir al API con los servicios existentes: el veterinario, mascotas, dueños y
 * medicamentos; el administrador, veterinarios, mascotas y medicamentos; el cliente, sus mascotas.
 * Cada fuente que falla se omite para no perder el resto; si todas fallan el estado es `error`.
 */
@Injectable({ providedIn: 'root' })
export class GlobalSearchService {
  private readonly auth = inject(AuthService);
  private readonly pets = inject(PetService);
  private readonly owners = inject(OwnerService);
  private readonly drugs = inject(DrugService);
  private readonly veterinarians = inject(VeterinarianService);

  private readonly entriesState = signal<SearchEntry[]>([]);
  private readonly statusState = signal<SearchLoadStatus>('idle');
  private loadedFor = '';
  private loadedAt = 0;
  private inFlight = false;

  readonly entries = this.entriesState.asReadonly();
  readonly status = this.statusState.asReadonly();
  readonly role = computed<AuthRole | null>(() => this.auth.user()?.role ?? null);

  /** Panel de búsqueda abierto en `medium` y `compact`, donde el campo no cabe en la barra. */
  readonly overlayOpen = signal(false);
  private returnFocus: HTMLElement | null = null;

  /** Pide los datos la primera vez (o si pasó un minuto, o si cambió el usuario); más veces no hace nada. */
  ensureLoaded(): void {
    const user = this.auth.user();
    if (!user || this.inFlight) {
      return;
    }
    const key = `${user.role}-${user.id}`;
    const sameUser = key === this.loadedFor;
    if (sameUser && this.statusState() === 'ready' && Date.now() - this.loadedAt < MAX_AGE_MS) {
      return;
    }
    if (!sameUser) {
      this.entriesState.set([]);
    }
    this.inFlight = true;
    // Con datos ya cargados la recarga es silenciosa: no vuelve a «Buscando…».
    if (this.entriesState().length === 0) {
      this.statusState.set('loading');
    }
    this.fetch(user.role, user.id).subscribe(({ data, failed }) => {
      this.inFlight = false;
      if (failed) {
        this.statusState.set(this.entriesState().length ? 'ready' : 'error');
        return;
      }
      this.loadedFor = key;
      this.loadedAt = Date.now();
      this.entriesState.set(buildSearchEntries(user.role, data, user.id));
      this.statusState.set('ready');
    });
  }

  openOverlay(origin: HTMLElement | null = null): void {
    this.returnFocus = origin ?? (document.activeElement as HTMLElement | null);
    this.overlayOpen.set(true);
    this.ensureLoaded();
  }

  /** Cierra el panel; con `restoreFocus` devuelve el foco a quien lo abrió. */
  closeOverlay(restoreFocus = false): void {
    this.overlayOpen.set(false);
    if (restoreFocus && this.returnFocus?.isConnected) {
      this.returnFocus.focus();
    }
    this.returnFocus = null;
  }

  private fetch(role: AuthRole, userId: number): Observable<{ data: SearchData; failed: boolean }> {
    // `null` marca la fuente que falló; el resto del rol se usa igual.
    const safe = <T>(source: Observable<T>) => source.pipe(catchError(() => of(null)));
    const done = (data: SearchData, values: unknown[]) => ({
      data,
      failed: values.length > 0 && values.every((value) => value === null),
    });

    switch (role) {
      case 'OWNER':
        return forkJoin([safe(this.pets.getByOwnerId(userId))]).pipe(
          map(([pets]) => done({ pets: pets ?? [] }, [pets])),
        );
      case 'VETERINARIAN':
        return forkJoin([safe(this.pets.getAll()), safe(this.owners.getAll()), safe(this.drugs.getAll())]).pipe(
          map(([pets, owners, drugs]) =>
            done({ pets: pets ?? [], owners: owners ?? [], drugs: drugs ?? [] }, [pets, owners, drugs]),
          ),
        );
      case 'ADMIN':
        return forkJoin([
          safe(this.veterinarians.getAll()),
          safe(this.pets.getAll()),
          safe(this.drugs.getAll()),
        ]).pipe(
          map(([veterinarians, pets, drugs]) =>
            done(
              { veterinarians: veterinarians ?? [], pets: pets ?? [], drugs: drugs ?? [] },
              [veterinarians, pets, drugs],
            ),
          ),
        );
    }
  }
}
