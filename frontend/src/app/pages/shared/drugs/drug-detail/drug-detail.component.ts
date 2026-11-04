import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { EMPTY, Subject, catchError, exhaustMap, map, of, switchMap, tap } from 'rxjs';
import { Drug } from '../../../../models/drug.model';
import { DrugService } from '../../../../services/drug.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { readDrugArea } from '../drug-area';
import { confirmDrugDeletion } from '../drug-delete-confirmation';
import {
  LOW_STOCK_THRESHOLD,
  formatCurrency,
  formatPercent,
  formatUnits,
  getMargin,
  getMarginPercent,
  getStockInfo,
} from '../drug-stock';

// Detalle de un medicamento compartido por el administrador y el veterinario. Editar y
// eliminar solo se muestran cuando la ruta lo permite (canManage).
@Component({
  selector: 'app-drug-detail',
  imports: [RouterLink, ButtonModule, TagModule],
  templateUrl: './drug-detail.component.html',
})
export class DrugDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly drugService = inject(DrugService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly area = readDrugArea(this.route);
  readonly lowStockThreshold = LOW_STOCK_THRESHOLD;

  // Cada emisión es el medicamento cuya eliminación el usuario ya confirmó.
  private readonly confirmedDelete$ = new Subject<Drug>();

  readonly state = signal<ViewState<Drug>>({ status: 'loading' });

  readonly formatCurrency = formatCurrency;
  readonly formatUnits = formatUnits;
  readonly formatPercent = formatPercent;
  readonly getMargin = getMargin;
  readonly getMarginPercent = getMarginPercent;
  readonly getStockInfo = getStockInfo;

  // Lee el :id de la URL; un id inválido termina en error sin llamar al API. switchMap descarta
  // la consulta anterior si el id cambia, y toViewState emite "cargando" y luego "listo" o "error".
  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        map((params) => parseRouteId(params.get('id'))),
        switchMap((id) =>
          id === null
            ? of<ViewState<Drug>>({ status: 'error', message: 'El medicamento solicitado no existe.' })
            : this.drugService.getById(id).pipe(toViewState<Drug>()),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => this.state.set(state));

    // exhaustMap ignora confirmaciones repetidas mientras una eliminación sigue en curso. Si el
    // API la rechaza (409 por tratamientos asociados) muestra su mensaje y deja al usuario aquí.
    this.confirmedDelete$
      .pipe(
        exhaustMap((drug) =>
          this.drugService.delete(drug.id).pipe(
            tap(() => this.notifications.success('Medicamento eliminado', `${drug.name} se eliminó correctamente.`)),
            catchError((error: unknown) => {
              this.notifications.error('No se pudo eliminar el medicamento', error);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.volver());
  }

  editar(id: number): void {
    this.router.navigate([this.area.basePath, 'update', id]);
  }

  volver(): void {
    this.router.navigate([this.area.basePath]);
  }

  eliminar(drug: Drug): void {
    confirmDrugDeletion(this.confirmationService, drug, () => this.confirmedDelete$.next(drug));
  }
}
