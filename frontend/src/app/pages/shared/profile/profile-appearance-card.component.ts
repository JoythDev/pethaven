import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { ThemeService } from '../../../core/theme/theme.service';
import { CardComponent } from '../card/card.component';

/**
 * Tarjeta «Apariencia»: interruptor «Tema oscuro» ligado al `ThemeService` (aplica al instante y
 * se recuerda). Refleja el tema efectivo: sin elección previa sigue al sistema; al moverlo queda
 * una elección explícita, oscura o clara.
 */
@Component({
  selector: 'app-profile-appearance-card',
  imports: [CardComponent, FormsModule, ToggleSwitchModule],
  template: `
    <section app-card heading="Apariencia">
      <div class="relative flex items-start gap-3 py-1">
        <div class="min-w-0 flex-1">
          <!-- La etiqueta cubre toda la fila: el área táctil supera los 44 px sin agrandar el interruptor. -->
          <label
            for="profile-dark-theme"
            class="block cursor-pointer text-sm leading-5 font-extrabold text-ink after:absolute after:inset-0 after:content-['']"
            >Tema oscuro</label
          >
          <p class="text-[12.5px] leading-[18px] text-ink-muted">La primera vez seguimos la preferencia de tu sistema.</p>
        </div>
        <p-toggleswitch
          class="mt-0.5 shrink-0"
          inputId="profile-dark-theme"
          [ngModel]="theme.isDark()"
          (ngModelChange)="theme.set($event ? 'dark' : 'light')"
        />
      </div>
    </section>
  `,
  host: { class: 'block' },
})
export class ProfileAppearanceCardComponent {
  protected readonly theme = inject(ThemeService);
}
