import { Component, computed, inject, input } from '@angular/core';

import { StickerSpriteService } from '../../../core/stickers/sticker-sprite.service';
import { CoatKit, StickerName, stickerCanvas, stickerFrame } from '../../../core/stickers/stickers';

// Ilustración decorativa con silueta crema. `width` es el ancho en píxeles; `head` recorta la cabeza
// de los personajes para avatares. El significado siempre va en el texto que la acompaña.
@Component({
  selector: 'app-sticker',
  template: `<svg
    [attr.class]="'sk sk-k' + kit()"
    [attr.width]="width()"
    [attr.height]="frame().height"
    [attr.viewBox]="frame().viewBox"
    aria-hidden="true"
    focusable="false"
  >
    <use class="sk-halo" [attr.href]="href()" [attr.width]="canvas()[0]" [attr.height]="canvas()[1]" />
    <use [attr.href]="href()" [attr.width]="canvas()[0]" [attr.height]="canvas()[1]" />
  </svg>`,
  host: { class: 'inline-flex shrink-0', 'aria-hidden': 'true' },
})
export class StickerComponent {
  readonly name = input.required<StickerName>();
  readonly width = input(64);
  readonly kit = input<CoatKit>(1);
  readonly head = input(false);

  protected readonly canvas = computed(() => stickerCanvas(this.name()));
  protected readonly frame = computed(() => stickerFrame(this.name(), this.width(), this.head()));
  protected readonly href = computed(() => `#sk-${this.name()}`);

  constructor() {
    inject(StickerSpriteService).ensureLoaded();
  }
}
