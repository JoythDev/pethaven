import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../../services/auth.service';
import { VeterinarianService } from '../../../services/veterinarian.service';
import { ShellComponent } from '../../shared/shell/shell.component';
import { ShellConfig, ShellTab } from '../../shared/shell/shell-tabs';

const TABS: ShellTab[] = [
  { label: 'Inicio', icon: 'house', link: '/veterinario', exact: true },
  { label: 'Agenda', icon: 'calendar', link: '/veterinario/agenda' },
  { label: 'Mascotas', icon: 'paw-print', link: '/veterinario/mascotas' },
  { label: 'Dueños', icon: 'users', link: '/veterinario/duenos' },
  { label: 'Medicamentos', icon: 'package', link: '/veterinario/medicamentos' },
  { label: 'Notificaciones', icon: 'bell', link: '/veterinario/notificaciones' },
  { label: 'Perfil', icon: 'user', link: '/veterinario/perfil' },
];

/**
 * Shell del área del veterinario: reutiliza el shell compartido con las pestañas del rol.
 * El nombre y la foto de la barra superior salen del veterinario que inició sesión.
 */
@Component({
  selector: 'app-veterinario-layout',
  imports: [ShellComponent],
  templateUrl: './veterinario-layout.component.html',
})
export class VeterinarioLayoutComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly photoUrl = signal<string | null>(null);

  // Pide al API la foto del veterinario que inició sesión; si falla, la barra muestra las iniciales.
  ngOnInit(): void {
    const user = this.auth.user();
    if (user?.role !== 'VETERINARIAN') {
      return;
    }
    this.veterinarianService
      .getById(user.id)
      .pipe(
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((vet) => this.photoUrl.set(vet?.photoUrl ?? null));
  }

  readonly shellConfig = computed<ShellConfig>(() => {
    const user = this.auth.user();
    const vet = user?.role === 'VETERINARIAN' ? user : null;

    return {
      tabs: TABS,
      roleLabel: 'Veterinario',
      userName: vet?.name ?? '',
      userId: vet?.id ?? null,
      userPhotoUrl: this.photoUrl(),
      profileLink: '/veterinario/perfil',
    };
  });

  // Borra la sesión guardada y vuelve al login del veterinario.
  protected cerrarSesion(): void {
    this.auth.logout();
  }
}
