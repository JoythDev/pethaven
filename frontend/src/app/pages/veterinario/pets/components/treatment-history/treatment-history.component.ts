import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { ViewState } from '../../../../../core/http/view-state';
import { Treatment } from '../../../../../models/treatment.model';

/** Lista el historial de tratamientos de una mascota según el estado de la consulta. */
@Component({
  selector: 'app-treatment-history',
  imports: [DatePipe],
  templateUrl: './treatment-history.component.html',
})
export class TreatmentHistoryComponent {
  readonly state = input.required<ViewState<Treatment[]>>();
}
