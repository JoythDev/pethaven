import { Component, DestroyRef, OnInit, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
export class DashboardTopbarComponent implements OnInit {
  /** Notifica al layout para abrir el drawer en móvil. */
  readonly menuToggle = output<void>();

  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly homeItem: MenuItem = { icon: 'pi pi-home', routerLink: '/veterinario' };
  readonly breadcrumbItems = signal<MenuItem[]>([]);

  readonly userMenuItems: MenuItem[] = [
    { label: 'Perfil', icon: 'pi pi-user', disabled: true },
    { label: 'Cerrar sesión', icon: 'pi pi-sign-out', disabled: true },
  ];

  private readonly pageLabels: Record<string, string> = {
    pets: 'Mascotas',
    owners: 'Dueños',
  };

  private readonly subPageLabels: Record<string, Record<string, string>> = {
    pets: { add: 'Nueva mascota', update: 'Editar mascota' },
    owners: { add: 'Nuevo dueño', update: 'Editar dueño' },
  };

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
        items.push({ label: this.subPageLabel(segment, sub) });
      }
    }
    this.breadcrumbItems.set(items);
  }

  private subPageLabel(parent: string, segment: string): string {
    const label = this.subPageLabels[parent]?.[segment];
    if (label) {
      return label;
    }
    if (/^\d+$/.test(segment)) {
      return 'Detalle';
    }
    return this.pageLabels[segment] ?? segment;
  }
}
