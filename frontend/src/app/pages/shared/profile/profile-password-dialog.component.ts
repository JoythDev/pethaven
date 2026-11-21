import { DOCUMENT, NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideEye, LucideEyeOff } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { Observable, finalize } from 'rxjs';

import { applyServerErrors } from '../../../core/http/http-error';
import { NotificationService } from '../../../core/notifications/notification.service';
import { FormFieldComponent } from '../form-field/form-field.component';
import {
  PASSWORD_VALIDATORS,
  PasswordField,
  passwordFieldError,
  passwordsMatchValidator,
} from './profile-password';

const FIELD_ORDER: readonly PasswordField[] = ['password', 'confirm'];

/**
 * Diálogo para cambiar la contraseña, igual para los tres roles: la página pasa `save`, que recibe
 * la contraseña nueva y devuelve la petición que la guarda. Valida al salir del campo y al enviar,
 * enfoca el primer campo inválido y deja el botón siempre activo (11.4). Se crea al abrirlo y se
 * destruye al cerrar; `closed` avisa de que hay que quitarlo.
 */
@Component({
  selector: 'app-profile-password-dialog',
  imports: [
    NgTemplateOutlet,
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    TooltipModule,
    LucideEye,
    LucideEyeOff,
    FormFieldComponent,
  ],
  template: `
    <p-dialog
      [visible]="true"
      [modal]="true"
      [draggable]="false"
      [resizable]="false"
      [closable]="true"
      [closeOnEscape]="true"
      [blockScroll]="true"
      appendTo="body"
      header="Cambiar contraseña"
      closeAriaLabel="Cerrar"
      styleClass="w-[min(28rem,calc(100vw-2rem))]"
      [focusOnShow]="false"
      (onShow)="focusFirst()"
      (visibleChange)="$event ? null : closed.emit()"
    >
      <form id="password-form" [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4" novalidate>
        <app-form-field fieldId="new-password" label="Contraseña nueva" [error]="errorFor('password')">
          <div class="relative">
            <input
              pInputText
              id="new-password"
              [type]="visible() ? 'text' : 'password'"
              formControlName="password"
              autocomplete="new-password"
              class="w-full pr-12"
              [attr.aria-invalid]="errorFor('password') ? 'true' : null"
              [attr.aria-describedby]="errorFor('password') ? 'new-password-error' : null"
            />
            <ng-container *ngTemplateOutlet="toggle" />
          </div>
        </app-form-field>

        <app-form-field fieldId="confirm-password" label="Repite la contraseña" [error]="errorFor('confirm')">
          <div class="relative">
            <input
              pInputText
              id="confirm-password"
              [type]="visible() ? 'text' : 'password'"
              formControlName="confirm"
              autocomplete="new-password"
              class="w-full pr-12"
              [attr.aria-invalid]="errorFor('confirm') ? 'true' : null"
              [attr.aria-describedby]="errorFor('confirm') ? 'confirm-password-error' : null"
            />
            <ng-container *ngTemplateOutlet="toggle" />
          </div>
        </app-form-field>
      </form>

      <ng-template #toggle>
        <button
          type="button"
          (click)="visible.set(!visible())"
          [attr.aria-label]="visible() ? 'Ocultar contraseñas' : 'Mostrar contraseñas'"
          [attr.aria-pressed]="visible()"
          [pTooltip]="visible() ? 'Ocultar contraseñas' : 'Mostrar contraseñas'"
          tooltipPosition="top"
          class="ph-focus absolute top-1 right-1 grid size-[30px] cursor-pointer place-items-center rounded-control text-ink-muted transition-colors duration-[180ms] hover:bg-sunken hover:text-ink motion-reduce:transition-none"
        >
          @if (visible()) {
            <svg lucideEyeOff [size]="18" aria-hidden="true"></svg>
          } @else {
            <svg lucideEye [size]="18" aria-hidden="true"></svg>
          }
        </button>
      </ng-template>

      <ng-template #footer>
        <div class="flex flex-wrap justify-end gap-2">
          <button pButton type="button" severity="secondary" [outlined]="true" label="Cancelar" (click)="closed.emit()"></button>
          <button
            pButton
            id="btnGuardarContrasena"
            type="submit"
            form="password-form"
            label="Guardar contraseña"
            [loading]="saving()"
          ></button>
        </div>
      </ng-template>
    </p-dialog>
  `,
})
export class ProfilePasswordDialogComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  /** Guarda la contraseña nueva; la página decide cómo (cada rol usa su propio servicio). */
  readonly save = input.required<(password: string) => Observable<unknown>>();
  readonly closed = output<void>();

  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);
  protected readonly visible = signal(false);

  protected readonly form = this.formBuilder.group(
    { password: ['', PASSWORD_VALIDATORS], confirm: ['', Validators.required] },
    { validators: [passwordsMatchValidator] },
  );

  // El error del servidor se muestra siempre; el de validación, tras salir del campo o al enviar.
  protected errorFor(field: PasswordField): string | null {
    const control = this.form.controls[field];
    const mismatch = field === 'confirm' && this.form.hasError('mismatch');
    const errors = mismatch ? { ...control.errors, mismatch: true } : control.errors;
    if (!errors || !(control.touched || this.submitted() || 'server' in errors)) {
      return null;
    }
    return passwordFieldError(field, errors);
  }

  // El foco inicial va al primer campo; PrimeNG lo dejaría en el pie del diálogo.
  protected focusFirst(): void {
    this.document.getElementById('new-password')?.focus();
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    if (this.form.invalid) {
      this.focusFirstInvalid();
      return;
    }

    this.saving.set(true);
    this.save()(this.form.controls.password.value)
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Contraseña actualizada', 'Úsala la próxima vez que inicies sesión.');
          this.closed.emit();
        },
        error: (error: unknown) => {
          // El API responde 400 con la clave `password`; cualquier otro error se muestra como aviso.
          if (applyServerErrors(this.form, error)) {
            this.focusFirstInvalid();
          } else {
            this.notifications.error('No se pudo cambiar la contraseña', error);
          }
        },
      });
  }

  private focusFirstInvalid(): void {
    const first = FIELD_ORDER.find((field) => this.errorFor(field) !== null);
    if (first) {
      this.document.getElementById(first === 'password' ? 'new-password' : 'confirm-password')?.focus();
    }
  }
}
