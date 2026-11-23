import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideCircleAlert, LucideInfo, LucideTriangleAlert } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TextareaModule } from 'primeng/textarea';
import { BehaviorSubject, Observable, combineLatest, finalize, of, switchMap } from 'rxjs';

import { getErrorMessage } from '../../../../core/http/http-error';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import {
  CLINICAL_NOTES_MAX,
  OUTCOME_LABELS,
  completionKind,
  completionRequest,
  notesRequired,
  reasonLabel,
} from '../../../../core/vet-appointments/vet-appointments';
import { Appointment, AppointmentOutcome } from '../../../../models/appointment.model';
import { Pet, isHospitalized } from '../../../../models/pet.model';
import { AppointmentService } from '../../../../services/appointment.service';
import { PetService } from '../../../../services/pet.service';
import { FormFieldComponent } from '../../../shared/form-field/form-field.component';
import { InlineErrorComponent } from '../../../shared/inline-error/inline-error.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';

const OUTCOME_OPTIONS = (['AMBULATORY', 'ADMISSION'] as const).map((value) => ({ value, label: OUTCOME_LABELS[value] }));

/**
 * «Completar» una cita en curso (S16.9), con el formulario que pide su motivo:
 * - Cita del dueño: ambulatoria (nota clínica obligatoria) o ingreso. El ingreso hospitaliza a la mascota
 *   y abre el formulario de tratamiento con `?cita=`; la cita se completa al guardar el tratamiento.
 * - Alta y entrega: exige el alta médica (se puede dar aquí mismo) y deja a la mascota en su casa.
 * - Reunión informativa: nota clínica obligatoria.
 * Los fallos del API quedan como alerta dentro del diálogo (10.20).
 */
@Component({
  selector: 'app-complete-dialog',
  imports: [
    FormsModule,
    ButtonModule,
    DialogModule,
    MessageModule,
    SelectButtonModule,
    TextareaModule,
    LucideCircleAlert,
    LucideInfo,
    LucideTriangleAlert,
    FormFieldComponent,
    InlineErrorComponent,
    SkeletonComponent,
  ],
  templateUrl: './complete-dialog.component.html',
})
export class CompleteDialogComponent {
  private readonly appointmentService = inject(AppointmentService);
  private readonly petService = inject(PetService);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly appointment = input.required<Appointment>();
  readonly veterinarianId = input.required<number>();

  readonly saved = output<Appointment>();
  readonly closed = output<void>();

  protected readonly outcomeOptions = OUTCOME_OPTIONS;
  protected readonly notesMax = CLINICAL_NOTES_MAX;

  protected readonly kind = computed(() => completionKind(this.appointment()));
  protected readonly title = computed(() => `Completar la cita de ${this.appointment().petName}`);
  protected readonly reason = computed(() => reasonLabel(this.appointment()));

  protected readonly outcome = signal<AppointmentOutcome | null>(null);
  protected readonly notes = signal('');
  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly discharging = signal(false);
  protected readonly saveError = signal<string | null>(null);

  private readonly petRetry = new BehaviorSubject<void>(undefined);
  // La mascota al día: el alta exige su alta médica y el ingreso la hospitaliza si aún no lo está.
  protected readonly pet = toSignal(
    combineLatest([toObservable(this.appointment), this.petRetry]).pipe(
      switchMap(([appointment]) =>
        appointment.petId === null
          ? of<ViewState<Pet | null>>({ status: 'ready', data: null })
          : this.petService.getById(appointment.petId).pipe(toViewState<Pet | null>()),
      ),
    ),
    { initialValue: { status: 'loading' } as ViewState<Pet | null> },
  );
  protected readonly loading = computed(() => this.pet().status === 'loading');
  protected readonly skeleton = skeletonVisible(this.loading);
  private readonly loadedPet = computed(() => {
    const state = this.pet();
    return state.status === 'ready' ? state.data : null;
  });
  // Alta y entrega sin alta médica: hay que darla antes de entregar la mascota.
  protected readonly pendingDischarge = computed(() => this.kind() === 'discharge' && !!this.loadedPet()?.disease);

  protected readonly requiresNotes = computed(() => notesRequired(this.kind(), this.outcome()));
  protected readonly showNotes = computed(() => this.kind() !== 'consultation' || this.outcome() === 'AMBULATORY');
  protected readonly notesHint = computed(() =>
    this.kind() === 'briefing'
      ? 'Lo que hablaron sobre la evolución. La ven el dueño y el veterinario.'
      : 'Diagnóstico e indicaciones. La ven el dueño y el veterinario.',
  );
  protected readonly outcomeError = computed(() =>
    this.submitted() && this.kind() === 'consultation' && !this.outcome() ? 'Indica cómo terminó la consulta.' : null,
  );
  protected readonly notesError = computed(() => {
    if (!this.submitted() || !this.showNotes()) {
      return null;
    }
    if (this.requiresNotes() && !this.notes().trim()) {
      return 'Escribe la nota clínica.';
    }
    return this.notes().length > CLINICAL_NOTES_MAX ? `Máximo ${CLINICAL_NOTES_MAX} caracteres.` : null;
  });
  protected readonly confirmLabel = computed(() => {
    if (this.kind() === 'discharge') {
      return 'Completar y entregar';
    }
    return this.outcome() === 'ADMISSION' ? 'Hospitalizar y registrar tratamiento' : 'Completar cita';
  });

  protected valueOf(event: Event): string {
    return (event.target as HTMLTextAreaElement).value;
  }

  protected retryPet(): void {
    this.petRetry.next();
  }

  // Alta médica desde el diálogo: el mismo endpoint y la misma confirmación que «Dar de alta» del
  // formulario de la mascota, porque queda en su historia clínica.
  protected discharge(): void {
    const pet = this.loadedPet();
    if (!pet || this.discharging()) {
      return;
    }
    this.confirmation.confirm({
      header: 'Dar de alta',
      message: `${pet.name} dejará de estar en tratamiento por «${pet.disease}». Quedará estable y el alta se registra en su historia clínica.`,
      acceptLabel: 'Dar de alta',
      rejectLabel: 'Cancelar',
      rejectButtonProps: { outlined: true, severity: 'secondary' },
      accept: () => this.doDischarge(pet),
    });
  }

  private doDischarge(pet: Pet): void {
    this.saveError.set(null);
    this.discharging.set(true);
    this.petService
      .discharge(pet.id, this.veterinarianId())
      .pipe(
        finalize(() => this.discharging.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Alta registrada', `${pet.name} quedó estable.`);
          this.petRetry.next();
        },
        error: (error: unknown) => this.saveError.set(getErrorMessage(error, 'No se pudo dar el alta médica.')),
      });
  }

  protected confirm(): void {
    this.submitted.set(true);
    this.saveError.set(null);
    if (this.outcomeError() || this.notesError() || this.pendingDischarge() || this.saving()) {
      return;
    }
    if (this.outcome() === 'ADMISSION') {
      this.admit();
      return;
    }
    const appointment = this.appointment();
    this.run(this.appointmentService.complete(appointment.id, completionRequest(this.kind(), this.notes())), (result) => {
      const detail =
        this.kind() === 'discharge'
          ? `${result.petName} volvió a casa con ${result.ownerName}.`
          : `La cita de ${result.petName} quedó completada.`;
      this.notifications.success('Cita completada', detail);
      this.saved.emit(result);
      this.closed.emit();
    });
  }

  // Ingreso: hospitaliza a la mascota (si aún no lo está) y abre el formulario de tratamiento enlazado a la cita.
  private admit(): void {
    const appointment = this.appointment();
    const pet = this.loadedPet();
    if (!pet) {
      return;
    }
    const hospitalized$: Observable<Pet> = isHospitalized(pet) ? of(pet) : this.petService.setActive(pet.id, true);
    this.run(hospitalized$, () => {
      this.closed.emit();
      void this.router.navigate(['/veterinario/mascotas', pet.id, 'treatments', 'add'], {
        queryParams: { cita: appointment.id },
      });
    });
  }

  private run<T>(request$: Observable<T>, done: (value: T) => void): void {
    this.saving.set(true);
    request$
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: done,
        error: (error: unknown) => this.saveError.set(getErrorMessage(error, 'No se pudo completar la cita.')),
      });
  }
}
