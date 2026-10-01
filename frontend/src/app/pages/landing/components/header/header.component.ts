import { Component, ElementRef, HostListener, ViewChild, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Header de la landing. Portado de `fragments.html` (`th:fragment="header"`).
 *
 * Cambios respecto al original:
 * - Se eliminó el botón "Registrarse" (desktop y menú móvil): el registro no
 *   forma parte del flujo de negocio.
 * - El menú móvil ya no lo controla `static/js/script.js` manipulando la clase
 *   `hidden`: ahora es estado del componente, con los mismos atributos ARIA.
 * - Los enlaces internos conservan `href="#seccion"` (la landing es
 *   same-document, así que el scroll nativo es idéntico al original).
 */
@Component({
  selector: 'app-header',
  imports: [RouterLink],
  templateUrl: './header.component.html'
})
export class HeaderComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  @ViewChild('menuBtn') private menuBtn?: ElementRef<HTMLButtonElement>;

  readonly menuOpen = signal(false);

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  /** Equivalente al listener de `document` del script original: cierra al hacer clic fuera del header. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.menuOpen()) {
      return;
    }

    const target = event.target as Node | null;
    if (!target || !this.host.nativeElement.contains(target)) {
      this.closeMenu();
    }
  }

  /** Equivalente al listener de la tecla Esc: cierra y devuelve el foco al botón. */
  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (!this.menuOpen()) {
      return;
    }

    this.closeMenu();
    this.menuBtn?.nativeElement.focus();
  }
}
