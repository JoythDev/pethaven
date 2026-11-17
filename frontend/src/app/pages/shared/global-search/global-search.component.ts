import { NgTemplateOutlet } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { LucideSearch } from '@lucide/angular';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { identityHue } from '../../../core/identity/identity';
import {
  SearchEntry,
  flattenSearchGroups,
  searchGroups,
  searchKindsFor,
} from '../../../core/search/global-search';
import { Species } from '../../../models/pet.model';
import { AvatarComponent } from '../avatar/avatar.component';
import { PopoverDismissDirective, PopoverDismissReason } from '../shell/popover-dismiss.directive';
import { ShellSidebarService } from '../shell/shell-sidebar.service';
import { StickerComponent } from '../sticker/sticker.component';
import { GlobalSearchService } from './global-search.service';

const DEBOUNCE_MS = 250;
const LISTBOX_ID = 'global-search-results';

/**
 * Búsqueda global de la barra superior (9.3, 11.16). En `expanded` es un campo de 220 px con un
 * popover de resultados; en `medium` un botón de icono que abre el mismo campo en un panel; en
 * `compact` el panel lo abre el elemento «Buscar» del menú de usuario (`GlobalSearchService.openOverlay`).
 * Los datos se piden al enfocar y se filtran con 250 ms de espera. Patrón combobox con listbox:
 * flechas mueven la opción activa, Intro la abre, Escape cierra.
 */
@Component({
  selector: 'app-global-search',
  imports: [
    NgTemplateOutlet,
    InputTextModule,
    TooltipModule,
    LucideSearch,
    AvatarComponent,
    StickerComponent,
    PopoverDismissDirective,
  ],
  templateUrl: './global-search.component.html',
  host: { '[class]': 'hostClass()' },
})
export class GlobalSearchComponent {
  protected readonly search = inject(GlobalSearchService);
  protected readonly layout = inject(ShellSidebarService).layout;
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly listboxId = LISTBOX_ID;
  protected readonly dog = Species.DOG;

  protected readonly query = signal('');
  private readonly debounced = signal('');
  protected readonly active = signal(0);
  private readonly inlineOpen = signal(false);
  private debounceTimer: ReturnType<typeof setTimeout> | undefined;

  private readonly input = viewChild<ElementRef<HTMLInputElement>>('input');
  private readonly trigger = viewChild<ElementRef<HTMLButtonElement>>('trigger');

  protected readonly inline = computed(() => this.layout() === 'expanded');
  // El campo cede ancho antes que las pestañas (9.4): se estrecha hasta 128 px y después las pestañas hacen scroll.
  protected readonly hostClass = computed(() => {
    switch (this.layout()) {
      case 'compact':
        return 'contents';
      case 'medium':
        return 'relative block';
      default:
        return 'relative block w-[220px] min-w-32 shrink-[100]';
    }
  });
  protected readonly overlayClass = computed(() =>
    this.layout() === 'compact'
      ? 'fixed inset-x-3 top-[68px] z-40 p-2'
      : 'absolute top-[calc(100%+8px)] right-0 z-40 w-[360px] max-w-[calc(100vw-24px)] p-2',
  );

  protected readonly open = computed(() =>
    this.inline() ? this.inlineOpen() && this.query().trim() !== '' : this.search.overlayOpen(),
  );

  private readonly kinds = computed(() => {
    const role = this.search.role();
    return role ? searchKindsFor(role) : [];
  });
  private readonly groups = computed(() => searchGroups(this.search.entries(), this.debounced(), this.kinds()));
  private readonly flat = computed(() => flattenSearchGroups(this.groups()));

  // Cada resultado con su posición global, para `aria-activedescendant` y el teclado.
  protected readonly rows = computed(() => {
    let index = 0;
    return this.groups().map((group) => ({
      kind: group.kind,
      label: group.label,
      items: group.entries.map((entry) => ({ entry, index: index++ })),
    }));
  });
  protected readonly hasResults = computed(() => this.flat().length > 0);
  protected readonly activeId = computed(() =>
    this.open() && this.hasResults() ? this.optionId(Math.min(this.active(), this.flat().length - 1)) : null,
  );
  protected readonly shownQuery = computed(() => this.debounced().trim());
  protected readonly liveText = computed(() => {
    if (!this.open() || this.search.status() === 'loading' || !this.shownQuery()) {
      return '';
    }
    const count = this.flat().length;
    return count === 0 ? 'Sin resultados' : count === 1 ? '1 resultado' : `${count} resultados`;
  });

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.debounceTimer));

    // Una búsqueda nueva empieza en el primer resultado.
    effect(() => {
      this.groups();
      untracked(() => this.active.set(0));
    });

    // El panel de pantallas estrechas se cierra si la ventana vuelve a `expanded`, donde el campo va en la barra.
    effect(() => {
      if (this.inline() && this.search.overlayOpen()) {
        untracked(() => this.search.closeOverlay());
      }
    });

    // Al abrir el panel el foco va al campo; el retraso deja que el menú de usuario termine de devolver el suyo.
    effect(() => {
      if (!this.inline() && this.search.overlayOpen()) {
        setTimeout(() => this.input()?.nativeElement.focus(), 80);
      }
    });

    // Mantiene a la vista la opción activa al moverse con el teclado.
    effect(() => {
      const id = this.activeId();
      if (id) {
        document.getElementById(id)?.scrollIntoView({ block: 'nearest' });
      }
    });
  }

  protected optionId(index: number): string {
    return `global-search-option-${index}`;
  }

  protected optionClass(index: number): string {
    return (
      'flex min-w-0 cursor-pointer items-center gap-3 rounded-row px-2.5 py-[7px] text-ink ' +
      (index === this.active() ? 'bg-sunken' : '')
    );
  }

  protected hue(entry: SearchEntry): string {
    return `hue-${identityHue(entry.id)}`;
  }

  protected onFocus(): void {
    this.search.ensureLoaded();
    if (this.inline() && this.query().trim()) {
      this.inlineOpen.set(true);
    }
  }

  protected onInput(value: string): void {
    this.query.set(value);
    if (this.inline()) {
      this.inlineOpen.set(true);
    }
    this.search.ensureLoaded();
    clearTimeout(this.debounceTimer);
    if (value.trim() === '') {
      this.debounced.set('');
      return;
    }
    this.debounceTimer = setTimeout(() => this.debounced.set(value), DEBOUNCE_MS);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const count = this.flat().length;
    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowUp':
        event.preventDefault();
        if (this.inline() && !this.inlineOpen() && this.query().trim()) {
          this.inlineOpen.set(true);
        } else if (count > 0) {
          const step = event.key === 'ArrowDown' ? 1 : -1;
          this.active.set((this.active() + step + count) % count);
        }
        break;
      case 'Enter': {
        // Intro justo después de teclear no espera a la pausa.
        this.flushDebounce();
        const entry = this.flat()[this.active()];
        if (entry) {
          event.preventDefault();
          this.select(entry);
        }
        break;
      }
      case 'Tab':
        this.close(false);
        break;
    }
  }

  protected toggleOverlay(): void {
    if (this.search.overlayOpen()) {
      this.search.closeOverlay(true);
    } else {
      this.search.openOverlay(this.trigger()?.nativeElement ?? null);
    }
  }

  protected dismiss(reason: PopoverDismissReason): void {
    this.close(reason === 'escape');
  }

  protected select(entry: SearchEntry): void {
    void this.router.navigateByUrl(entry.route);
    clearTimeout(this.debounceTimer);
    this.query.set('');
    this.debounced.set('');
    this.close(false);
    this.input()?.nativeElement.blur();
  }

  private flushDebounce(): void {
    clearTimeout(this.debounceTimer);
    this.debounced.set(this.query());
  }

  private close(restoreFocus: boolean): void {
    this.inlineOpen.set(false);
    if (this.search.overlayOpen()) {
      this.search.closeOverlay(restoreFocus);
    }
  }
}
