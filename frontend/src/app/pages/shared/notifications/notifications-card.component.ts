import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';

import { DemoNotificationsService } from '../../../services/demo/demo-notifications.service';
import { CardComponent } from '../card/card.component';
import { DemoTagComponent } from '../demo-tag/demo-tag.component';
import { injectNotificationClock } from './notification-icons';
import { NotificationItemComponent } from './notification-item.component';

/**
 * Último módulo de la barra lateral contextual (9.5, 11.9): las tres notificaciones más recientes y
 * «Mostrar todo», que abre la pestaña Notificaciones del rol. El shell lo añade tras el contenido de
 * la página y lo omite en la propia pantalla de notificaciones.
 */
@Component({
  selector: 'app-notifications-card',
  imports: [RouterLink, ButtonModule, CardComponent, DemoTagComponent, NotificationItemComponent],
  template: `
    <section app-card heading="Notificaciones">
      <app-demo-tag title-extra />
      @for (notification of notifications.recent(); track notification.id) {
        <app-notification-item [notification]="notification" [now]="now()" variant="card" />
      } @empty {
        <p class="py-1 text-[12.5px] leading-[18px] text-ink-muted">No hay avisos por ahora.</p>
      }
      <a pButton [text]="true" size="small" label="Mostrar todo" class="mt-1 -ml-2" [routerLink]="link()"></a>
    </section>
  `,
  host: { class: 'block' },
})
export class NotificationsCardComponent {
  /** Ruta de la pestaña Notificaciones del rol. */
  readonly link = input.required<string>();

  protected readonly notifications = inject(DemoNotificationsService);
  protected readonly now = injectNotificationClock();
}
