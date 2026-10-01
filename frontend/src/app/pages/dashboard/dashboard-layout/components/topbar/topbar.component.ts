import { Component, inject, output, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

@Component({
  selector: 'app-dashboard-topbar',
  imports: [AvatarModule, BreadcrumbModule, ButtonModule, MenuModule],
  templateUrl: './topbar.component.html',
})
export class DashboardTopbarComponent {
  /** Notifica al layout para abrir el drawer en móvil. */
  readonly menuToggle = output<void>();

  private readonly router = inject(Router);

  readonly homeItem: MenuItem = { icon: 'pi pi-home', routerLink: '/dashboard' };
  readonly breadcrumbItems = signal<MenuItem[]>([]);

  readonly userMenuItems: MenuItem[] = [
    { label: 'Perfil', icon: 'pi pi-user', disabled: true },
    { label: 'Cerrar sesión', icon: 'pi pi-sign-out', disabled: true },
  ];

  private readonly pageLabels: Record<string, string> = {
    pets: 'Mascotas',
  };

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.updateBreadcrumb(event.urlAfterRedirects));
    this.updateBreadcrumb(this.router.url);
  }

  private updateBreadcrumb(url: string): void {
    const parts = url.split('?')[0].split('/').filter(Boolean);
    const items: MenuItem[] = [];
    const segment = parts[1];
    if (!segment) {
      items.push({ label: 'Inicio' });
    } else {
      items.push({ label: this.pageLabels[segment] ?? segment });
      const sub = parts[2];
      if (sub) {
        items.push({ label: this.subPageLabel(sub) });
      }
    }
    this.breadcrumbItems.set(items);
  }

  private subPageLabel(segment: string): string {
    if (segment === 'add') {
      return 'Nueva mascota';
    }
    if (segment === 'update') {
      return 'Editar mascota';
    }
    if (/^\d+$/.test(segment)) {
      return 'Detalle';
    }
    return this.pageLabels[segment] ?? segment;
  }
}
