import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { EMPTY, Observable, Subject, catchError, exhaustMap, finalize, map, switchMap } from 'rxjs';
import { Drug, DrugRequest } from '../../../../models/drug.model';
import { DrugService } from '../../../../services/drug.service';
import { applyServerErrors, getErrorMessage } from '../../../../core/http/http-error';
import { parseRouteId } from '../../../../core/http/route-id';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { ADMIN_DRUG_AREA } from '../../../shared/drugs/drug-area';
import { formatUnits } from '../../../shared/drugs/drug-stock';

// Límites de DrugRequest en el API: nombre de 120 caracteres, precios con 10 enteros y 2 decimales
// y unidades como Integer de Java.
export const DRUG_LIMITS = {
  nameMaxLength: 120,
  priceMax: 9_999_999_999.99,
  unitsMax: 2_147_483_647,
} as const;

// Precio con máximo 2 decimales (el API rechaza más con @Digits).
const twoDecimals = Validators.pattern(/^\d+(\.\d{1,2})?$/);

@Component({
  selector: 'app-drug-form',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputNumberModule, InputTextModule, MessageModule],
  templateUrl: './drug-form.component.html',
})
export class DrugFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly drugService = inject(DrugService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly basePath = ADMIN_DRUG_AREA.basePath;
  readonly limits = DRUG_LIMITS;
  readonly formatUnits = formatUnits;

  // Con valor, el formulario edita ese medicamento; con null, registra uno nuevo.
  editandoId: number | null = null;

  // Unidades vendidas del medicamento en edición: solo lectura, las controla el sistema.
  readonly unitsSold = signal<number | null>(null);

  readonly saving = signal(false);

  // Los números empiezan en null para que el campo se vea vacío y "required" funcione.
  // Pattern /\S/ rechaza nombres solo con espacios, que el API rechaza con @NotBlank.
  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(DRUG_LIMITS.nameMaxLength)]],
    purchasePrice: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      Validators.max(DRUG_LIMITS.priceMax),
      twoDecimals,
    ]),
    salePrice: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      Validators.max(DRUG_LIMITS.priceMax),
      twoDecimals,
    ]),
    unitsAvailable: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      Validators.max(DRUG_LIMITS.unitsMax),
      Validators.pattern(/^\d+$/),
    ]),
  });

  // Cada emisión indica que el formulario es válido y debe guardarse en el API.
  private readonly submit$ = new Subject<void>();

  ngOnInit(): void {
    // exhaustMap ignora los envíos que lleguen mientras uno sigue en curso, así un doble clic en
    // Guardar no registra el medicamento dos veces. Al terminar avisa y navega al detalle.
    this.submit$
      .pipe(
        exhaustMap(() => this.save()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((drug) => {
        const editando = this.editandoId !== null;
        this.notifications.success(
          editando ? 'Medicamento actualizado' : 'Medicamento registrado',
          `${drug.name} se ${editando ? 'actualizó' : 'registró'} correctamente.`,
        );
        this.router.navigate([this.basePath, drug.id]);
      });

    // Lee el :id de la URL. Sin id es un alta. Con id, lo valida antes de llamar al API;
    // si es inválido o el medicamento no existe, avisa y regresa al listado.
    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        switchMap((rawId) => {
          if (rawId === null) {
            this.editandoId = null;
            return EMPTY;
          }

          const id = parseRouteId(rawId);
          if (id === null) {
            this.handleNotFound(null);
            return EMPTY;
          }

          this.editandoId = id;
          return this.drugService.getById(id).pipe(
            catchError((error: unknown) => {
              this.handleNotFound(error);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((drug) => {
        this.unitsSold.set(drug.unitsSold);
        this.form.patchValue({
          name: drug.name,
          purchasePrice: drug.purchasePrice,
          salePrice: drug.salePrice,
          unitsAvailable: drug.unitsAvailable,
        });
      });
  }

  // Valida el formulario y, si es válido, avisa a submit$ para que lo guarde.
  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submit$.next();
  }

  cancelar(): void {
    if (this.editandoId !== null) {
      this.router.navigate([this.basePath, this.editandoId]);
    } else {
      this.router.navigate([this.basePath]);
    }
  }

  private handleNotFound(error: unknown): void {
    this.notifications.error('Medicamento no encontrado', error);
    this.router.navigate([this.basePath]);
  }

  // Llama a create o update según haya o no un id en la URL. Un 400 por campo va a cada control;
  // un 409 (nombre repetido) se muestra en el campo nombre. Otros errores muestran un toast.
  // En los tres casos termina sin emitir para que el usuario corrija y reintente.
  private save(): Observable<Drug> {
    this.saving.set(true);

    const value = this.form.getRawValue();
    const body: DrugRequest = {
      name: value.name.trim(),
      purchasePrice: value.purchasePrice ?? 0,
      salePrice: value.salePrice ?? 0,
      unitsAvailable: value.unitsAvailable ?? 0,
    };
    const id = this.editandoId;
    const saved$ = id === null ? this.drugService.create(body) : this.drugService.update(id, body);

    return saved$.pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.form.controls.name.setErrors({ server: getErrorMessage(error) });
          this.form.controls.name.markAsTouched();
          this.notifications.warn('Revisa los campos marcados.', '');
        } else if (applyServerErrors(this.form, error)) {
          this.notifications.warn('Revisa los campos marcados.', '');
        } else {
          this.notifications.error('No se pudo guardar el medicamento', error);
        }
        return EMPTY;
      }),
      finalize(() => this.saving.set(false)),
    );
  }
}
