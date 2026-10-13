import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  exhaustMap,
  filter,
  finalize,
  map,
  switchMap,
  tap,
} from 'rxjs';
import { Owner } from '../../../../models/owner.model';
import { OwnerService } from '../../../../services/owner.service';
import { getErrorMessage } from '../../../../utils/http-error.util';

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
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  // Con valor, el formulario edita a ese dueño; con null, registra a uno nuevo.
  editandoId: number | null = null;

  readonly saving = signal(false);

  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    document: ['', [Validators.required, Validators.maxLength(20)]],
    phone: ['', [Validators.required, Validators.maxLength(20)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: ['', [Validators.required, ...PASSWORD_LENGTH_VALIDATORS]],
  });

  // Cada emisión indica que el formulario es válido y debe guardarse en el API.
  private readonly submit$ = new Subject<void>();

  constructor() {
    // exhaustMap ignora los envíos que lleguen mientras uno sigue en curso, así un doble clic
    // en Guardar no registra al dueño dos veces. Al terminar avisa y navega: a la ficha si
    // se editó, o al listado si se creó.
    this.submit$
      .pipe(
        exhaustMap(() => this.save()),
        takeUntilDestroyed(),
      )
      .subscribe((owner) => {
        const editando = this.editandoId !== null;
        this.messageService.add({
          severity: 'success',
          summary: editando ? 'Dueño actualizado' : 'Dueño registrado',
          detail: `${owner.name} se ${editando ? 'actualizó' : 'registró'} correctamente.`,
        });
        this.router.navigate(editando ? ['/dashboard/owners', owner.id] : ['/dashboard/owners']);
      });
  }

  ngOnInit(): void {
    // Lee el :id de la URL. Si existe, pasa el formulario a modo edición, llama a
    // OwnerService.getById y rellena los campos; si el dueño no existe o falla, avisa y
    // regresa al listado. La contraseña no se rellena porque el API nunca la devuelve.
    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        tap((id) => this.setEditMode(id === null ? null : Number(id))),
        filter((id): id is string => id !== null),
        switchMap((id) =>
          this.ownerService.getById(Number(id)).pipe(
            catchError((error: unknown) => {
              this.notifyError('Dueño no encontrado', error);
              this.router.navigate(['/dashboard/owners']);
              return EMPTY;
            }),
          ),
        ),
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
    this.router.navigate(['/dashboard/owners']);
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

  // Llama a OwnerService.create o OwnerService.update según haya o no un id en la URL.
  // Si el API rechaza el guardado (por ejemplo 409 por documento o correo repetidos) muestra
  // su mensaje y termina sin emitir, para que el usuario corrija y reintente.
  private save(): Observable<Owner> {
    this.saving.set(true);

    const { name, document, email, password, phone } = this.form.getRawValue();
    const id = this.editandoId;
    const saved$ =
      id === null
        ? this.ownerService.create({ document, name, email, password, phone })
        : this.ownerService.update(id, { document, name, email, password: password || null, phone });

    return saved$.pipe(
      catchError((error: unknown) => {
        this.notifyError('No se pudo guardar al dueño', error);
        return EMPTY;
      }),
      finalize(() => this.saving.set(false)),
    );
  }

  private notifyError(summary: string, error: unknown): void {
    this.messageService.add({ severity: 'error', summary, detail: getErrorMessage(error) });
  }
}
