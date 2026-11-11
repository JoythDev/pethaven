import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { Subject, of, switchMap } from 'rxjs';
import { parseRouteId } from '../../../core/http/route-id';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { greetingFor } from '../../../core/ui/greeting';
import { onPhotoError as handlePhotoError } from '../../../core/ui/photo-fallback';
import { Pet } from '../../../models/pet.model';
import { AuthService } from '../../../services/auth.service';
import { PetService } from '../../../services/pet.service';
import { ageLabel, speciesLabel } from '../shared/cliente-labels';
import { ClientePetStatusTagComponent } from '../shared/pet-status-tag/cliente-pet-status-tag.component';
import { PageBannerComponent } from '../../shared/page-banner/page-banner.component';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';

// Conteos de la fila de métricas del listado.
interface PetMetrics {
  total: number;
  active: number;
  inactive: number;
}

@Component({
  selector: 'app-mascotas',
  imports: [PageBannerComponent, StatCardComponent, ButtonModule, SkeletonModule, TagModule, ClientePetStatusTagComponent],
  templateUrl: './mascotas.component.html',
})
export class MascotasComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  // :ownerId de la ruta padre; null mientras llega o si el id es inválido.
  private readonly ownerId = signal<number | null>(null);

  // Cada emisión vuelve a pedir las mascotas del dueño (carga inicial y recargas).
  // El primer HTTP sale de ngOnInit, no del constructor.
  private readonly refresh$ = new Subject<void>();

  // Estado del listado. Un id inválido termina en error sin llamar al API; con un id
  // válido toViewState emite "cargando" y luego "listo" o "error".
  readonly pets = toSignal(
    this.refresh$.pipe(
      switchMap(() => {
        const ownerId = this.ownerId();
        return ownerId === null
          ? of<ViewState<Pet[]>>({ status: 'error', message: 'Dueño no encontrado.' })
          : this.petService.getByOwnerId(ownerId).pipe(toViewState<Pet[]>());
      }),
    ),
    { initialValue: { status: 'loading' } as ViewState<Pet[]> },
  );

  // Conteos de la fila de métricas; null mientras el estado no esté listo.
  readonly metrics = computed<PetMetrics | null>(() => {
    const state = this.pets();
    if (state.status !== 'ready') {
      return null;
    }
    return {
      total: state.data.length,
      active: state.data.filter((pet) => pet.active).length,
      inactive: state.data.filter((pet) => !pet.active).length,
    };
  });

  // Saludo con el nombre del dueño que inició sesión; se calcula en cada ciclo de detección.
  greeting(): string {
    return greetingFor(this.auth.user()?.name);
  }

  // Tarjetas de esqueleto que se pintan mientras carga el listado.
  readonly loadingPlaceholders = [0, 1, 2];

  // Etiquetas en español del cliente, expuestas con estos nombres para la plantilla.
  readonly speciesLabel = speciesLabel;
  readonly ageLabel = ageLabel;

  // Ícono de respaldo si la foto no carga. Se expone con este nombre para el binding de la plantilla.
  readonly onPhotoError = handlePhotoError;

  ngOnInit(): void {
    // El :ownerId pertenece a la ruta padre: con la estrategia de herencia de params por
    // defecto ("emptyOnly") esta ruta hija no lo hereda, así que se lee del padre.
    // El param puede cambiar sin recrear la página (misma área, otro dueño).
    this.route.parent!.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.ownerId.set(parseRouteId(params.get('ownerId')));
      this.refresh$.next();
    });
  }

  // Color de la franja superior de cada tarjeta de mascota; rota entre cuatro tonos.
  coverClass(index: number): string {
    return ['bg-honey', 'bg-sage', 'bg-secondary', 'bg-terracotta/70'][index % 4];
  }

  // Abre el detalle de la mascota dentro del área del dueño.
  verDetalles(petId: number): void {
    const ownerId = this.ownerId();
    if (ownerId === null) {
      return;
    }
    this.router.navigate(['/cliente', ownerId, 'mascotas', petId]);
  }
}
