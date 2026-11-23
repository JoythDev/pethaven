import { Component, DestroyRef, Signal, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideCalendar, LucideChevronLeft, LucideDownload, LucideInfo, LucidePencil, LucidePlus } from '@lucide/angular';
import { BehaviorSubject, Observable, catchError, combineLatest, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';

import { getErrorMessage } from '../../../../core/http/http-error';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { identityHue } from '../../../../core/identity/identity';
import { belongsToArea, bookingLink } from '../../../../core/client-pets/client-pets';
import { ownerIdParam } from '../../../../core/client-pets/owner-route';
import { historyTabFromParam, historyTabToParam, resolvePetArea, resolvePetBasePath } from '../../../../core/pets-list/pet-area';
import {
  DetailTab,
  NOT_FOUND,
  PetLoad,
  TAB_PARAM,
  TREATMENT_PARAM,
  ageLabel,
  formatKg,
  parseTab,
  toPetLoad,
  toVisits,
} from '../../../../core/pet-detail/pet-detail';
import { petStatus } from '../../../../core/status/pet-status';
import { coatKitFor, speciesSticker } from '../../../../core/stickers/stickers';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { VaccineCard } from '../../../../models/demo/vaccine.model';
import { Drug } from '../../../../models/drug.model';
import { Pet, Species } from '../../../../models/pet.model';
import { Treatment } from '../../../../models/treatment.model';
import { WeightPoint } from '../../../../models/weight.model';
import { AuthService } from '../../../../services/auth.service';
import { DemoVaccinesService } from '../../../../services/demo/demo-vaccines.service';
import { DrugService } from '../../../../services/drug.service';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { TreatmentService } from '../../../../services/treatment.service';
import { BlockComponent } from '../../../shared/block/block.component';
import { DemoTagComponent } from '../../../shared/demo-tag/demo-tag.component';
import { EmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../../shared/inline-error/inline-error.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../../shared/sticker/sticker.component';
import { HistoryTabComponent } from './history-tab/history-tab.component';
import { SummaryTabComponent } from './summary-tab/summary-tab.component';
import { VaccinesTabComponent } from './vaccines-tab/vaccines-tab.component';

const LOADING = { status: 'loading' } as const;

// Pestañas (10.14): subrayado de 2,5 px en `accent` y línea base de 1 px en `line`, sobre los tokens de PrimeNG.
const TAB_TOKENS = {
  tablist: { background: 'transparent', borderColor: 'var(--color-line)' },
  tab: {
    padding: '9px 14px',
    fontWeight: '700',
    color: 'var(--color-ink-muted)',
    hoverColor: 'var(--color-ink)',
    activeColor: 'var(--color-ink)',
    activeBorderColor: 'var(--color-accent)',
  },
  activeBar: { height: '2.5px', bottom: '-1px', background: 'var(--color-accent)' },
  tabpanel: { background: 'transparent', padding: '14px 0 0' },
};

/**
 * Ficha de una mascota (11.2) para el veterinario y, en solo consulta, para el administrador y el
 * cliente (el área llega por el `data` de la ruta): cabecera en el color de identidad con las acciones
 * del rol, pestañas Resumen, Historia clínica (Tratamientos para el cliente) y Vacunas, y el estado de
 * cada pestaña en la URL (`?pestana`, `?tratamiento`). Cada fuente (mascota, tratamientos, inventario,
 * curva y vacunas demo) carga y falla por separado. El inventario solo se pide si el área gestiona
 * (el dueño no puede leerlo). En el área del cliente una mascota de otro dueño se trata como inexistente.
 * «Descargar PDF» genera la historia clínica completa.
 */
@Component({
  selector: 'app-pets-detail',
  imports: [
    RouterLink,
    ButtonModule,
    TabsModule,
    TagModule,
    LucideCalendar,
    LucideChevronLeft,
    LucideDownload,
    LucideInfo,
    LucidePencil,
    LucidePlus,
    BlockComponent,
    DemoTagComponent,
    EmptyStateComponent,
    HistoryTabComponent,
    InlineErrorComponent,
    SkeletonComponent,
    StickerComponent,
    SummaryTabComponent,
    VaccinesTabComponent,
  ],
  templateUrl: './pet-detail.component.html',
})
export class PetsDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly ownerService = inject(OwnerService);
  private readonly treatmentService = inject(TreatmentService);
  private readonly drugService = inject(DrugService);
  private readonly vaccinesService = inject(DemoVaccinesService);
  private readonly auth = inject(AuthService);
  private readonly messages = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly area = resolvePetArea(this.route.snapshot.data);
  protected readonly tabTokens = TAB_TOKENS;
  protected readonly formatKg = formatKg;
  protected readonly ageLabel = ageLabel;

  private readonly petId = toSignal(this.route.paramMap.pipe(map((params) => parseRouteId(params.get('id')))), {
    initialValue: parseRouteId(this.route.snapshot.paramMap.get('id')),
  });
  // :ownerId del portal del cliente (una ruta ancestra); null fuera de él o si es inválido.
  private readonly ownerId = toSignal(ownerIdParam(this.route), { initialValue: null });
  protected readonly basePath = computed(() => resolvePetBasePath(this.area, this.ownerId()));
  protected readonly scheduleLink = computed(() => bookingLink(this.ownerId()));
  private readonly queryParams = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });
  protected readonly tab = computed(() => parseTab(historyTabFromParam(this.area, this.queryParams().get(TAB_PARAM))));

  private readonly petRetry = new BehaviorSubject<void>(undefined);
  private readonly treatmentsRetry = new BehaviorSubject<void>(undefined);
  private readonly weightRetry = new BehaviorSubject<void>(undefined);
  private readonly vaccinesRetry = new BehaviorSubject<void>(undefined);

  private readonly petLoad = toSignal(
    combineLatest([toObservable(this.petId), this.petRetry]).pipe(
      switchMap(([id]) => (id === null ? of<PetLoad>(NOT_FOUND) : this.petService.getById(id).pipe(toPetLoad()))),
    ),
    { initialValue: LOADING as PetLoad },
  );
  // En el área del cliente una mascota de otro dueño se trata como inexistente. Es una comprobación de interfaz, no de seguridad.
  protected readonly petState = computed<PetLoad>(() => {
    const load = this.petLoad();
    return load.status === 'ready' && !belongsToArea(this.area, load.data, this.ownerId()) ? NOT_FOUND : load;
  });
  protected readonly pet = computed<Pet | null>(() => {
    const state = this.petState();
    return state.status === 'ready' ? state.data : null;
  });

  protected readonly treatments = toSignal(
    combineLatest([toObservable(this.petId), this.treatmentsRetry]).pipe(
      switchMap(([id]) =>
        id === null
          ? of(LOADING as ViewState<Treatment[]>)
          : this.treatmentService.getByPetId(id).pipe(toViewState<Treatment[]>()),
      ),
    ),
    { initialValue: LOADING as ViewState<Treatment[]> },
  );

  // Inventario con existencias: solo para saber si se puede registrar un tratamiento. Quien no gestiona no
  // lo pide (el dueño recibiría 403 y el administrador no registra tratamientos).
  private readonly drugs: Signal<ViewState<Drug[]>> = this.area.canManage
    ? toSignal(this.drugService.getAvailable().pipe(toViewState<Drug[]>()), { initialValue: LOADING as ViewState<Drug[]> })
    : signal(LOADING as ViewState<Drug[]>);

  protected readonly weight = toSignal(
    combineLatest([toObservable(this.petId), this.weightRetry]).pipe(
      switchMap(([id]) =>
        id === null
          ? of(LOADING as ViewState<WeightPoint[]>)
          : this.petService.getWeights(id).pipe(toViewState<WeightPoint[]>()),
      ),
    ),
    { initialValue: LOADING as ViewState<WeightPoint[]> },
  );
  // Carné de vacunas (demo): se genera a partir de la mascota real.
  protected readonly vaccines = toSignal(
    combineLatest([toObservable(this.pet), this.vaccinesRetry]).pipe(
      switchMap(([pet]) => this.demoFor(pet, (p) => this.vaccinesService.getCard(p), toViewState<VaccineCard>())),
    ),
    { initialValue: LOADING as ViewState<VaccineCard> },
  );

  protected readonly petSkeleton = skeletonVisible(computed(() => this.petState().status === 'loading'));
  protected readonly downloading = signal(false);
  protected readonly photoFailedUrl = signal<string | null>(null);

  protected readonly hue = computed(() => identityHue(this.pet()?.id ?? 0));
  protected readonly status = computed(() => {
    const pet = this.pet();
    return pet ? petStatus(pet) : null;
  });
  protected readonly sticker = computed(() => speciesSticker(this.pet()?.species ?? Species.DOG));
  protected readonly kit = computed(() => coatKitFor(this.pet()?.id ?? 0));
  protected readonly speciesLabel = computed(() => (this.pet()?.species === Species.CAT ? 'Gato' : 'Perro'));

  // Indica si se puede registrar un tratamiento y, si no, el motivo que se muestra al veterinario.
  // Mientras algo carga no se habilita el botón pero tampoco se muestra ningún motivo.
  protected readonly treatmentAvailability = computed<{ allowed: boolean; reason: string | null }>(() => {
    const pet = this.petState();
    const drugs = this.drugs();

    // El administrador solo consulta: no se ofrece la acción ni se explica su ausencia.
    if (!this.area.canManage) {
      return { allowed: false, reason: null };
    }
    if (this.auth.user()?.role !== 'VETERINARIAN') {
      return { allowed: false, reason: 'Inicia sesión como veterinario para registrar tratamientos.' };
    }
    if (pet.status === 'ready' && !pet.data.active) {
      return { allowed: false, reason: 'La mascota está inactiva: no se le pueden registrar tratamientos.' };
    }
    if (drugs.status === 'loading' || pet.status !== 'ready') {
      return { allowed: false, reason: null };
    }
    if (drugs.status === 'error') {
      return { allowed: false, reason: 'No se pudo verificar el inventario de medicamentos.' };
    }
    if (drugs.data.length === 0) {
      return {
        allowed: false,
        reason: 'No hay medicamentos disponibles en el inventario: no se pueden registrar tratamientos.',
      };
    }
    return { allowed: true, reason: null };
  });

  protected setTab(value: string | number | undefined): void {
    const tab: DetailTab = parseTab(String(value));
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { [TAB_PARAM]: historyTabToParam(this.area, tab), [TREATMENT_PARAM]: null },
      queryParamsHandling: 'merge',
    });
  }

  protected retryPet(): void {
    this.petRetry.next();
  }

  protected retryTreatments(): void {
    this.treatmentsRetry.next();
  }

  protected retryWeight(): void {
    this.weightRetry.next();
  }

  protected retryVaccines(): void {
    this.vaccinesRetry.next();
  }

  // Historia clínica completa en PDF: pide el dueño y la foto al momento y carga jsPDF solo entonces.
  protected downloadPdf(): void {
    const pet = this.pet();
    const treatments = this.treatments();
    if (!pet || treatments.status !== 'ready' || this.downloading()) {
      return;
    }
    this.downloading.set(true);
    forkJoin({
      owner: this.ownerService.getById(pet.ownerId),
      photo: this.petService.getPhoto(pet.id).pipe(catchError(() => of(null))),
    })
      .pipe(
        switchMap(async ({ owner, photo }) => {
          const { downloadClinicalHistoryPdf } = await import('./clinical-history-pdf');
          await downloadClinicalHistoryPdf({ pet, owner, visits: toVisits(treatments.data) }, photo);
        }),
        finalize(() => this.downloading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: (error: unknown) =>
          this.messages.add({
            severity: 'error',
            summary: 'No se pudo descargar el PDF',
            detail: getErrorMessage(error),
            life: 5000,
          }),
      });
  }

  private demoFor<T>(
    pet: Pet | null,
    load: (pet: Pet) => Observable<T>,
    state: ReturnType<typeof toViewState<T>>,
  ): Observable<ViewState<T>> {
    return pet ? load(pet).pipe(state) : of(LOADING as ViewState<T>);
  }
}
