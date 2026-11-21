import { Component, ElementRef, computed, inject, input, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { LucideBell } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DemoNotificationsService } from '../../../services/demo/demo-notifications.service';
import { DemoTagComponent } from '../demo-tag/demo-tag.component';
import { PopoverDismissDirective, PopoverDismissReason } from '../shell/popover-dismiss.directive';
import { injectNotificationClock } from './notification-icons';
import { NotificationItemComponent } from './notification-item.component';

/**
 * Campana de la barra superior (9.3, 11.9): botón con punto mientras haya avisos sin leer y un
 * popover con las tres notificaciones más recientes y «Mostrar todo». Escape y un clic fuera lo
 * cierran; Escape devuelve el foco a la campana.
 */
@Component({
  selector: 'app-notifications-bell',
  imports: [
    ButtonModule,
    TooltipModule,
    LucideBell,
    DemoTagComponent,
    NotificationItemComponent,
    PopoverDismissDirective,
  ],
  templateUrl: './notifications-bell.component.html',
  host: { class: 'relative block' },
})
export class NotificationsBellComponent {
  /** Ruta de la pestaña Notificaciones del rol. */
  readonly link = input.required<string>();

  protected readonly notifications = inject(DemoNotificationsService);
  protected readonly now = injectNotificationClock();
  private readonly router = inject(Router);

  protected readonly open = signal(false);
  private readonly trigger = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');

  protected readonly label = computed(() => {
    const unread = this.notifications.unreadCount();
    return unread > 0 ? `Notificaciones, ${unread} sin leer` : 'Notificaciones';
  });

  protected dismiss(reason: PopoverDismissReason): void {
    this.open.set(false);
    if (reason === 'escape') {
      this.trigger().nativeElement.focus();
    }
  }

  protected showAll(): void {
    this.open.set(false);
    void this.router.navigateByUrl(this.link());
  }
}
