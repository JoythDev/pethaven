import { DOCUMENT, NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnDestroy, OnInit, computed, effect, inject, input, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  LucideBell,
  LucideCalendar,
  LucideChartColumn,
  LucideDynamicIcon,
  LucideEllipsis,
  LucideHouse,
  LucideIcon,
  LucideLogOut,
  LucideMoon,
  LucidePackage,
  LucidePawPrint,
  LucideSearch,
  LucideStethoscope,
  LucideSun,
  LucideUser,
  LucideUsers,
  provideLucideConfig,
} from '@lucide/angular';
import { filter, map } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DrawerModule } from 'primeng/drawer';
import { MenuModule } from 'primeng/menu';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';

import { ThemeService } from '../../../core/theme/theme.service';
import { DemoNotificationsService } from '../../../services/demo/demo-notifications.service';
import { AvatarComponent } from '../avatar/avatar.component';
import { GlobalSearchComponent } from '../global-search/global-search.component';
import { GlobalSearchService } from '../global-search/global-search.service';
import { NotificationsBellComponent } from '../notifications/notifications-bell.component';
import { NotificationsCardComponent } from '../notifications/notifications-card.component';
import { BrandLogoComponent } from '../brand-logo/brand-logo.component';
import { ShellSidebarService } from './shell-sidebar.service';
import { SidebarToggleComponent } from './sidebar-toggle.component';
import { ShellConfig, ShellIconName, activeTabIndex, layoutForWidth, splitBottomTabs } from './shell-tabs';

const TAB_ICONS: Record<ShellIconName, LucideIcon> = {
  house: LucideHouse,
  calendar: LucideCalendar,
  'paw-print': LucidePawPrint,
  users: LucideUsers,
  package: LucidePackage,
  bell: LucideBell,
  user: LucideUser,
  'chart-column': LucideChartColumn,
  stethoscope: LucideStethoscope,
};

const REDUCED_MOTION = '(prefers-reduced-motion: reduce)';

/**
 * Shell de los portales: barra superior fija con pestañas de sección, barra lateral contextual,
 * contenido con scroll propio y, en compacto, barra inferior. Ocupa exactamente el alto de la
 * ventana y nunca hace scroll como conjunto. Aloja también el toast y la confirmación globales.
 */
@Component({
  selector: 'app-shell',
  imports: [
    NgTemplateOutlet,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    ConfirmDialogModule,
    DrawerModule,
    MenuModule,
    ToastModule,
    TooltipModule,
    AvatarComponent,
    GlobalSearchComponent,
    NotificationsBellComponent,
    NotificationsCardComponent,
    SidebarToggleComponent,
    LucideBell,
    LucideDynamicIcon,
    LucideEllipsis,
    LucideLogOut,
    LucideMoon,
    BrandLogoComponent,
    LucideSearch,
    LucideSun,
    LucideUser,
  ],
  templateUrl: './shell.component.html',
  // Iconos del sistema de diseño (18 px, trazo 1,8) para todo el portal. Se declara aquí y no en
  // app.config para que Lucide no entre en el paquete inicial.
  providers: [provideLucideConfig({ size: 18, strokeWidth: 1.8 })],
  host: { class: '@container block h-dvh overflow-clip' },
})
export class ShellComponent implements OnInit, OnDestroy {
  readonly config = input.required<ShellConfig>();
  readonly logout = output<void>();

  protected readonly theme = inject(ThemeService);
  protected readonly sidebar = inject(ShellSidebarService);
  protected readonly notifications = inject(DemoNotificationsService);
  private readonly search = inject(GlobalSearchService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  private resizeObserver?: ResizeObserver;

  /** Con movimiento reducido las transiciones de menú, cajón y hoja son instantáneas (7). */
  protected readonly transition = window.matchMedia(REDUCED_MOTION).matches
    ? '0ms'
    : '180ms cubic-bezier(0.23, 1, 0.32, 1)';

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  protected readonly icons = TAB_ICONS;

  protected readonly tabs = computed(() => this.config().tabs);
  protected readonly homeLink = computed(() => this.tabs()[0]?.link ?? '/');
  protected readonly activeIndex = computed(() => activeTabIndex(this.tabs(), this.url()));
  protected readonly bottom = computed(() => splitBottomTabs(this.tabs()));
  protected readonly moreActive = computed(
    () => this.bottom().overflow.length > 0 && this.activeIndex() >= this.bottom().visible.length,
  );

  /** Pestaña con foco de teclado (roving): la que tiene el foco o, si no, la activa. */
  protected readonly focusIndex = signal<number | null>(null);
  protected readonly tabStop = computed(() => this.focusIndex() ?? Math.max(this.activeIndex(), 0));

  /** Pestaña Notificaciones del rol (la del icono de campana) y si la pantalla actual es esa. */
  protected readonly notificationsLink = computed(() => this.tabs().find((tab) => tab.icon === 'bell')?.link ?? null);
  private readonly onNotifications = computed(() => this.tabs()[this.activeIndex()]?.icon === 'bell');
  // El bloque «Notificaciones» cierra la barra lateral (9.5), salvo en su propia pantalla. Solo se suma
  // a una barra que la página ya aporta: sin módulos propios, la barra se omite (decisión del usuario).
  protected readonly showNotificationsCard = computed(
    () => this.notificationsLink() !== null && !this.onNotifications(),
  );

  protected readonly showColumn = computed(
    () => this.sidebar.layout() === 'expanded' && this.sidebar.hasContent() && !this.sidebar.collapsed(),
  );
  protected readonly showDrawer = computed(() => this.sidebar.layout() !== 'expanded' && this.sidebar.hasContent());

  protected readonly moreOpen = signal(false);
  protected readonly menuOpen = signal(false);

  protected readonly themeLabel = computed(() =>
    this.theme.isDark() ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro',
  );
  protected readonly userKey = computed(() => this.config().userId ?? this.config().userName);

  protected readonly userButton = viewChild<ElementRef<HTMLButtonElement>>('userButton');

  protected readonly menuItems = computed<MenuItem[]>(() => [
    // En compacto la búsqueda y las notificaciones viven en el menú de usuario (9.9).
    ...(this.sidebar.layout() === 'compact'
      ? [
          {
            id: 'search',
            label: 'Buscar',
            command: () => this.search.openOverlay(this.userButton()?.nativeElement ?? null),
          },
          {
            id: 'notifications',
            label: 'Notificaciones',
            command: () => {
              const link = this.notificationsLink();
              if (link) {
                void this.router.navigateByUrl(link);
              }
            },
          },
        ]
      : []),
    { id: 'profile', label: 'Perfil', command: () => void this.router.navigateByUrl(this.config().profileLink) },
    {
      id: 'theme',
      label: this.theme.isDark() ? 'Tema claro' : 'Tema oscuro',
      command: () => this.theme.toggle(),
    },
    { separator: true },
    { id: 'logout', label: 'Cerrar sesión', command: () => this.logout.emit() },
  ]);

  constructor() {
    effect(() => {
      // Navegar cierra el cajón y la hoja «Más»: los ítems de la barra lateral son atajos.
      this.url();
      this.sidebar.drawerOpen.set(false);
      this.moreOpen.set(false);
    });
    effect(() => {
      const layout = this.sidebar.layout();
      if (layout === 'expanded') {
        this.sidebar.drawerOpen.set(false);
      }
      if (layout !== 'compact') {
        this.moreOpen.set(false);
      }
    });
  }

  ngOnInit(): void {
    // Los avisos demo se generan al entrar al portal; la pantalla de notificaciones los vuelve a pedir.
    this.notifications.refresh().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();

    // Scrollbars propios mientras haya un portal en pantalla (4.5).
    this.document.documentElement.classList.add('ph-scrollbars');

    const element = this.host.nativeElement;
    this.sidebar.layout.set(layoutForWidth(element.getBoundingClientRect().width || window.innerWidth));
    this.resizeObserver = new ResizeObserver(([entry]) => {
      this.sidebar.layout.set(layoutForWidth(entry.contentRect.width));
    });
    this.resizeObserver.observe(element);
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.document.documentElement.classList.remove('ph-scrollbars');
    this.sidebar.drawerOpen.set(false);
  }

  /** Flechas, Inicio y Fin mueven el foco entre las pestañas; Enter activa el enlace enfocado. */
  protected onTabsKeydown(event: KeyboardEvent): void {
    const links = Array.from((event.currentTarget as HTMLElement).querySelectorAll<HTMLAnchorElement>('a'));
    const current = links.indexOf(this.document.activeElement as HTMLAnchorElement);
    const last = links.length - 1;
    let target = -1;
    if (event.key === 'ArrowRight') {
      target = current >= last ? 0 : current + 1;
    } else if (event.key === 'ArrowLeft') {
      target = current <= 0 ? last : current - 1;
    } else if (event.key === 'Home') {
      target = 0;
    } else if (event.key === 'End') {
      target = last;
    }
    if (target >= 0) {
      event.preventDefault();
      links[target]?.focus();
    }
  }

  protected onTabsFocusOut(event: FocusEvent): void {
    if (!(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node | null)) {
      this.focusIndex.set(null);
    }
  }
}
