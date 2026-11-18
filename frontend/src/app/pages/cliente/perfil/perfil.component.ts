import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { BehaviorSubject, combineLatest, finalize, of, switchMap } from 'rxjs';

import { ownerIdParam } from '../../../core/client-pets/owner-route';
import { HasUnsavedChanges } from '../../../core/forms/unsaved-changes.guard';
import { applyServerErrors } from '../../../core/http/http-error';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { NotificationService } from '../../../core/notifications/notification.service';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { Owner } from '../../../models/owner.model';
import { AuthService } from '../../../services/auth.service';
import { OwnerService } from '../../../services/owner.service';
import { CardComponent } from '../../shared/card/card.component';
import { FormFieldComponent } from '../../shared/form-field/form-field.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { ProfileAppearanceCardComponent } from '../../shared/profile/profile-appearance-card.component';
import { ProfileHeaderComponent } from '../../shared/profile/profile-header.component';
import { ProfileLayoutComponent } from '../../shared/profile/profile-layout.component';
import { ProfileSecurityCardComponent } from '../../shared/profile/profile-security-card.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { ClienteContextStore } from '../cliente-context.store';
import { OwnerProfileField, buildOwnerUpdate, ownerProfileFieldError } from './perfil-form';

const OWNER_NOT_FOUND: ViewState<Owner> = { status: 'error', message: 'Dueño no encontrado.' };

// Orden de los campos editables; el id es el del elemento que recibe el foco.
const FIELD_ORDER: readonly OwnerProfileField[] = ['name', 'phone'];

/**
 * Perfil del cliente: bloque de color con su identidad, «Datos personales» (nombre y teléfono se
 * editan y se guardan en el API; cédula y correo no se pueden cambiar, porque el API lo rechaza
 * cuando quien guarda es el propio dueño), «Apariencia» y «Seguridad». Valida al salir del campo y al
 * enviar, y el botón de guardar siempre está activo (11.4). Implementa `HasUnsavedChanges` para la
 * guarda de rutas.
 */
@Component({
  selector: 'app-cliente-perfil',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
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
export class ClientePerfilComponent implements OnInit, HasUnsavedChanges {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly ownerService = inject(OwnerService);
  private readonly context = inject(ClienteContextStore);
  private readonly notifications = inject(NotificationService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  private readonly ownerId$ = ownerIdParam(this.route);
  private readonly retry$ = new BehaviorSubject<void>(undefined);

  protected readonly state = signal<ViewState<Owner>>({ status: 'loading' });
  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));
  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);

  // Pattern /\S/ rechaza textos solo con espacios, que el API rechaza con @NotBlank.
  protected readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    phone: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(20)]],
  });

  // Cómo guarda la contraseña la tarjeta «Seguridad»: el mismo PUT con lo ya guardado y la clave nueva.
  protected readonly savePassword = computed(() => {
    const state = this.state();
    if (state.status !== 'ready') {
      return null;
    }
    const owner = state.data;
    return (password: string) => this.ownerService.update(owner.id, buildOwnerUpdate(owner, { password }));
  });

  // El primer HTTP sale de ngOnInit, no del constructor.
  ngOnInit(): void {
    combineLatest([this.ownerId$, this.retry$])
      .pipe(
        switchMap(([ownerId]) => {
          this.state.set({ status: 'loading' });
          return ownerId === null ? of(OWNER_NOT_FOUND) : this.ownerService.getById(ownerId).pipe(toViewState<Owner>());
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => {
        if (state.status === 'ready') {
          this.applyOwner(state.data);
        }
        this.state.set(state);
      });
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.saving();
  }

  protected retry(): void {
    this.retry$.next();
  }

  // Mensaje visible de un campo: el del servidor siempre; el de validación, tras salir del campo o al enviar.
  protected errorFor(field: OwnerProfileField): string | null {
    const control = this.form.controls[field];
    if (!control.errors || !(control.touched || this.submitted() || 'server' in control.errors)) {
      return null;
    }
    return ownerProfileFieldError(field, control.errors);
  }

  // El botón no se deshabilita (11.4): con errores los muestra y enfoca el primer campo inválido.
  protected guardar(owner: Owner): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    if (this.form.invalid) {
      this.focusFirstInvalid();
      return;
    }

    this.saving.set(true);
    this.ownerService
      .update(owner.id, buildOwnerUpdate(owner, this.form.getRawValue()))
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (saved) => {
          this.applyOwner(saved);
          // La barra superior toma el nombre del contexto del área; el menú de usuario, de la sesión.
          this.context.update(saved);
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
  private applyOwner(owner: Owner): void {
    this.form.reset({ name: owner.name, phone: owner.phone });
    this.submitted.set(false);
  }

  private focusFirstInvalid(): void {
    const first = FIELD_ORDER.find((field) => this.form.controls[field].invalid);
    if (first) {
      this.document.getElementById(first)?.focus();
    }
  }
}
