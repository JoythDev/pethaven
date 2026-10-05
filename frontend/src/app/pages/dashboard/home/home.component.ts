import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TagModule } from 'primeng/tag';

interface DashboardModule {
  label: string;
  description: string;
  icon: string;
  link?: string;
}

@Component({
  selector: 'app-dashboard-home',
  imports: [RouterLink, TagModule],
  templateUrl: './home.component.html',
})
export class DashboardHomeComponent {
  readonly modules: DashboardModule[] = [
    {
      label: 'Mascotas',
      description: 'Registra y gestiona los pacientes de la clínica.',
      icon: '/images/icons/paw-print-icon.svg',
      link: '/dashboard/pets',
    },
    {
      label: 'Dueños',
      description: 'Administra los datos de contacto de las familias.',
      icon: '/images/icons/user-icon.svg',
      link: '/dashboard/owners',
    },
    {
      label: 'Veterinarios',
      description: 'Gestiona el equipo médico y sus especialidades.',
      icon: '/images/icons/stethoscope-icon.svg',
    },
    {
      label: 'Medicamentos',
      description: 'Controla el inventario farmacológico de la clínica.',
      icon: '/images/icons/pulse-icon.svg',
    },
    {
      label: 'Tratamientos',
      description: 'Da seguimiento a los planes de tratamiento activos.',
      icon: '/images/icons/hospital-bed-icon.svg',
    },
  ];
}
