import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { ShellComponent } from '../../shared/shell/shell.component';
import { ShellConfig, ShellTab } from '../../shared/shell/shell-tabs';

const TABS: ShellTab[] = [
  { label: 'Panorama', icon: 'chart-column', link: '/administrador/dashboard' },
  { label: 'Veterinarios', icon: 'stethoscope', link: '/administrador/veterinarios' },
  { label: 'Mascotas', icon: 'paw-print', link: '/administrador/mascotas' },
  { label: 'Medicamentos', icon: 'package', link: '/administrador/medicamentos' },
  { label: 'Notificaciones', icon: 'bell', link: '/administrador/notificaciones' },
  { label: 'Perfil', icon: 'user', link: '/administrador/perfil' },
];

/** Shell del área del administrador: reutiliza el shell compartido con las pestañas del rol. */
@Component({
  selector: 'app-admin-layout',
  imports: [ShellComponent],
  template: '<app-shell [config]="shellConfig()" (logout)="cerrarSesion()" />',
})
export class AdminLayoutComponent {
  private readonly auth = inject(AuthService);

  readonly shellConfig = computed<ShellConfig>(() => {
    const user = this.auth.user();
    const admin = user?.role === 'ADMIN' ? user : null;

    return {
      tabs: TABS,
      roleLabel: 'Administrador',
      userName: admin?.name ?? '',
      userId: admin?.id ?? null,
      userPhotoUrl: null,
      profileLink: '/administrador/perfil',
    };
  });

  protected cerrarSesion(): void {
    this.auth.logout();
  }
}
