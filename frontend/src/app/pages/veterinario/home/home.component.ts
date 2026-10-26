import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin, map, throwError } from 'rxjs';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { Pet } from '../../../models/pet.model';
import { Treatment } from '../../../models/treatment.model';
import { AuthService } from '../../../services/auth.service';
import { PetService } from '../../../services/pet.service';
import { VeterinarianService } from '../../../services/veterinarian.service';

interface DashboardModule {
  label: string;
  description: string;
  icon: string;
  link?: string;
}

interface DashboardStats {
  patientCount: number;
  treatmentCount: number;
  activePatientCount: number;
  latestAttention: string | null;
  recentTreatments: RecentTreatment[];
}

interface RecentTreatment {
  id: number;
  petId: number;
  petName: string;
  date: string;
  drugs: string;
}

@Component({
  selector: 'app-dashboard-home',
  imports: [RouterLink],
  templateUrl: './home.component.html',
})
export class DashboardHomeComponent {
  private readonly authService = inject(AuthService);
  private readonly petService = inject(PetService);
  private readonly veterinarianService = inject(VeterinarianService);

  // Cargar los indicadores del veterinario autenticado.
  readonly dashboard = toSignal(
    this.loadDashboard().pipe(toViewState<DashboardStats>()),
    { initialValue: { status: 'loading' } as ViewState<DashboardStats> },
  );

// Módulos disponibles en el portal veterinario.
  readonly modules: DashboardModule[] = [
    {
      label: 'Mascotas',
      description: 'Registra y gestiona los pacientes de la clínica.',
      icon: '/images/icons/paw-print-icon.svg',
      link: '/veterinario/pets',
    },
    {
      label: 'Dueños',
      description: 'Administra los datos de contacto de las familias.',
      icon: '/images/icons/user-icon.svg',
      link: '/veterinario/owners',
    },
  ];

// Obtener los datos del dashboard del veterinario autenticado.
  private loadDashboard(): Observable<DashboardStats> {
    const user = this.authService.user();
    if (!user || user.role !== 'VETERINARIAN') {
      return throwError(() => new Error('No hay un veterinario autenticado.'));
    }

    return forkJoin({
      treatments: this.veterinarianService.getTreatmentsByVeterinarianId(user.id),
      pets: this.petService.getAll(),
    }).pipe(map(({ treatments, pets }) => this.calculateStats(treatments, pets)));
  }

  // Calcular pacientes, tratamientos, pacientes activos y última atención.
  private calculateStats(treatments: Treatment[], pets: Pet[]): DashboardStats {
    const patientIds = new Set(treatments.map((treatment) => treatment.petId));
    const orderedTreatments = [...treatments].sort(
      (first, second) => new Date(second.date).getTime() - new Date(first.date).getTime(),
    );
    const latestTreatment = orderedTreatments[0];

    return {
      patientCount: patientIds.size,
      treatmentCount: treatments.length,
      activePatientCount: pets.filter((pet) => patientIds.has(pet.id) && pet.active).length,
      latestAttention: latestTreatment ? this.formatDate(latestTreatment.date) : null,
      recentTreatments: orderedTreatments.slice(0, 3).map((treatment) => ({
        id: treatment.id,
        petId: treatment.petId,
        petName: treatment.petName,
        date: this.formatDate(treatment.date),
        drugs: this.formatDrugs(treatment),
      })),
    };
  }

  private formatDate(date: string): string {
    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(date));
  }

  private formatDrugs(treatment: Treatment): string {
    if (treatment.drugs.length === 0) {
      return 'Sin medicamentos registrados';
    }

    return treatment.drugs
      .map((drug) => `${drug.drugName} (${drug.units} ${drug.units === 1 ? 'unidad' : 'unidades'})`)
      .join(', ');
  }
}
