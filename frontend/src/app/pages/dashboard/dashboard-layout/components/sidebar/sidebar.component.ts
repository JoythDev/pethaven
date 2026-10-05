import { Component, output } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

interface DashboardNavItem {
  label: string;
  icon: string;
  link?: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-dashboard-sidebar',
  imports: [NgClass, RouterLink, RouterLinkActive, TooltipModule],
  templateUrl: './sidebar.component.html',
})
export class DashboardSidebarComponent {
  /** Notifica al layout para cerrar el drawer en móvil. */
  readonly itemClicked = output<void>();

  readonly items: DashboardNavItem[] = [
    { label: 'Mascotas', icon: '/images/icons/paw-print-icon.svg', link: '/dashboard/pets' },
    { label: 'Dueños', icon: '/images/icons/user-icon.svg', link: '/dashboard/owners' },
    { label: 'Veterinarios', icon: '/images/icons/stethoscope-icon.svg', disabled: true },
    { label: 'Medicamentos', icon: '/images/icons/pulse-icon.svg', disabled: true },
    { label: 'Tratamientos', icon: '/images/icons/hospital-bed-icon.svg', disabled: true },
  ];
}
