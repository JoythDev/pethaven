import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import {
  AreaLayoutConfig,
  DEFAULT_AREA_LAYOUT_CONFIG,
  DEFAULT_NAV_ITEMS,
  DEFAULT_PAGE_LABELS,
} from '../../shared/area-layout/area-layout-config';
import { AreaLayoutComponent } from '../../shared/area-layout/area-layout.component';

/**
 * Shell del área del veterinario: reutiliza el shell compartido con la
 * configuración del panel. El nombre de la topbar sale del veterinario que inició sesión.
 */
@Component({
  selector: 'app-veterinario-layout',
  imports: [AreaLayoutComponent],
  templateUrl: './veterinario-layout.component.html',
})
export class VeterinarioLayoutComponent {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  /** Config del panel con el perfil y el cierre de sesión del veterinario. */
  readonly layoutConfig = computed<AreaLayoutConfig>(() => {
    const user = this.auth.user();
    const vet = user?.role === 'VETERINARIAN' ? user : null;

    return {
      sidebar: {
        ...DEFAULT_AREA_LAYOUT_CONFIG.sidebar,
        items: [
          ...DEFAULT_NAV_ITEMS,
          { label: 'Mi Perfil', icon: 'pi pi-user', link: '/veterinario/perfil' },
          { label: 'Cerrar sesión', icon: 'pi pi-sign-out', command: () => this.cerrarSesion() },
        ],
      },
      topbar: {
        ...DEFAULT_AREA_LAYOUT_CONFIG.topbar,
        pageLabels: { ...DEFAULT_PAGE_LABELS, perfil: 'Mi Perfil' },
        userName: vet?.name ?? '',
        userInitial: vet ? vet.name.charAt(0).toUpperCase() : '',
        userMenuItems: [
          {
            label: 'Mi Perfil',
            icon: 'pi pi-user',
            command: () => void this.router.navigate(['/veterinario/perfil']),
          },
          { label: 'Cerrar sesión', icon: 'pi pi-sign-out', command: () => this.cerrarSesion() },
        ],
      },
    };
  });

  // Borra la sesión guardada y vuelve al login del veterinario.
  private cerrarSesion(): void {
    this.auth.logout();
    void this.router.navigate(['/vet/login']);
  }
}
