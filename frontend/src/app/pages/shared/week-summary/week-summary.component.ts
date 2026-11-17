import { Component, computed, input, output } from '@angular/core';

import { ViewState } from '../../../core/http/view-state';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { WeekSummary } from '../../../core/vet-home/vet-home';
import { BlockComponent } from '../block/block.component';
import { CardComponent } from '../card/card.component';
import { DemoTagComponent } from '../demo-tag/demo-tag.component';
import { InlineErrorComponent } from '../inline-error/inline-error.component';
import { SkeletonComponent } from '../skeleton/skeleton.component';

/**
 * «Esta semana» (demo, 9.5): citas de la semana y barra de atendidas. Va en la barra lateral del
 * Inicio y de la Agenda del veterinario. Solo presenta: quien la usa calcula el estado y atiende
 * «Reintentar».
 */
@Component({
  selector: 'app-week-summary',
  imports: [BlockComponent, CardComponent, DemoTagComponent, InlineErrorComponent, SkeletonComponent],
  templateUrl: './week-summary.component.html',
  host: { class: 'block' },
})
export class WeekSummaryComponent {
  readonly state = input.required<ViewState<WeekSummary>>();
  readonly retry = output<void>();

  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));
}
