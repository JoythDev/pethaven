import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';

import { formatRelativeTime } from '../../../core/notifications/relative-time';
import { DemoNotification } from '../../../models/demo/demo-notification.model';
import { notificationIcon } from './notification-icons';

export type NotificationItemVariant = 'bell' | 'card' | 'page';

/**
 * Una notificación en los tres lugares donde vive (11.9). `bell`: icono, título y hora. `card`: añade
 * el detalle, acotado a dos líneas porque la barra lateral mide 236 px. `page`: detalle completo, hora
 * a la derecha y marca de sin leer; con `interactive` es un solo botón que emite `activate`.
 * Reúne las tres variantes porque `app-list-item` recorta el título a una línea y aquí no cabe.
 */
@Component({
  selector: 'app-notification-item',
  imports: [NgTemplateOutlet, LucideDynamicIcon],
  templateUrl: './notification-item.component.html',
  host: { class: 'block min-w-0' },
})
export class NotificationItemComponent {
  readonly notification = input.required<DemoNotification>();
  readonly now = input.required<Date>();
  readonly variant = input<NotificationItemVariant>('bell');
  readonly interactive = input(false);
  readonly activate = output<void>();

  protected readonly icon = computed(() => notificationIcon(this.notification().icon));
  protected readonly time = computed(() => formatRelativeTime(this.notification().at, this.now()));
  protected readonly isPage = computed(() => this.variant() === 'page');
  protected readonly showDetail = computed(() => this.variant() !== 'bell');

  protected readonly rowClass = computed(
    () =>
      'flex w-full min-w-0 items-start gap-3 rounded-row text-left text-ink ' +
      (this.isPage() ? 'px-2.5 py-[9px] ' : 'px-0 py-2 ') +
      (this.interactive()
        ? 'ph-focus cursor-pointer transition-colors duration-[180ms] ease-out-strong hover:bg-sunken motion-reduce:transition-none'
        : ''),
  );
  protected readonly titleClass = computed(
    () =>
      'block text-sm leading-5 text-ink ' +
      (this.notification().unread ? 'font-extrabold' : 'font-semibold') +
      (this.isPage() ? '' : ' line-clamp-2'),
  );
  protected readonly detailClass = computed(
    () => 'block text-[12.5px] leading-[18px] text-ink-muted' + (this.isPage() ? '' : ' line-clamp-2'),
  );
}
