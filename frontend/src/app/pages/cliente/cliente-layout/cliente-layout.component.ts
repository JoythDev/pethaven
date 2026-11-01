import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { parseRouteId } from '../../../core/http/route-id';
import { AreaLayoutConfig } from '../../shared/area-layout/area-layout-config';
import { AreaLayoutComponent } from '../../shared/area-layout/area-layout.component';
import { ClienteContextStore } from '../cliente-context.store';

/**
 * Shell del área del cliente: configura el shell compartido con los enlaces
 * del dueño (:ownerId) y su identidad para la topbar.
 */
@Component({
  selector: 'app-cliente-layout',
  imports: [AreaLayoutComponent],
  templateUrl: './cliente-layout.component.html',
})
export class ClienteLayoutComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly context = inject(ClienteContextStore);
  private readonly destroyRef = inject(DestroyRef);

  /** :ownerId de la ruta; null si el id es inválido. */
  private readonly ownerId = signal<number | null>(null);

  /**
   * Config del shell. Es un computed para que los enlaces, el nombre de la
   * topbar y el menú se actualicen al cargar el dueño o cambiar :ownerId.
   */
  readonly layoutConfig = computed<AreaLayoutConfig>(() => {
    // Con un id inválido los enlaces apuntan a /cliente/x/... (segmento no numérico):
    // las páginas del área muestran "no encontrado" sin llamar al API, en vez de
    // tratar a 0 como si fuera un id válido.
    const id = this.ownerId() ?? 'x';
    const home = `/cliente/${id}`;
    const owner = this.context.owner();

    return {
      sidebar: {
        items: [
          { label: 'Mis Mascotas', icon: 'pi pi-heart', link: `${home}/mascotas` },
          { label: 'Mi Perfil', icon: 'pi pi-user', link: `${home}/perfil` },
          {
            label: 'Cerrar sesión',
            icon: 'pi pi-sign-out',
            command: () => void this.router.navigate(['/login']),
          },
        ],
        homeLink: home,
        sectionLabel: 'Mi cuenta',
      },
      topbar: {
        homeItem: { icon: 'pi pi-home', routerLink: home },
        pageLabels: { mascotas: 'Mis Mascotas', perfil: 'Mi Perfil' },
        subPageLabels: {},
        userName: owner?.name ?? '',
        userInitial: owner ? owner.name.charAt(0).toUpperCase() : '',
        userMenuItems: [
          {
            label: 'Mi Perfil',
            icon: 'pi pi-user',
            command: () => void this.router.navigate([home, 'perfil']),
          },
          {
            label: 'Cerrar sesión',
            icon: 'pi pi-sign-out',
            command: () => void this.router.navigate(['/login']),
          },
        ],
        skipNumericSegment: true,
      },
    };
  });

  ngOnInit(): void {
    // El param puede cambiar sin recrear el layout (misma ruta, otro dueño).
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const id = parseRouteId(params.get('ownerId'));
        this.ownerId.set(id);
        if (id !== null) {
          this.context.load(id);
        }
      });
  }
}
