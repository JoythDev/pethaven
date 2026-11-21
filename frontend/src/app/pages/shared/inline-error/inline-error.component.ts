import { Component, input, output } from '@angular/core';
import { LucideCircleAlert } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';

/** Aviso en línea cuando un bloque no pudo cargar sus datos (11.8): qué pasó y «Reintentar». */
@Component({
  selector: 'app-inline-error',
  imports: [ButtonModule, MessageModule, LucideCircleAlert],
  template: `
    <p-message severity="error" styleClass="w-full">
      <ng-template #icon><svg lucideCircleAlert class="shrink-0" [size]="18" aria-hidden="true"></svg></ng-template>
      <div class="flex flex-wrap items-center gap-x-4 gap-y-2">
        <span>{{ text() }}</span>
        <button
          pButton
          type="button"
          size="small"
          [outlined]="true"
          severity="danger"
          label="Reintentar"
          (click)="retry.emit()"
        ></button>
      </div>
    </p-message>
  `,
  host: { class: 'block' },
})
export class InlineErrorComponent {
  readonly text = input.required<string>();
  readonly retry = output<void>();
}
