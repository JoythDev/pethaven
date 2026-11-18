import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideChevronLeft, LucidePencil, LucideTrash2 } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { BehaviorSubject, combineLatest, finalize, map, of, switchMap } from 'rxjs';

import { DRUG_NOT_FOUND, DrugLoad, toDrugLoad } from '../../../../core/drugs/drug-detail';
import { DrugRow, buildDrugRows, drugStatus } from '../../../../core/drugs/drugs-list';
import { parseRouteId } from '../../../../core/http/route-id';
import { identityHue } from '../../../../core/identity/identity';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { Drug } from '../../../../models/drug.model';
import { DrugService } from '../../../../services/drug.service';
import { DrugFormDialogComponent } from '../drug-form-dialog/drug-form-dialog.component';
import { BlockComponent } from '../../block/block.component';
import { CardComponent } from '../../card/card.component';
import { DefinitionListComponent } from '../../definition-list/definition-list.component';
import { EmptyStateComponent } from '../../empty-state/empty-state.component';
import { InlineErrorComponent } from '../../inline-error/inline-error.component';
import { SkeletonComponent } from '../../skeleton/skeleton.component';
import { StickerComponent } from '../../sticker/sticker.component';
import { StockMeterComponent } from '../../stock-meter/stock-meter.component';
import { readDrugArea } from '../drug-area';
import { confirmDrugDeletion } from '../drug-delete-confirmation';
import { LOW_STOCK_THRESHOLD, formatCurrency, formatPercent, formatUnits, getMarginPercent } from '../drug-stock';

const LOADING = { status: 'loading' } as const;

/**
 * Ficha de un medicamento (11.2): cabecera en su color de identidad con el estado y las acciones que el
 * rol puede hacer, y tarjetas «Existencias» y «Precios y ventas». El veterinario solo
 * consulta y ve el precio de venta; el administrador además el de compra, el margen y el ingreso.
 * «Editar» abre el diálogo de edición en la misma ficha (como en la ficha del veterinario), sin cambiar de
 * ruta, y al guardar la ficha se actualiza sin volver al esqueleto.
 */
@Component({
  selector: 'app-drug-detail',
  imports: [
    RouterLink,
    ButtonModule,
    TagModule,
    LucideChevronLeft,
    LucidePencil,
    LucideTrash2,
    BlockComponent,
    CardComponent,
    DefinitionListComponent,
    DrugFormDialogComponent,
    EmptyStateComponent,
    InlineErrorComponent,
    SkeletonComponent,
    StickerComponent,
    StockMeterComponent,
  ],
  templateUrl: './drug-detail.component.html',
})
export class DrugDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly drugService = inject(DrugService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly area = readDrugArea(this.route);

  protected readonly state = signal<DrugLoad>(LOADING);
  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));
  private readonly deleting = signal(false);
  protected readonly editing = signal(false);
  private readonly reload$ = new BehaviorSubject<void>(undefined);
  // Al recargar tras guardar se conservan los datos que ya se ven en vez de volver al esqueleto.
  private silentReload = false;

  // Con los datos listos: el medicamento con su nivel de existencias.
  protected readonly row = computed<DrugRow | null>(() => {
    const state = this.state();
    return state.status === 'ready' ? buildDrugRows([state.data])[0] : null;
  });
  protected readonly hue = computed(() => identityHue(this.row()?.id ?? 0));

  protected readonly lowStockThreshold = LOW_STOCK_THRESHOLD;
  protected readonly status = drugStatus;
  protected readonly formatCurrency = formatCurrency;
  protected readonly formatUnits = formatUnits;
  protected readonly formatPercent = formatPercent;
  protected readonly getMarginPercent = getMarginPercent;

  constructor() {
    // Lee el :id de la URL; un id inválido es «no encontrado» sin llamar al API. switchMap descarta la
    // consulta anterior si el id o la recarga cambian.
    combineLatest([this.route.paramMap.pipe(map((params) => parseRouteId(params.get('id')))), this.reload$])
      .pipe(
        switchMap(([id]) =>
          id === null ? of<DrugLoad>(DRUG_NOT_FOUND) : this.drugService.getById(id).pipe(toDrugLoad()),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((state) => {
        if (state.status === 'loading' && this.silentReload) {
          return;
        }
        if (state.status !== 'loading') {
          this.silentReload = false;
        }
        this.state.set(state);
      });
  }

  protected retry(): void {
    this.state.set(LOADING);
    this.reload$.next();
  }

  /** El diálogo guardó: la ficha se recarga con los datos nuevos. */
  protected onSaved(): void {
    this.silentReload = true;
    this.reload$.next();
  }

  protected remove(drug: Drug): void {
    confirmDrugDeletion(this.confirmationService, drug, () => this.deleteDrug(drug));
  }

  // Si el API rechaza la eliminación (409 por tratamientos asociados) muestra su mensaje y deja al usuario
  // en la ficha; si funciona, avisa y regresa a la lista.
  private deleteDrug(drug: Drug): void {
    if (this.deleting()) {
      return;
    }
    this.deleting.set(true);
    this.drugService
      .delete(drug.id)
      .pipe(
        finalize(() => this.deleting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Medicamento eliminado', `${drug.name} se eliminó correctamente.`);
          void this.router.navigate([this.area.basePath]);
        },
        error: (error: unknown) => this.notifications.error('No se pudo eliminar el medicamento', error),
      });
  }
}
