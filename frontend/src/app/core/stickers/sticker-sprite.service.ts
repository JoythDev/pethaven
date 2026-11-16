import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

const SPRITE_URL = '/images/stickers/stickers.svg';

// Inserta el sprite de stickers en el documento una sola vez, la primera vez que se pinta uno.
// Va en línea (no como <use href="archivo#id">) para que los estilos de styles.css alcancen los símbolos.
@Injectable({ providedIn: 'root' })
export class StickerSpriteService {
  private readonly document = inject(DOCUMENT);
  private loading: Promise<void> | null = null;

  ensureLoaded(): Promise<void> {
    this.loading ??= fetch(SPRITE_URL)
      .then((response) => (response.ok ? response.text() : Promise.reject(new Error(String(response.status)))))
      .then((markup) => {
        const host = this.document.createElement('div');
        host.setAttribute('aria-hidden', 'true');
        host.style.display = 'none';
        host.innerHTML = markup;
        this.document.body.prepend(host);
      })
      .catch(() => {
        // Los stickers son decorativos: si el sprite no carga se omiten y se reintenta en el próximo.
        this.loading = null;
      });
    return this.loading;
  }
}
