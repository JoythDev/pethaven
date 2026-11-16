import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

// Relativa al base href, por si la app se publica en una subruta.
const SPRITE_PATH = 'images/stickers/stickers.svg';

// Inserta el sprite de stickers en el documento una sola vez, la primera vez que se pinta uno.
// Va en línea (no como <use href="archivo#id">) para que los estilos de styles.css alcancen los símbolos.
@Injectable({ providedIn: 'root' })
export class StickerSpriteService {
  private readonly document = inject(DOCUMENT);
  private loading: Promise<void> | null = null;

  ensureLoaded(): Promise<void> {
    this.loading ??= fetch(new URL(SPRITE_PATH, this.document.baseURI))
      .then((response) => (response.ok ? response.text() : Promise.reject(new Error(String(response.status)))))
      .then((markup) => {
        const host = this.document.createElement('div');
        host.setAttribute('aria-hidden', 'true');
        host.style.display = 'none';
        host.innerHTML = markup;
        this.document.body.prepend(host);
      })
      .catch((error) => {
        // Los stickers son decorativos: si el sprite no carga se omiten. Un solo intento por carga
        // de página, para no repetir la petición con cada sticker de una lista.
        console.warn('No se pudo cargar el sprite de stickers', error);
      });
    return this.loading;
  }
}
