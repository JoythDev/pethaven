import { Directive, ElementRef, inject, input, output } from '@angular/core';

export type PopoverDismissReason = 'outside' | 'escape';

/**
 * Cierre de un popover (10.20): un clic fuera del elemento o Escape. Solo escucha mientras está
 * abierto. Quien lo usa decide qué hacer con el motivo; con `escape` devuelve el foco al disparador.
 * El elemento anfitrión debe contener al disparador y al panel, para que pulsar el botón no cuente como «fuera».
 */
@Directive({
  selector: '[appPopoverDismiss]',
  host: {
    '(document:pointerdown)': 'onPointerDown($event)',
    '(document:keydown.escape)': 'onEscape($event)',
  },
})
export class PopoverDismissDirective {
  readonly open = input(false, { alias: 'appPopoverDismiss' });
  readonly dismissed = output<PopoverDismissReason>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected onPointerDown(event: PointerEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node | null)) {
      this.dismissed.emit('outside');
    }
  }

  protected onEscape(event: Event): void {
    if (this.open()) {
      event.preventDefault();
      this.dismissed.emit('escape');
    }
  }
}
