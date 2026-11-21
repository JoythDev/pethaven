import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideCheck } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { ToggleButtonModule } from 'primeng/togglebutton';

import { NotificationService } from '../../../core/notifications/notification.service';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { DemoNotification, NotificationGroup } from '../../../models/demo/demo-notification.model';
import { DemoNotificationsService } from '../../../services/demo/demo-notifications.service';
import { BlockComponent } from '../block/block.component';
import { CardComponent } from '../card/card.component';
import { DefinitionItem, DefinitionListComponent } from '../definition-list/definition-list.component';
import { EmptyStateComponent } from '../empty-state/empty-state.component';
import { PageHeaderComponent } from '../page-header/page-header.component';
import { ShellSidebarService } from '../shell/shell-sidebar.service';
import { SkeletonComponent } from '../skeleton/skeleton.component';
import { StickerComponent } from '../sticker/sticker.component';
import { AuthRole } from '../../../models/auth.model';
import { AuthService } from '../../../services/auth.service';
import { injectNotificationClock } from './notification-icons';
import { NotificationItemComponent } from './notification-item.component';

type NotificationFilter = 'all' | 'unread';

const GROUPS: readonly NotificationGroup[] = ['Hoy', 'Esta semana'];

// Lo que avisa cada rol (11.9); las preferencias llegarán con el perfil.
const TOPICS: Record<AuthRole, DefinitionItem[]> = {
  OWNER: [
    { term: 'Citas', value: 'Activado' },
    { term: 'Cuidados del día', value: 'Activado' },
    { term: 'Vacunas', value: 'Activado' },
  ],
  VETERINARIAN: [
    { term: 'Agenda y urgencias', value: 'Activado' },
    { term: 'Existencias', value: 'Activado' },
    { term: 'Controles pendientes', value: 'Activado' },
  ],
  ADMIN: [
    { term: 'Inventario', value: 'Activado' },
    { term: 'Ventas', value: 'Activado' },
    { term: 'Equipo', value: 'Activado' },
  ],
};

/**
 * Pestaña Notificaciones (11.9), la misma para los tres roles: el contenido lo decide el servicio
 * demo según la sesión. Lista agrupada por «Hoy» y «Esta semana» con filtro «Sin leer», resumen y
 * temas avisados a un lado, y «Marcar todo como leído». Abrir una notificación la marca como leída
 * y navega a su ruta si la tiene.
 */
@Component({
  selector: 'app-notifications-page',
  imports: [
    FormsModule,
    ButtonModule,
    ToggleButtonModule,
    LucideCheck,
    BlockComponent,
    CardComponent,
    DefinitionListComponent,
    EmptyStateComponent,
    NotificationItemComponent,
    PageHeaderComponent,
    SkeletonComponent,
    StickerComponent,
  ],
  templateUrl: './notifications-page.component.html',
})
export class NotificationsPageComponent implements OnInit {
  protected readonly notifications = inject(DemoNotificationsService);
  protected readonly now = injectNotificationClock();
  private readonly router = inject(Router);
  private readonly feedback = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly layout = inject(ShellSidebarService).layout;
  private readonly user = inject(AuthService).user;

  protected readonly filter = signal<NotificationFilter>('all');
  private readonly loading = signal(true);
  protected readonly showSkeleton = skeletonVisible(this.loading);

  // Dos columnas (8 + 4) solo en `expanded`; por debajo, una sola (9.7).
  protected readonly twoColumns = computed(() => this.layout() === 'expanded');

  protected readonly unread = this.notifications.unreadCount;
  protected readonly total = computed(() => this.notifications.notifications().length);
  protected readonly subtitle = computed(() => (this.unread() > 0 ? `${this.unread()} sin leer` : 'Todo al día'));
  protected readonly topics = computed(() => {
    const role = this.user()?.role;
    return role ? TOPICS[role] : [];
  });

  protected readonly groups = computed(() => {
    const onlyUnread = this.filter() === 'unread';
    const visible = this.notifications.notifications().filter((n) => !onlyUnread || n.unread);
    return GROUPS.map((title) => ({ title, items: visible.filter((n) => n.group === title) })).filter(
      (group) => group.items.length > 0,
    );
  });

  ngOnInit(): void {
    this.notifications
      .refresh()
      .pipe(takeUntilDestroyed(this.destroyRef))
      // Si falla, la lista queda vacía en vez de dejar el esqueleto fijo.
      .subscribe({ complete: () => this.loading.set(false), error: () => this.loading.set(false) });
  }

  protected setFilter(filter: NotificationFilter): void {
    this.filter.set(filter);
  }

  protected open(notification: DemoNotification): void {
    this.notifications.markAsRead(notification.id);
    if (notification.route) {
      void this.router.navigateByUrl(notification.route);
    }
  }

  protected markAllAsRead(): void {
    this.notifications.markAllAsRead();
    this.feedback.success('Notificaciones', 'Todo quedó marcado como leído.');
  }
}
