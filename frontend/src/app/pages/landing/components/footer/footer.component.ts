import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Footer de la landing. Portado de `fragments.html` (`th:fragment="footer"`).
 *
 * Cambios respecto al original:
 * - Los enlaces de "Portales" usan `routerLink` hacia las rutas `/login` y
 *   `/vet/login`.
 * - Facebook y TikTok quedaron deshabilitados (`aria-disabled`, `title`):
 *   nunca tuvieron URL real y se eliminó el modal WIP que los interceptaba.
 */
@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  templateUrl: './footer.component.html'
})
export class FooterComponent {}
