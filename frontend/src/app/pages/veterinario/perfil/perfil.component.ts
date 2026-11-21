import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { finalize, of } from 'rxjs';

import { HasUnsavedChanges } from '../../../core/forms/unsaved-changes.guard';
import { applyServerErrors } from '../../../core/http/http-error';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { NotificationService } from '../../../core/notifications/notification.service';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { Veterinarian } from '../../../models/veterinarian.model';
import { AuthService } from '../../../services/auth.service';
import { VeterinarianService } from '../../../services/veterinarian.service';
import { CardComponent } from '../../shared/card/card.component';
import { FormFieldComponent } from '../../shared/form-field/form-field.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { ProfileAppearanceCardComponent } from '../../shared/profile/profile-appearance-card.component';
import { ProfileHeaderComponent } from '../../shared/profile/profile-header.component';
import { ProfileLayoutComponent } from '../../shared/profile/profile-layout.component';
import { ProfileSecurityCardComponent } from '../../shared/profile/profile-security-card.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { VetProfileField, buildVetUpdate, vetProfileFieldError } from './perfil-form';

const NOT_VETERINARIAN = 'Inicia sesión como veterinario para ver tu perfil.';

// Orden de los campos editables; el id es el del elemento que recibe el foco.
const FIELD_ORDER: readonly VetProfileField[] = ['name', 'specialty'];

/**
 * Perfil del veterinario: bloque de color con su identidad, «Datos personales» (nombre y
 * especialidad se editan y se guardan en el API; cédula y correo no se pueden cambiar),
 * «Apariencia» y «Seguridad». Valida al salir del campo y al enviar, y el botón de guardar siempre
 * está activo (11.4). Implementa `HasUnsavedChanges` para la guarda de rutas.
 */
@Component({
  selector: 'app-veterinario-perfil',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    TagModule,
    CardComponent,
    FormFieldComponent,
    InlineErrorComponent,
    ProfileAppearanceCardComponent,
    ProfileHeaderComponent,
    ProfileLayoutComponent,
    ProfileSecurityCardComponent,
    SkeletonComponent,
  ],
  templateUrl: './perfil.component.html',
})
export class VeterinarioPerfilComponent implements OnInit, HasUnsavedChanges {
  private readonly auth = inject(AuthService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly notifications = inject(NotificationService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  protected readonly state = signal<ViewState<Veterinarian>>({ status: 'loading' });
  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));
  // Sin sesión de veterinario no hay nada que reintentar: se ofrece ir al inicio de sesión.
  protected readonly signedOut = signal(false);
  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);

  // Pattern /\S/ rechaza textos solo con espacios, que el API rechaza con @NotBlank.
  protected readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    specialty: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(80)]],
  });

  // Cómo guarda la contraseña la tarjeta «Seguridad»: el mismo PUT con lo ya guardado y la clave nueva.
  protected readonly savePassword = computed(() => {
    const state = this.state();
    if (state.status !== 'ready') {
      return null;
    }
    const vet = state.data;
    return (password: string) => this.veterinarianService.update(vet.id, buildVetUpdate(vet, { password }));
  });

  // El primer HTTP sale de ngOnInit, no del constructor.
  ngOnInit(): void {
    this.load();
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.saving();
  }

  protected load(): void {
    this.state.set({ status: 'loading' });
    const user = this.auth.user();
    const isVet = user?.role === 'VETERINARIAN';
    this.signedOut.set(!isVet);

    // Sin sesión de veterinario termina en error sin llamar al API.
    const profile$ = isVet
      ? this.veterinarianService.getById(user.id).pipe(toViewState<Veterinarian>())
      : of<ViewState<Veterinarian>>({ status: 'error', message: NOT_VETERINARIAN });

    profile$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((state) => {
      if (state.status === 'ready') {
        this.applyVet(state.data);
      }
      this.state.set(state);
    });
  }

  // Mensaje visible de un campo: el del servidor siempre; el de validación, tras salir del campo o al enviar.
  protected errorFor(field: VetProfileField): string | null {
    const control = this.form.controls[field];
    if (!control.errors || !(control.touched || this.submitted() || 'server' in control.errors)) {
      return null;
    }
    return vetProfileFieldError(field, control.errors);
  }

  // El botón no se deshabilita (11.4): con errores los muestra y enfoca el primer campo inválido.
  protected guardar(vet: Veterinarian): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    if (this.form.invalid) {
      this.focusFirstInvalid();
      return;
    }

    this.saving.set(true);
    this.veterinarianService
      .update(vet.id, buildVetUpdate(vet, this.form.getRawValue()))
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (saved) => {
          this.applyVet(saved);
          this.auth.updateName(saved.name);
          this.state.set({ status: 'ready', data: saved });
          this.notifications.success('Perfil actualizado', 'Tus datos se guardaron correctamente.');
        },
        error: (error: unknown) => {
          // 400 por campo: cada error va a su control y la persona conserva lo que escribió.
          if (applyServerErrors(this.form, error)) {
            this.notifications.warn('Revisa los campos marcados.', '');
            this.focusFirstInvalid();
          } else {
            this.notifications.error('No se pudo guardar tu perfil', error);
          }
        },
      });
  }

  // Lleva los datos guardados al formulario y lo deja sin cambios pendientes.
  private applyVet(vet: Veterinarian): void {
    this.form.reset({ name: vet.name, specialty: vet.specialty });
    this.submitted.set(false);
  }

  private focusFirstInvalid(): void {
    const first = FIELD_ORDER.find((field) => this.form.controls[field].invalid);
    if (first) {
      this.document.getElementById(first)?.focus();
    }
  }
}
