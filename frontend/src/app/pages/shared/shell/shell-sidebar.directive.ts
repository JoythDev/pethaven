import { Directive, DestroyRef, OnInit, TemplateRef, inject } from '@angular/core';

import { ShellSidebarService } from './shell-sidebar.service';

/**
 * Una página aporta el contenido de la barra lateral contextual:
 * `<ng-template appShellSidebar>…módulos…</ng-template>` o `<div *appShellSidebar>`.
 * Se retira solo al destruirse la página; sin contenido el shell omite la columna.
 */
@Directive({ selector: '[appShellSidebar]' })
export class ShellSidebarDirective implements OnInit {
  private readonly template = inject(TemplateRef);
  private readonly sidebar = inject(ShellSidebarService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.sidebar.register(this.template);
    this.destroyRef.onDestroy(() => this.sidebar.unregister(this.template));
  }
}
