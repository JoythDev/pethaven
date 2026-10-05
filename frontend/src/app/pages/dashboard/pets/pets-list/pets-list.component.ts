import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { Table, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { Pet, Species } from '../../../../models/pet.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { PetStatusTagComponent } from '../components/pet-status-tag/pet-status-tag.component';

interface SpeciesOption {
  label: string;
  value: Species;
}

@Component({
  selector: 'app-pets-list',
  imports: [
    FormsModule,
    RouterLink,
    TableModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    SelectModule,
    PetStatusTagComponent,
  ],
  templateUrl: './pets-list.component.html',
})
export class PetsListComponent {
  private readonly petService = inject(PetService);
  private readonly ownerService = inject(OwnerService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  readonly searchTerm = signal('');
  readonly speciesFilter = signal<Species | null>(null);

  readonly speciesOptions: SpeciesOption[] = [
    { label: 'Perro', value: Species.DOG },
    { label: 'Gato', value: Species.CAT },
  ];

  readonly filteredPets = computed(() => {
    const term = this.normalize(this.searchTerm());
    const species = this.speciesFilter();
    return this.petService.petsList().filter((pet) => {
      if (species && pet.species !== species) {
        return false;
      }
      if (!term) {
        return true;
      }
      return this.normalize(pet.name).includes(term) || this.normalize(pet.breed).includes(term);
    });
  });

  private readonly ownersById = computed(
    () => new Map(this.ownerService.ownersList().map((owner) => [owner.id, owner]))
  );

  ownerName(ownerId: number): string {
    return this.ownersById().get(ownerId)?.name ?? 'Sin asignar';
  }

  onSearch(value: string, table: Table): void {
    this.searchTerm.set(value);
    table.first = 0;
  }

  onSpeciesChange(value: Species | null, table: Table): void {
    this.speciesFilter.set(value);
    table.first = 0;
  }

  nueva(): void {
    this.router.navigate(['/dashboard/pets/add']);
  }

  ver(pet: Pet): void {
    this.router.navigate(['/dashboard/pets', pet.id]);
  }

  editar(pet: Pet): void {
    this.router.navigate(['/dashboard/pets/update', pet.id]);
  }

  eliminar(pet: Pet): void {
    this.confirmationService.confirm({
      header: 'Eliminar mascota',
      message: `¿Seguro que quieres eliminar a ${pet.name}? Esta acción no se puede deshacer.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.petService.delete(pet.id);
        this.messageService.add({
          severity: 'success',
          summary: 'Mascota eliminada',
          detail: `${pet.name} se eliminó correctamente.`,
        });
      },
    });
  }

  onPhotoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (!img.src.endsWith('paw-print-icon.svg')) {
      img.src = '/images/icons/paw-print-icon.svg';
    }
  }

  private normalize(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }
}
