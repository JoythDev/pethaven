import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { AreaLayoutConfig } from '../../shared/area-layout/area-layout-config';
import { AreaLayoutComponent } from '../../shared/area-layout/area-layout.component';

/** Shell del área del administrador: reutiliza el shell compartido con sus enlaces. */
@Component({
  selector: 'app-admin-layout',
  imports: [AreaLayoutComponent],
  template: '<app-area-layout [layoutConfig]="layoutConfig()" />',
})
export class AdminLayoutComponent {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly layoutConfig = computed<AreaLayoutConfig>(() => {
    const user = this.auth.user();
    const admin = user?.role === 'ADMIN' ? user : null;
    const cerrarSesion = { label: 'Cerrar sesión', icon: 'pi pi-sign-out', command: () => this.cerrarSesion() };

    return {
      sidebar: {
        items: [
          { label: 'Veterinarios', icon: '/images/icons/stethoscope-icon.svg', link: '/administrador/veterinarios' },
          { label: 'Mascotas', icon: '/images/icons/paw-print-icon.svg', link: '/administrador/mascotas' },
          cerrarSesion,
        ],
        homeLink: '/administrador',
        sectionLabel: 'Administración',
      },
      topbar: {
        homeItem: { icon: 'pi pi-home', routerLink: '/administrador' },
        pageLabels: { veterinarios: 'Veterinarios', mascotas: 'Mascotas' },
        subPageLabels: { veterinarios: { add: 'Nuevo veterinario', update: 'Editar veterinario' } },
        userName: admin?.name ?? '',
        userInitial: admin ? admin.name.charAt(0).toUpperCase() : '',
        userMenuItems: [cerrarSesion],
        skipNumericSegment: false,
      },
    };
  });

  private cerrarSesion(): void {
    this.auth.logout();
    void this.router.navigate(['/admin/login']);
  }
}
