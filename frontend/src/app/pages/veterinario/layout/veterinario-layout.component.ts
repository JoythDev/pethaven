import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  AreaLayoutConfig,
  DEFAULT_AREA_LAYOUT_CONFIG,
} from '../../shared/area-layout/area-layout-config';
import { AreaLayoutComponent } from '../../shared/area-layout/area-layout.component';

/**
 * Shell del área del veterinario: reutiliza el shell compartido con la
 * configuración del panel. El menú de usuario habilita 'Cerrar sesión'.
 */
@Component({
  selector: 'app-veterinario-layout',
  imports: [AreaLayoutComponent],
  templateUrl: './veterinario-layout.component.html',
})
export class VeterinarioLayoutComponent {
  private readonly router = inject(Router);

  /** Config del panel: todo por defecto, salvo el cierre de sesión (navega a /login). */
  readonly layoutConfig: AreaLayoutConfig = {
    ...DEFAULT_AREA_LAYOUT_CONFIG,
    topbar: {
      ...DEFAULT_AREA_LAYOUT_CONFIG.topbar,
      userMenuItems: [
        { label: 'Perfil', icon: 'pi pi-user', disabled: true },
        {
          label: 'Cerrar sesión',
          icon: 'pi pi-sign-out',
          command: () => void this.router.navigate(['/login']),
        },
      ],
    },
  };
}
