import { DOCUMENT } from '@angular/common';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { DARK_CLASS, THEME_STORAGE_KEY, Theme, parseStoredTheme, resolveTheme } from './theme';

// Tema claro u oscuro: sigue al sistema hasta que el usuario elige, y entonces recuerda la elección.
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly systemDark = window.matchMedia('(prefers-color-scheme: dark)');

  private stored = this.readStored();
  private readonly current = signal<Theme>(resolveTheme(this.stored, this.systemDark.matches));

  readonly theme = this.current.asReadonly();
  readonly isDark = computed(() => this.current() === 'dark');

  constructor() {
    effect(() => {
      this.document.documentElement.classList.toggle(DARK_CLASS, this.isDark());
    });

    this.systemDark.addEventListener('change', (event) => {
      if (this.stored === null) {
        this.current.set(event.matches ? 'dark' : 'light');
      }
    });
  }

  toggle(): void {
    this.set(this.isDark() ? 'light' : 'dark');
  }

  set(theme: Theme): void {
    this.stored = theme;
    this.current.set(theme);
    try {
      localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // Sin almacenamiento disponible la elección dura solo esta visita.
    }
  }

  private readStored(): Theme | null {
    try {
      return parseStoredTheme(localStorage.getItem(THEME_STORAGE_KEY));
    } catch {
      return null;
    }
  }
}
