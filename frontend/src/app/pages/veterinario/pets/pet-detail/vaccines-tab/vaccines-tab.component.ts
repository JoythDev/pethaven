import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { ViewState } from '../../../../../core/http/view-state';
import { vaccineSeverity } from '../../../../../core/pet-detail/pet-detail';
import { skeletonVisible } from '../../../../../core/ui/skeleton-timing';
import { VaccineCard } from '../../../../../models/demo/vaccine.model';
import { CardComponent } from '../../../../shared/card/card.component';
import { DemoTagComponent } from '../../../../shared/demo-tag/demo-tag.component';
import { InlineErrorComponent } from '../../../../shared/inline-error/inline-error.component';
import { SkeletonComponent } from '../../../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../../../shared/sticker/sticker.component';

const SKELETON_CARDS = [0, 1, 2];

/**
 * Pestaña «Vacunas» (demo, S3): una tarjeta por vacuna del carné con su última dosis, la próxima y
 * la etiqueta de estado (12.3). Los datos son de ejemplo hasta que exista el backend. Con `scheduleLink`
 * (el cliente) cada vacuna que no está al día ofrece «Agendar» hacia las citas de esa mascota.
 */
@Component({
  selector: 'app-vaccines-tab',
  imports: [DatePipe, RouterLink, ButtonModule, TagModule, CardComponent, DemoTagComponent, InlineErrorComponent, SkeletonComponent, StickerComponent],
  templateUrl: './vaccines-tab.component.html',
})
export class VaccinesTabComponent {
  readonly state = input.required<ViewState<VaccineCard>>();
  /** Enlace a las citas del dueño; null si el área no agenda (veterinario, administrador). */
  readonly scheduleLink = input<(string | number)[] | null>(null);
  /** Mascota a la que se agenda: viaja en `?mascota` para que la cita la preseleccione. */
  readonly petId = input<number | null>(null);
  readonly retry = output<void>();

  protected readonly vaccineSeverity = vaccineSeverity;
  protected readonly skeletonCards = SKELETON_CARDS;
  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));

  protected readonly summary = computed(() => {
    const state = this.state();
    if (state.status !== 'ready') {
      return '';
    }
    const { upToDate, soon, overdue } = state.data.summary;
    const parts = [
      `${upToDate} ${upToDate === 1 ? 'vigente' : 'vigentes'}`,
      ...(soon ? [`${soon} por vencer`] : []),
      ...(overdue ? [`${overdue} ${overdue === 1 ? 'vencida' : 'vencidas'}`] : []),
    ];
    return parts.join(' · ');
  });
}
