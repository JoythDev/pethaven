import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { EMPTY, Observable, Subject, catchError, exhaustMap, finalize, map, switchMap } from 'rxjs';
import { Owner } from '../../../../models/owner.model';
import { OwnerService } from '../../../../services/owner.service';
import { applyServerErrors } from '../../../../core/http/http-error';
import { parseRouteId } from '../../../../core/http/route-id';
import { NotificationService } from '../../../../core/notifications/notification.service';

// Longitud permitida de la contraseña (la misma que valida el API).
const PASSWORD_LENGTH_VALIDATORS = [Validators.minLength(8), Validators.maxLength(255)];

@Component({
  selector: 'app-owners-form',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, MessageModule],
  templateUrl: './owner-form.component.html',
})
export class OwnersFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ownerService = inject(OwnerService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  // Con valor, el formulario edita a ese dueño; con null, registra a uno nuevo.
  editandoId: number | null = null;

  readonly saving = signal(false);

  // Pattern /\S/ rechaza textos solo con espacios, que el API rechaza con @NotBlank.
  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    document: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(20)]],
    phone: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(20)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: ['', [Validators.required, ...PASSWORD_LENGTH_VALIDATORS]],
  });

  // Cada emisión indica que el formulario es válido y debe guardarse en el API.
  private readonly submit$ = new Subject<void>();

  ngOnInit(): void {
    // Las suscripciones se crean aquí y no en el constructor: así no hay lógica al construir
    // el componente y takeUntilDestroyed corta todo al destruirlo.

    // exhaustMap ignora los envíos que lleguen mientras uno sigue en curso, así un doble clic
    // en Guardar no registra al dueño dos veces. Al terminar avisa y navega: a la ficha si
    // se editó, o al listado si se creó.
    this.submit$
      .pipe(
        exhaustMap(() => this.save()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((owner) => {
        const editando = this.editandoId !== null;
        this.notifications.success(
          editando ? 'Dueño actualizado' : 'Dueño registrado',
          `${owner.name} se ${editando ? 'actualizó' : 'registró'} correctamente.`,
        );
        this.router.navigate(editando ? ['/veterinario/owners', owner.id] : ['/veterinario/owners']);
      });

    // Lee el :id de la URL. Sin id es un alta. Con id, lo valida antes de llamar al API;
    // si es inválido o el dueño no existe, avisa y regresa al listado. La contraseña no se
    // rellena porque el API nunca la devuelve.
    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        switchMap((rawId) => {
          if (rawId === null) {
            this.setEditMode(null);
            return EMPTY;
          }

          // Un id inválido se trata como no encontrado; no se envía una petición con NaN.
          const id = parseRouteId(rawId);
          if (id === null) {
            this.handleNotFound(null);
            return EMPTY;
          }

          this.setEditMode(id);
          return this.ownerService.getById(id).pipe(
            catchError((error: unknown) => {
              this.handleNotFound(error);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((owner) =>
        this.form.patchValue({
          name: owner.name,
          document: owner.document,
          phone: owner.phone,
          email: owner.email,
        }),
      );
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
    this.router.navigate(['/veterinario/owners']);
  }

  // Guarda el id en edición y, en ese modo, vuelve opcional la contraseña: vacía significa
  // "conservar la actual", pero si se escribe una debe cumplir la longitud del API.
  private setEditMode(id: number | null): void {
    this.editandoId = id;
    if (id !== null) {
      const password = this.form.controls.password;
      password.setValidators(PASSWORD_LENGTH_VALIDATORS);
      password.updateValueAndValidity();
    }
  }

  // Dueño inexistente o id inválido: mismo aviso y regreso al listado.
  private handleNotFound(error: unknown): void {
    this.notifications.error('Dueño no encontrado', error);
    this.router.navigate(['/veterinario/owners']);
  }

  // Llama a OwnerService.create o OwnerService.update según haya o no un id en la URL.
  // Los textos libres se recortan antes de enviarse; la contraseña no se modifica.
  // Si el API responde 400 por campo, el error va a cada control y el usuario se queda en el
  // formulario. Otros errores (p. ej. 409 por documento o correo repetidos) muestran su
  // mensaje y terminan sin emitir, para que el usuario corrija y reintente.
  private save(): Observable<Owner> {
    this.saving.set(true);

    const value = this.form.getRawValue();
    const body = {
      document: value.document.trim(),
      name: value.name.trim(),
      email: value.email.trim(),
      phone: value.phone.trim(),
    };
    const id = this.editandoId;
    const saved$ =
      id === null
        ? this.ownerService.create({ ...body, password: value.password })
        : this.ownerService.update(id, { ...body, password: value.password || null });

    return saved$.pipe(
      catchError((error: unknown) => {
        if (applyServerErrors(this.form, error)) {
          this.notifications.warn('Revisa los campos marcados.', '');
        } else {
          this.notifications.error('No se pudo guardar al dueño', error);
        }
        return EMPTY;
      }),
      finalize(() => this.saving.set(false)),
    );
  }
}
