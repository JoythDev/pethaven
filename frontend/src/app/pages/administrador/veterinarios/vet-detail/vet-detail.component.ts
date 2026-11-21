import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideChevronLeft, LucidePencil } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { BehaviorSubject, catchError, combineLatest, forkJoin, map, of, switchMap } from 'rxjs';

import { parseRouteId } from '../../../../core/http/route-id';
import { identityHue, initials } from '../../../../core/identity/identity';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { deriveVetActivity } from '../../../../core/vets/vet-activity';
import { VET_NOT_FOUND, VetLoad, toVetLoad } from '../../../../core/vets/vet-detail';
import { vetStatusLabel } from '../../../../core/vets/vets-list';
import { Veterinarian } from '../../../../models/veterinarian.model';
import { VeterinarianService } from '../../../../services/veterinarian.service';
import { AvatarComponent } from '../../../shared/avatar/avatar.component';
import { BlockComponent } from '../../../shared/block/block.component';
import { CardComponent } from '../../../shared/card/card.component';
import { DefinitionListComponent } from '../../../shared/definition-list/definition-list.component';
import { EmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../../shared/inline-error/inline-error.component';
import { ListItemComponent } from '../../../shared/list-item/list-item.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../../shared/sticker/sticker.component';
import { VetActiveSwitchComponent } from '../vet-active-switch/vet-active-switch.component';
import { VetFormDialogComponent } from '../vet-form-dialog/vet-form-dialog.component';

const LOADING = { status: 'loading' } as const;
const LIST_URL = '/administrador/veterinarios';

/**
 * Ficha de un veterinario (11.2): cabecera en su color de identidad con su especialidad y estado, y
 * tarjetas «Datos», «Actividad» y «Pacientes recientes». La actividad se deriva de sus tratamientos.
 * Editar abre el diálogo de edición sin salir de la ficha.
 */
@Component({
  selector: 'app-vet-detail',
  imports: [
    DatePipe,
    RouterLink,
    ButtonModule,
    TagModule,
    LucideChevronLeft,
    LucidePencil,
    AvatarComponent,
    BlockComponent,
    CardComponent,
    DefinitionListComponent,
    EmptyStateComponent,
    InlineErrorComponent,
    ListItemComponent,
    SkeletonComponent,
    StickerComponent,
    VetActiveSwitchComponent,
    VetFormDialogComponent,
  ],
  templateUrl: './vet-detail.component.html',
})
export class VetDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly vetService = inject(VeterinarianService);

  protected readonly state = signal<VetLoad>(LOADING);
  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));
  protected readonly editing = signal(false);

  private readonly reload$ = new BehaviorSubject<void>(undefined);
  // Al recargar tras guardar se conservan los datos que ya se ven en vez de volver al esqueleto.
  private silentReload = false;

  protected readonly hue = computed(() => {
    const state = this.state();
    return identityHue(state.status === 'ready' ? state.data.vet.id : 0);
  });

  // Los tratamientos pueden fallar sin tumbar la ficha: la actividad queda sin dato.
  protected readonly activity = computed(() => {
    const state = this.state();
    return state.status === 'ready' && state.data.treatments
      ? deriveVetActivity(state.data.treatments, new Date())
      : null;
  });

  protected readonly listUrl = LIST_URL;
  protected readonly initials = initials;
  protected readonly statusLabel = vetStatusLabel;

  constructor() {
    // Lee el :id de la URL y pide en paralelo el veterinario y sus tratamientos. Un id inválido es
    // «no encontrado» sin llamar al API; switchMap descarta la consulta anterior si el id o la recarga cambian.
    combineLatest([this.route.paramMap.pipe(map((params) => parseRouteId(params.get('id')))), this.reload$])
      .pipe(
        switchMap(([id]) =>
          id === null
            ? of<VetLoad>(VET_NOT_FOUND)
            : forkJoin({
                vet: this.vetService.getById(id),
                treatments: this.vetService.getTreatmentsByVeterinarianId(id).pipe(catchError(() => of(null))),
              }).pipe(toVetLoad()),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((load) => {
        if (load.status === 'loading' && this.silentReload) {
          return;
        }
        if (load.status !== 'loading') {
          this.silentReload = false;
        }
        this.state.set(load);
      });
  }

  protected retry(): void {
    this.state.set(LOADING);
    this.reload$.next();
  }

  /** El diálogo o el interruptor guardó: la ficha se recarga con los datos nuevos. */
  protected onSaved(): void {
    this.silentReload = true;
    this.reload$.next();
  }

  protected patientsLabel(count: number): string {
    return count === 1 ? 'paciente atendido' : 'pacientes atendidos';
  }

  protected headline(vet: Veterinarian): string {
    return `${vet.specialty} · ${vetStatusLabel(vet.active)}`;
  }
}
