import { Directive, ElementRef, HostListener, inject } from '@angular/core';

/**
 * Desplazamiento suave para los enlaces `href="#seccion"` dentro del host.
 *
 * Intercepta el clic, busca el elemento con ese id y llama a `scrollIntoView`
 * con `behavior: 'smooth'`. Con `prefers-reduced-motion` el salto es inmediato.
 */
@Directive({
  selector: '[appSmoothAnchors]'
})
export class SmoothAnchorsDirective {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    const link = (event.target as HTMLElement | null)?.closest<HTMLAnchorElement>('a[href^="#"]');
    if (!link || !this.host.nativeElement.contains(link)) {
      return;
    }

    const id = link.getAttribute('href')!.slice(1);
    const target = id ? document.getElementById(id) : null;
    if (!target) {
      return;
    }

    event.preventDefault();
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    target.scrollIntoView({ behavior: reduced ? 'auto' : 'smooth', block: 'start' });
  }
}
