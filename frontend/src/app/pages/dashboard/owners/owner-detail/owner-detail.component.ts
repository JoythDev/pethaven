import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';

@Component({
  selector: 'app-owners-detail',
  imports: [RouterLink, ButtonModule, TagModule],
  templateUrl: './owner-detail.component.html',
})
export class OwnersDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ownerService = inject(OwnerService);
  private readonly petService = inject(PetService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  private readonly ownerId = Number(this.route.snapshot.paramMap.get('id'));

  readonly owner = computed(() => this.ownerService.getById(this.ownerId));
  readonly pets = computed(() => this.petService.getByOwnerId(this.ownerId));

  editar(): void {
    this.router.navigate(['/dashboard/owners/update', this.ownerId]);
  }

  volver(): void {
    this.router.navigate(['/dashboard/owners']);
  }

  verPet(id: number): void {
    this.router.navigate(['/dashboard/pets', id]);
  }

  eliminar(): void {
    const owner = this.owner();
    if (!owner) {
      return;
    }

    const petsCount = this.pets().length;
    const petsDetail =
      petsCount > 0
        ? `También se eliminarán ${petsCount === 1 ? 'su mascota asociada' : `sus ${petsCount} mascotas asociadas`}.`
        : 'No tiene mascotas asociadas.';

    this.confirmationService.confirm({
      header: 'Eliminar dueño',
      message: `¿Seguro que quieres eliminar a ${owner.name}? ${petsDetail} Esta acción no se puede deshacer.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.ownerService.delete(owner.id);
        this.messageService.add({
          severity: 'success',
          summary: 'Dueño eliminado',
          detail: `${owner.name} se eliminó correctamente.`,
        });
        this.router.navigate(['/dashboard/owners']);
      },
    });
  }

  onPhotoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (!img.src.endsWith('paw-print-icon.svg')) {
      img.src = '/images/icons/paw-print-icon.svg';
    }
  }
}
