import { Injectable, TemplateRef, computed, signal } from '@angular/core';

import { ShellLayout } from './shell-tabs';

const STORAGE_KEY = 'pethaven.sidebar';

/**
 * Estado de la barra lateral contextual. Cada página registra su contenido con `*appShellSidebar`
 * y el shell lo muestra: como columna a partir de 1080 px y como cajón por debajo.
 */
@Injectable({ providedIn: 'root' })
export class ShellSidebarService {
  private readonly templateState = signal<TemplateRef<unknown> | null>(null);

  readonly template = this.templateState.asReadonly();
  readonly hasContent = computed(() => this.templateState() !== null);

  /** Disposición actual; la mide el shell. */
  readonly layout = signal<ShellLayout>('expanded');
  /** Columna oculta por el usuario (se recuerda y vale para todas las secciones). */
  readonly collapsed = signal(this.readCollapsed());
  readonly drawerOpen = signal(false);

  /** Valor de `aria-expanded` del botón que muestra u oculta la barra. */
  readonly expanded = computed(() => (this.layout() === 'expanded' ? !this.collapsed() : this.drawerOpen()));

  register(template: TemplateRef<unknown>): void {
    this.templateState.set(template);
  }

  // Solo quita su propia plantilla: la página nueva puede registrarse antes de que la anterior se destruya.
  unregister(template: TemplateRef<unknown>): void {
    if (this.templateState() === template) {
      this.templateState.set(null);
    }
  }

  toggle(): void {
    if (this.layout() === 'expanded') {
      const collapsed = !this.collapsed();
      this.collapsed.set(collapsed);
      this.writeCollapsed(collapsed);
    } else {
      this.drawerOpen.update((open) => !open);
    }
  }

  private readCollapsed(): boolean {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'closed';
    } catch {
      return false;
    }
  }

  private writeCollapsed(collapsed: boolean): void {
    try {
      localStorage.setItem(STORAGE_KEY, collapsed ? 'closed' : 'open');
    } catch {
      // Sin almacenamiento la elección dura solo esta visita.
    }
  }
}
