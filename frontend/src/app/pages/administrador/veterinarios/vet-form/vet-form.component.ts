import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { finalize } from 'rxjs';
import { VeterinarianService } from '../../../../services/veterinarian.service';
import { applyServerErrors } from '../../../../core/http/http-error';
import { parseRouteId } from '../../../../core/http/route-id';
import { NotificationService } from '../../../../core/notifications/notification.service';

// Longitud permitida de la contraseña (la misma que valida el API).
const PASSWORD_LENGTH_VALIDATORS = [Validators.minLength(8), Validators.maxLength(255)];
const LIST_ROUTE = '/administrador/veterinarios';

@Component({
  selector: 'app-vet-form',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, MessageModule],
  templateUrl: './vet-form.component.html',
})
export class VetFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  // Con valor, el formulario edita a ese veterinario; con null, registra a uno nuevo.
  editandoId: number | null = null;
  readonly listRoute = LIST_ROUTE;
  readonly saving = signal(false);

  // Pattern /\S/ rechaza textos solo con espacios, que el API rechaza con @NotBlank.
  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    document: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(20)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    specialty: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(80)]],
    photoUrl: ['', [Validators.maxLength(500)]],
    password: ['', [Validators.required, ...PASSWORD_LENGTH_VALIDATORS]],
  });

  ngOnInit(): void {
    const rawId = this.route.snapshot.paramMap.get('id');
    if (rawId === null) return;

    const id = parseRouteId(rawId);
    if (id === null) {
      this.handleNotFound(null);
      return;
    }

    // En edición la contraseña es opcional: vacía conserva la actual.
    this.editandoId = id;
    this.form.controls.password.setValidators(PASSWORD_LENGTH_VALIDATORS);
    this.form.controls.password.updateValueAndValidity();

    this.veterinarianService
      .getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (vet) =>
          this.form.patchValue({
            name: vet.name,
            document: vet.document,
            email: vet.email,
            specialty: vet.specialty,
            photoUrl: vet.photoUrl ?? '',
          }),
        error: (error: unknown) => this.handleNotFound(error),
      });
  }

  // El botón queda deshabilitado mientras guarda, así un doble clic no crea dos veterinarios.
  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.saving()) return;
    this.saving.set(true);

    const value = this.form.getRawValue();
    const body = {
      document: value.document.trim(),
      name: value.name.trim(),
      email: value.email.trim(),
      specialty: value.specialty.trim(),
      photoUrl: value.photoUrl.trim() || null,
    };
    const id = this.editandoId;
    const saved$ =
      id === null
        ? this.veterinarianService.create({ ...body, password: value.password })
        : this.veterinarianService.update(id, { ...body, password: value.password || null });

    saved$
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (vet) => {
          this.notifications.success(
            id === null ? 'Veterinario registrado' : 'Veterinario actualizado',
            `${vet.name} se ${id === null ? 'registró' : 'actualizó'} correctamente.`,
          );
          this.router.navigate([LIST_ROUTE]);
        },
        // 400 por campo va a cada control; otros (p. ej. 409 por documento o correo repetidos) se notifican.
        error: (error: unknown) => {
          if (applyServerErrors(this.form, error)) {
            this.notifications.warn('Revisa los campos marcados.', '');
          } else {
            this.notifications.error('No se pudo guardar al veterinario', error);
          }
        },
      });
  }

  cancelar(): void {
    this.router.navigate([LIST_ROUTE]);
  }

  private handleNotFound(error: unknown): void {
    this.notifications.error('Veterinario no encontrado', error);
    this.router.navigate([LIST_ROUTE]);
  }
}
