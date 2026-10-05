import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { PetStatusTagComponent } from '../components/pet-status-tag/pet-status-tag.component';

@Component({
  selector: 'app-pets-detail',
  imports: [RouterLink, ButtonModule, TagModule, PetStatusTagComponent],
  templateUrl: './pet-detail.component.html',
})
export class PetsDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly ownerService = inject(OwnerService);

  private readonly petId = Number(this.route.snapshot.paramMap.get('id'));

  readonly pet = computed(() => this.petService.getById(this.petId));

  readonly ownerName = computed(() => {
    const pet = this.pet();
    if (!pet) {
      return 'Sin asignar';
    }
    return this.ownerService.getById(pet.ownerId)?.name ?? 'Sin asignar';
  });

  editar(id: number): void {
    this.router.navigate(['/dashboard/pets/update', id]);
  }

  volver(): void {
    this.router.navigate(['/dashboard/pets']);
  }
}
