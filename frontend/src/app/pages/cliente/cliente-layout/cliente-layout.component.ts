import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { parseRouteId } from '../../../core/http/route-id';
import { ShellComponent } from '../../shared/shell/shell.component';
import { ShellConfig } from '../../shared/shell/shell-tabs';
import { ClienteContextStore } from '../cliente-context.store';
import { AuthService } from '../../../services/auth.service';

/**
 * Shell del área del cliente: configura el shell compartido con las pestañas del dueño
 * (:ownerId) y su identidad para la barra superior.
 */
@Component({
  selector: 'app-cliente-layout',
  imports: [ShellComponent],
  templateUrl: './cliente-layout.component.html',
})
export class ClienteLayoutComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly context = inject(ClienteContextStore);
  private readonly destroyRef = inject(DestroyRef);
  private readonly auth = inject(AuthService);

  /** :ownerId de la ruta; null si el id es inválido. */
  private readonly ownerId = signal<number | null>(null);

  /**
   * Config del shell. Es un computed para que los enlaces y el nombre de la barra
   * se actualicen al cargar el dueño o cambiar :ownerId.
   */
  readonly shellConfig = computed<ShellConfig>(() => {
    // Con un id inválido los enlaces apuntan a /cliente/x/... (segmento no numérico):
    // las páginas del área muestran "no encontrado" sin llamar al API, en vez de
    // tratar a 0 como si fuera un id válido.
    const id = this.ownerId() ?? 'x';
    const home = `/cliente/${id}`;
    const owner = this.context.owner();

    return {
      tabs: [
        { label: 'Inicio', icon: 'house', link: home, exact: true },
        { label: 'Mis mascotas', icon: 'paw-print', link: `${home}/mascotas` },
        { label: 'Citas', icon: 'calendar', link: `${home}/citas` },
        { label: 'Notificaciones', icon: 'bell', link: `${home}/notificaciones` },
        { label: 'Perfil', icon: 'user', link: `${home}/perfil` },
      ],
      roleLabel: 'Cliente',
      userName: owner?.name ?? '',
      userId: owner?.id ?? this.ownerId(),
      userPhotoUrl: null,
      profileLink: `${home}/perfil`,
    };
  });

  // Cierra la sesión (borra el token) y vuelve al login de dueños.
  protected logout(): void {
    this.auth.logout();
  }

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
