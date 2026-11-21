import { Component, inject, input, signal } from '@angular/core';
import { LucideLogOut } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { Observable } from 'rxjs';

import { AuthService } from '../../../services/auth.service';
import { CardComponent } from '../card/card.component';
import { ProfilePasswordDialogComponent } from './profile-password-dialog.component';

/**
 * Tarjeta «Seguridad»: fila «Contraseña» con «Cambiar contraseña» (abre el diálogo) y fila «Sesión»
 * con «Cerrar sesión». Es la misma para los tres roles; cada página pasa en `savePassword` cómo se
 * guarda la contraseña nueva. Sin `savePassword` la fila de contraseña no se muestra.
 */
@Component({
  selector: 'app-profile-security-card',
  imports: [ButtonModule, CardComponent, LucideLogOut, ProfilePasswordDialogComponent],
  template: `
    <section app-card heading="Seguridad">
      @if (savePassword(); as save) {
        <div class="flex flex-wrap items-center gap-x-3 gap-y-2">
          <div class="min-w-0 flex-1 basis-[220px]">
            <p class="text-sm leading-5 font-extrabold text-ink">Contraseña</p>
            <p class="text-[12.5px] leading-[18px] text-ink-muted">Elige una nueva cuando quieras.</p>
          </div>
          <button pButton id="btnCambiarContrasena" type="button" severity="secondary" [outlined]="true" size="small" label="Cambiar contraseña" (click)="changing.set(true)"></button>
        </div>
        <hr class="my-3 border-0 border-t border-line" />

        @if (changing()) {
          <app-profile-password-dialog [save]="save" (closed)="changing.set(false)" />
        }
      }

      <div class="flex flex-wrap items-center gap-x-3 gap-y-2">
        <div class="min-w-0 flex-1 basis-[220px]">
          <p class="text-sm leading-5 font-extrabold text-ink">Sesión</p>
          <p class="text-[12.5px] leading-[18px] text-ink-muted">Cierra la sesión en este dispositivo.</p>
        </div>
        <button pButton id="btnCerrarSesion" type="button" severity="secondary" [outlined]="true" size="small" (click)="auth.logout()">
          <svg lucideLogOut [size]="18" aria-hidden="true"></svg>
          <span class="p-button-label">Cerrar sesión</span>
        </button>
      </div>
    </section>
  `,
  host: { class: 'block' },
})
export class ProfileSecurityCardComponent {
  protected readonly auth = inject(AuthService);

  /** Guarda la contraseña nueva con el servicio del rol; sin ella no se ofrece el cambio. */
  readonly savePassword = input<((password: string) => Observable<unknown>) | null>(null);

  protected readonly changing = signal(false);
}
