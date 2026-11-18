import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { finalize } from 'rxjs';

import { DRUG_LIMITS, DrugFormField, drugFieldError, priceWarning, toDrugRequest } from '../../../../core/drugs/drug-form';
import { applyServerErrors } from '../../../../core/http/http-error';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { Drug } from '../../../../models/drug.model';
import { DrugService } from '../../../../services/drug.service';
import { FormFieldComponent } from '../../../shared/form-field/form-field.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';

// Orden de los campos en pantalla; el id es el del elemento que recibe el foco (los usa Selenium).
const FIELD_ORDER: readonly DrugFormField[] = ['name', 'unitsAvailable', 'purchasePrice', 'salePrice'];

/**
 * Alta y edición de un medicamento en un diálogo (11.4: seis campos o menos). Valida al salir del
 * campo y al enviar, y enfoca el primer campo inválido; el botón de guardar siempre está activo.
 * Vender por debajo del costo solo avisa (no bloquea). Cerrar con cambios sin guardar pide
 * confirmación. Se crea cuando hay que mostrarlo y se destruye al cerrar.
 *
 * - `saved` se emite en cuanto el API guarda, para que la página recargue sus datos.
 * - `closed` se emite cuando la persona termina (cerrar o tras guardar).
 */
@Component({
  selector: 'app-drug-form-dialog',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    FormFieldComponent,
    SkeletonComponent,
  ],
  templateUrl: './drug-form-dialog.component.html',
})
export class DrugFormDialogComponent implements OnInit {
  private readonly drugService = inject(DrugService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  /** Con id, edita ese medicamento; sin él, registra uno nuevo. */
  readonly drugId = input<number | null>(null);

  readonly saved = output<Drug>();
  readonly closed = output<void>();

  protected readonly limits = DRUG_LIMITS;
  protected readonly editing = computed(() => this.drugId() !== null);
  protected readonly loading = signal(false);
  protected readonly skeleton = skeletonVisible(this.loading);
  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);

  protected readonly title = computed(() => (this.editing() ? 'Editar medicamento' : 'Nuevo medicamento'));

  // Pattern /\S/ rechaza nombres solo con espacios, que el API rechaza con @NotBlank. Los números empiezan
  // en null para que el campo se vea vacío; sin min ni max en los campos para que se vea el mensaje de rango.
  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(DRUG_LIMITS.nameMaxLength)]],
    unitsAvailable: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      Validators.max(DRUG_LIMITS.unitsMax),
    ]),
    purchasePrice: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      Validators.max(DRUG_LIMITS.priceMax),
    ]),
    salePrice: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      Validators.max(DRUG_LIMITS.priceMax),
    ]),
  });

  ngOnInit(): void {
    const id = this.drugId();
    if (id === null) {
      return;
    }

    this.loading.set(true);
    this.drugService
      .getById(id)
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (drug) => {
          this.form.patchValue({
            name: drug.name,
            unitsAvailable: drug.unitsAvailable,
            purchasePrice: drug.purchasePrice,
            salePrice: drug.salePrice,
          });
          this.form.markAsPristine();
          setTimeout(() => this.focusName());
        },
        error: (error: unknown) => {
          this.notifications.error('Medicamento no encontrado', error);
          this.closed.emit();
        },
      });
  }

  // Mensaje visible de un campo: el del servidor siempre; el de validación, tras salir del campo o al enviar.
  protected errorFor(field: DrugFormField): string | null {
    const control = this.form.controls[field];
    if (!control.errors || !(control.touched || this.submitted() || 'server' in control.errors)) {
      return null;
    }
    return drugFieldError(field, control.errors);
  }

  // Aviso de venta por debajo del costo: aparece al salir del precio de venta y no impide guardar.
  protected salePriceWarning(): string | null {
    const { purchasePrice, salePrice } = this.form.controls;
    if (!(salePrice.touched || this.submitted())) {
      return null;
    }
    return priceWarning(purchasePrice.value, salePrice.value);
  }

  // El foco inicial va al primer campo; PrimeNG lo dejaría en el pie del diálogo.
  protected focusName(): void {
    if (!this.loading()) {
      this.document.getElementById('name')?.focus();
    }
  }

  // Valida el formulario y, si es válido, lo guarda. El botón no se deshabilita (11.4): con errores
  // los muestra y enfoca el primer campo inválido.
  protected guardar(): void {
    if (this.loading() || this.saving()) {
      return;
    }
    this.submitted.set(true);
    if (this.form.invalid) {
      this.focusFirstInvalid();
      return;
    }
    this.save();
  }

  // X, Escape y «Cancelar»: con cambios sin guardar pide confirmación (mismo texto que la guarda de rutas).
  protected requestClose(): void {
    if (!this.form.dirty || this.saving()) {
      this.closed.emit();
      return;
    }
    this.confirmation.confirm({
      header: 'Salir sin guardar',
      message: 'Tienes cambios sin guardar. Si sales ahora, se perderán.',
      acceptLabel: 'Salir sin guardar',
      rejectLabel: 'Seguir editando',
      rejectButtonProps: { outlined: true, severity: 'secondary' },
      acceptButtonProps: { severity: 'danger' },
      accept: () => this.closed.emit(),
    });
  }

  // Si el API responde 400 por campo, el error va a cada control y la persona se queda en el diálogo
  // con lo que escribió. Otros errores (p. ej. 409 por nombre repetido) muestran su mensaje en un aviso
  // y dejan reintentar.
  private save(): void {
    this.saving.set(true);

    const body = toDrugRequest(this.form.getRawValue());
    const id = this.drugId();
    const saved$ = id === null ? this.drugService.create(body) : this.drugService.update(id, body);

    saved$
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (drug) => {
          this.form.markAsPristine();
          this.saved.emit(drug);
          this.notifications.success(
            'Medicamento guardado',
            `${drug.name} se ${id === null ? 'registró' : 'actualizó'} correctamente.`,
          );
          this.closed.emit();
        },
        error: (error: unknown) => {
          if (applyServerErrors(this.form, error)) {
            this.notifications.warn('Revisa los campos marcados.', '');
            this.focusFirstInvalid();
          } else {
            this.notifications.error('No se pudo guardar el medicamento', error);
          }
        },
      });
  }

  private focusFirstInvalid(): void {
    const first = FIELD_ORDER.find((field) => this.form.controls[field].invalid);
    if (first) {
      this.document.getElementById(first)?.focus();
    }
  }
}
