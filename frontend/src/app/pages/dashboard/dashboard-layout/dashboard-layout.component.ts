import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DrawerModule } from 'primeng/drawer';
import { ToastModule } from 'primeng/toast';
import { DashboardSidebarComponent } from './components/sidebar/sidebar.component';
import { DashboardTopbarComponent } from './components/topbar/topbar.component';

@Component({
  selector: 'app-dashboard-layout',
  imports: [
    RouterOutlet,
    DrawerModule,
    ToastModule,
    ConfirmDialogModule,
    DashboardSidebarComponent,
    DashboardTopbarComponent,
  ],
  templateUrl: './dashboard-layout.component.html',
})
export class DashboardLayoutComponent {
  readonly mobileMenuVisible = signal(false);
}
