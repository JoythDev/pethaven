import { Component, computed, effect, input, signal } from '@angular/core';

import { identityHue, initials } from '../../../core/identity/identity';
import { coatKitFor, speciesSticker } from '../../../core/stickers/stickers';
import { Species } from '../../../models/pet.model';
import { StickerComponent } from '../sticker/sticker.component';

export type AvatarSize = 32 | 42 | 60;

const SIZE_CLASS: Record<AvatarSize, string> = {
  32: 'size-8 text-xs',
  42: 'size-[42px] text-sm',
  60: 'size-[60px] font-display text-xl font-semibold',
};

/**
 * Círculo con el color de identidad del recurso. Muestra la foto si la hay y carga; si no, las
 * iniciales (personas) o la cabeza del sticker de su especie (mascotas).
 */
@Component({
  selector: 'app-avatar',
  imports: [StickerComponent],
  templateUrl: './avatar.component.html',
  host: { '[class]': 'hostClass()' },
})
export class AvatarComponent {
  readonly id = input.required<number | string>();
  readonly name = input.required<string>();
  readonly kind = input<'pet' | 'person'>('person');
  readonly species = input<Species>(Species.DOG);
  readonly photoUrl = input<string | null>(null);
  readonly size = input<AvatarSize>(42);

  private readonly photoFailed = signal(false);

  protected readonly showPhoto = computed(() => !!this.photoUrl() && !this.photoFailed());
  protected readonly initials = computed(() => initials(this.name()));
  protected readonly sticker = computed(() => speciesSticker(this.species()));
  protected readonly kit = computed(() => coatKitFor(this.id()));

  protected readonly hostClass = computed(
    () =>
      `hue-${identityHue(this.id())} relative inline-grid shrink-0 place-items-center overflow-hidden rounded-full ` +
      `bg-(--hue) font-extrabold text-on-hue ${SIZE_CLASS[this.size()]}` +
      (this.showPhoto() ? ' ring-2 ring-card' : ''),
  );

  constructor() {
    // Una URL nueva merece otro intento de carga.
    effect(() => {
      this.photoUrl();
      this.photoFailed.set(false);
    });
  }

  protected onPhotoError(): void {
    this.photoFailed.set(true);
  }
}
