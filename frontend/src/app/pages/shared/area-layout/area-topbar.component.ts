import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import {
  DEFAULT_HOME_ITEM,
  DEFAULT_PAGE_LABELS,
  DEFAULT_SUB_PAGE_LABELS,
  DEFAULT_USER_MENU_ITEMS,
} from './area-layout-config';

/** Barra superior de un área: breadcrumb, usuario y menú. Configurable y por defecto veterinario. */
@Component({
  selector: 'app-area-topbar',
  imports: [AvatarModule, BreadcrumbModule, ButtonModule, MenuModule],
  templateUrl: './area-topbar.component.html',
})
export class AreaTopbarComponent implements OnInit {
  /** Notifica al layout para abrir el drawer en móvil. */
  readonly menuToggle = output<void>();

  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly homeItem = input<MenuItem>(DEFAULT_HOME_ITEM);
  readonly pageLabels = input<Record<string, string>>(DEFAULT_PAGE_LABELS);
  readonly subPageLabels = input<Record<string, Record<string, string>>>(DEFAULT_SUB_PAGE_LABELS);
  readonly userName = input<string>('Admin');
  readonly userInitial = input<string>('A');
  readonly userMenuItems = input<MenuItem[]>(DEFAULT_USER_MENU_ITEMS);
  /** En el cliente el id del dueño va antes de la página (/cliente/7/mascotas). */
  readonly skipNumericSegment = input<boolean>(false);

  readonly breadcrumbItems = signal<MenuItem[]>([]);

  ngOnInit(): void {
    // Se suscribe en ngOnInit, no en el constructor, para que la suscripción se libere con la vista.
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((event) => this.updateBreadcrumb(event.urlAfterRedirects));
    this.updateBreadcrumb(this.router.url);
  }

  /** parts[1] es la página y parts[2] la subpágina. */
  private updateBreadcrumb(url: string): void {
    const parts = url.split('?')[0].split('/').filter(Boolean);
    // Con skipNumericSegment, parts[1] puede ser el id del área y no una página.
    if (this.skipNumericSegment() && /^\d+$/.test(parts[1] ?? '')) {
      parts.splice(1, 1);
    }
    const items: MenuItem[] = [];
    const segment = parts[1];
    if (!segment) {
      items.push({ label: 'Inicio' });
    } else {
      items.push({ label: this.pageLabels()[segment] ?? segment });
      const sub = parts[2];
      if (sub) {
        items.push({ label: this.subPageLabel(segment, sub) });
      }
    }
    this.breadcrumbItems.set(items);
  }

  private subPageLabel(parent: string, segment: string): string {
    const label = this.subPageLabels()[parent]?.[segment];
    if (label) {
      return label;
    }
    // Los segmentos numéricos son ids de detalle.
    if (/^\d+$/.test(segment)) {
      return 'Detalle';
    }
    return this.pageLabels()[segment] ?? segment;
  }
}
