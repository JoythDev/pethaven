import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideEye, LucideEyeOff } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { finalize } from 'rxjs';

import { applyServerErrors } from '../../../../core/http/http-error';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { VetFormField, vetFieldError } from '../../../../core/vets/vet-form';
import { Veterinarian } from '../../../../models/veterinarian.model';
import { VeterinarianService } from '../../../../services/veterinarian.service';
import { FormFieldComponent } from '../../../shared/form-field/form-field.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';

// Longitud permitida de la contraseña (la misma que valida el API).
const PASSWORD_LENGTH_VALIDATORS = [Validators.minLength(8), Validators.maxLength(255)];

// Orden de los campos en pantalla; el id es el del elemento que recibe el foco (los usa Selenium).
const FIELD_ORDER: readonly VetFormField[] = ['name', 'document', 'specialty', 'email', 'password', 'photoUrl'];

/**
 * Alta y edición de un veterinario en un diálogo (11.4: seis campos o menos). Valida al salir del
 * campo y al enviar, y enfoca el primer campo inválido; el botón de guardar siempre está activo.
 * Cerrar con cambios sin guardar pide confirmación. Se crea cuando hay que mostrarlo y se destruye
 * al cerrar.
 *
 * - `saved` se emite en cuanto el API guarda, para que la página recargue sus datos.
 * - `closed` se emite cuando la persona termina (cerrar o tras guardar).
 */
@Component({
  selector: 'app-vet-form-dialog',
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
  ],
  templateUrl: './vet-form-dialog.component.html',
})
export class VetFormDialogComponent implements OnInit {
  private readonly vetService = inject(VeterinarianService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  /** Con id, edita a ese veterinario; sin él, registra uno nuevo. */
  readonly vetId = input<number | null>(null);

  readonly saved = output<Veterinarian>();
  readonly closed = output<void>();

  protected readonly editing = computed(() => this.vetId() !== null);
  protected readonly loading = signal(false);
  protected readonly skeleton = skeletonVisible(this.loading);
  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);
  protected readonly showPassword = signal(false);

  protected readonly title = computed(() => (this.editing() ? 'Editar veterinario' : 'Nuevo veterinario'));

  // Pattern /\S/ rechaza textos solo con espacios, que el API rechaza con @NotBlank.
  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    document: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(20)]],
    specialty: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: ['', [Validators.required, ...PASSWORD_LENGTH_VALIDATORS]],
    photoUrl: ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    const id = this.vetId();
    if (id === null) {
      return;
    }

    // En edición, vacía significa «conservar la actual», pero si se escribe una debe cumplir la
    // longitud del API. La contraseña no se rellena porque el API nunca la devuelve.
    const password = this.form.controls.password;
    password.setValidators(PASSWORD_LENGTH_VALIDATORS);
    password.updateValueAndValidity();

    this.loading.set(true);
    this.vetService
      .getById(id)
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (vet) => {
          this.form.patchValue({
            name: vet.name,
            document: vet.document,
            specialty: vet.specialty,
            email: vet.email,
            photoUrl: vet.photoUrl ?? '',
          });
          this.form.markAsPristine();
          setTimeout(() => this.focusName());
        },
        error: (error: unknown) => {
          this.notifications.error('Veterinario no encontrado', error);
          this.closed.emit();
        },
      });
  }

  // Mensaje visible de un campo: el del servidor siempre; el de validación, tras salir del campo o al enviar.
  protected errorFor(field: VetFormField): string | null {
    const control = this.form.controls[field];
    if (!control.errors || !(control.touched || this.submitted() || 'server' in control.errors)) {
      return null;
    }
    return vetFieldError(field, control.errors, { editing: this.editing() });
  }

  // El foco inicial va al primer campo; PrimeNG lo dejaría en el pie del diálogo.
  protected focusName(): void {
    if (!this.loading()) {
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
      specialty: value.specialty.trim(),
      photoUrl: value.photoUrl.trim() || null,
    };
    const id = this.vetId();
    const saved$ =
      id === null
        ? this.vetService.create({ ...body, password: value.password })
        : this.vetService.update(id, { ...body, password: value.password || null });

    saved$
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (vet) => {
          this.form.markAsPristine();
          this.saved.emit(vet);
          this.notifications.success(
            id === null ? 'Veterinario registrado' : 'Veterinario actualizado',
            `${vet.name} se ${id === null ? 'registró' : 'actualizó'} correctamente.`,
          );
          this.closed.emit();
        },
        error: (error: unknown) => {
          if (applyServerErrors(this.form, error)) {
            this.notifications.warn('Revisa los campos marcados.', '');
            this.focusFirstInvalid();
          } else {
            this.notifications.error('No se pudo guardar al veterinario', error);
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
