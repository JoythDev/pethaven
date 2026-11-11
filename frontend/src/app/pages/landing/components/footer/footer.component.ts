import { Component } from '@angular/core';

/**
 * Footer de la landing. Portado de `fragments.html` (`th:fragment="footer"`).
 *
 * Cambios respecto al original:
 * - Facebook y TikTok quedaron deshabilitados (`aria-disabled`, `title`):
 *   nunca tuvieron URL real y se eliminó el modal WIP que los interceptaba.
 */
@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html'
})
export class FooterComponent {}
