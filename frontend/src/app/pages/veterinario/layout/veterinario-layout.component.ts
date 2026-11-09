import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../../services/auth.service';
import { VeterinarianService } from '../../../services/veterinarian.service';
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
export class VeterinarioLayoutComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly photoUrl = signal<string | null>(null);

  // Pide al API la foto del veterinario que inició sesión; si falla, la topbar muestra la inicial.
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
        userPhotoUrl: this.photoUrl(),
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
    void this.router.navigate(['/login']);
  }
}
