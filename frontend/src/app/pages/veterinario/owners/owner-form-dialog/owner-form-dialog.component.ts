import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideEye, LucideEyeOff } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { finalize } from 'rxjs';

import { applyServerErrors } from '../../../../core/http/http-error';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { OwnerFormField, newPetUrl, ownerFieldError, ownerPetQuery } from '../../../../core/owners/owner-form';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { Owner } from '../../../../models/owner.model';
import { OwnerService } from '../../../../services/owner.service';
import { FormFieldComponent } from '../../../shared/form-field/form-field.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../../shared/sticker/sticker.component';

// Longitud permitida de la contraseña (la misma que valida el API).
const PASSWORD_LENGTH_VALIDATORS = [Validators.minLength(8), Validators.maxLength(255)];

// Orden de los campos en pantalla; el id es el del elemento que recibe el foco (los usa Selenium).
const FIELD_ORDER: readonly OwnerFormField[] = ['name', 'document', 'phone', 'email', 'password'];

/**
 * Alta y edición de un dueño en un diálogo (11.4: seis campos o menos). Valida al salir del campo y
 * al enviar, y enfoca el primer campo inválido; el botón de guardar siempre está activo. Cerrar con
 * cambios sin guardar pide confirmación. Tras crear, el mismo diálogo ofrece registrar una mascota
 * para ese dueño (11.12). Se crea cuando hay que mostrarlo y se destruye al cerrar.
 *
 * - `saved` se emite en cuanto el API guarda, para que la página recargue sus datos.
 * - `closed` se emite cuando la persona termina (cerrar, «Más tarde» o tras editar).
 */
@Component({
  selector: 'app-owner-form-dialog',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    TooltipModule,
    LucideEye,
    LucideEyeOff,
    FormFieldComponent,
    SkeletonComponent,
    StickerComponent,
  ],
  templateUrl: './owner-form-dialog.component.html',
})
export class OwnerFormDialogComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly ownerService = inject(OwnerService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  /** Con id, edita a ese dueño; sin él, registra uno nuevo. */
  readonly ownerId = input<number | null>(null);

  readonly saved = output<Owner>();
  readonly closed = output<void>();

  protected readonly editing = computed(() => this.ownerId() !== null);
  protected readonly loading = signal(false);
  protected readonly skeleton = skeletonVisible(this.loading);
  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);
  protected readonly showPassword = signal(false);

  // `form` es el alta o la edición; `done` es la confirmación con la pregunta de la mascota (11.12).
  protected readonly step = signal<'form' | 'done'>('form');
  protected readonly savedOwner = signal<Owner | null>(null);

  protected readonly title = computed(() => {
    if (this.step() === 'done') {
      return `${this.savedOwner()?.name ?? 'El dueño'} quedó registrado`;
    }
    return this.editing() ? 'Editar dueño' : 'Nuevo dueño';
  });

  // Pattern /\S/ rechaza textos solo con espacios, que el API rechaza con @NotBlank.
  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    document: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(20)]],
    phone: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(20)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: ['', [Validators.required, ...PASSWORD_LENGTH_VALIDATORS]],
  });

  ngOnInit(): void {
    const id = this.ownerId();
    if (id === null) {
      return;
    }

    // En edición, vacía significa «conservar la actual», pero si se escribe una debe cumplir la
    // longitud del API. La contraseña no se rellena porque el API nunca la devuelve.
    const password = this.form.controls.password;
    password.setValidators(PASSWORD_LENGTH_VALIDATORS);
    password.updateValueAndValidity();

    this.loading.set(true);
    this.ownerService
      .getById(id)
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (owner) => {
          this.form.patchValue({
            name: owner.name,
            document: owner.document,
            phone: owner.phone,
            email: owner.email,
          });
          this.form.markAsPristine();
          setTimeout(() => this.focusName());
        },
        error: (error: unknown) => {
          this.notifications.error('Dueño no encontrado', error);
          this.closed.emit();
        },
      });
  }

  // Mensaje visible de un campo: el del servidor siempre; el de validación, tras salir del campo o al enviar.
  protected errorFor(field: OwnerFormField): string | null {
    const control = this.form.controls[field];
    if (!control.errors || !(control.touched || this.submitted() || 'server' in control.errors)) {
      return null;
    }
    return ownerFieldError(field, control.errors, { editing: this.editing() });
  }

  // El foco inicial va al primer campo; PrimeNG lo dejaría en el pie del diálogo.
  protected focusName(): void {
    if (this.step() === 'form' && !this.loading()) {
      this.document.getElementById('name')?.focus();
    }
  }

  protected togglePassword(): void {
    this.showPassword.update((visible) => !visible);
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
    if (this.step() === 'done') {
      this.later();
      return;
    }
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

  /** «Registrar»: lleva al formulario de mascota con este dueño ya elegido. */
  protected registerPet(): void {
    const owner = this.savedOwner();
    if (owner) {
      void this.router.navigate([newPetUrl()], { queryParams: ownerPetQuery(owner.id) });
    }
  }

  /** «Más tarde»: cierra el diálogo y muestra el aviso de siempre. */
  protected later(): void {
    const owner = this.savedOwner();
    if (owner) {
      this.notifications.success('Dueño registrado', `${owner.name} se registró correctamente.`);
    }
    this.closed.emit();
  }

  // Los textos libres se recortan antes de enviarse. Si el API responde 400 por campo, el error va a
  // cada control y la persona se queda en el diálogo con lo que escribió. Otros errores (p. ej. 409
  // por cédula o correo repetidos) muestran su mensaje y dejan reintentar.
  private save(): void {
    this.saving.set(true);

    const value = this.form.getRawValue();
    const body = {
      document: value.document.trim(),
      name: value.name.trim(),
      email: value.email.trim(),
      phone: value.phone.trim(),
    };
    const id = this.ownerId();
    const saved$ =
      id === null
        ? this.ownerService.create({ ...body, password: value.password })
        : this.ownerService.update(id, { ...body, password: value.password || null });

    saved$
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (owner) => {
          this.form.markAsPristine();
          this.saved.emit(owner);
          if (id === null) {
            this.savedOwner.set(owner);
            this.step.set('done');
            setTimeout(() => this.document.getElementById('btnRegistrarMascota')?.focus());
          } else {
            this.notifications.success('Dueño actualizado', `${owner.name} se actualizó correctamente.`);
            this.closed.emit();
          }
        },
        error: (error: unknown) => {
          if (applyServerErrors(this.form, error)) {
            this.notifications.warn('Revisa los campos marcados.', '');
            this.focusFirstInvalid();
          } else {
            this.notifications.error('No se pudo guardar al dueño', error);
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
