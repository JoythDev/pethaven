import { Component, input, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DrawerModule } from 'primeng/drawer';
import { ToastModule } from 'primeng/toast';
import { AreaLayoutConfig, DEFAULT_AREA_LAYOUT_CONFIG } from './area-layout-config';
import { AreaSidebarComponent } from './area-sidebar.component';
import { AreaTopbarComponent } from './area-topbar.component';

/** Shell de un área autenticada: sidebar, topbar y feedback global. */
@Component({
  selector: 'app-area-layout',
  imports: [
    RouterOutlet,
    DrawerModule,
    ToastModule,
    ConfirmDialogModule,
    AreaSidebarComponent,
    AreaTopbarComponent,
  ],
  templateUrl: './area-layout.component.html',
})
export class AreaLayoutComponent {
  readonly mobileMenuVisible = signal(false);

  /** Configuración visible del área; por defecto, la del panel del veterinario. */
  readonly layoutConfig = input<AreaLayoutConfig>(DEFAULT_AREA_LAYOUT_CONFIG);
}
