import { Component, HostListener, signal } from '@angular/core';

/**
 * Botón flotante que aparece al bajar de la primera pantalla y vuelve al inicio
 * de la landing con desplazamiento suave.
 */
@Component({
  selector: 'app-back-to-top',
  templateUrl: './back-to-top.component.html'
})
export class BackToTopComponent {
  readonly visible = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    this.visible.set(window.scrollY > window.innerHeight * 0.6);
  }

  scrollToTop(): void {
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    window.scrollTo({ top: 0, behavior: reduced ? 'auto' : 'smooth' });
  }
}
