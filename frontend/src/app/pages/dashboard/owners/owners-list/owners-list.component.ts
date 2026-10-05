import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { Table, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { Owner } from '../../../../models/owner.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';

@Component({
  selector: 'app-owners-list',
  imports: [TableModule, ButtonModule, TooltipModule, IconFieldModule, InputIconModule, InputTextModule],
  templateUrl: './owners-list.component.html',
})
export class OwnersListComponent {
  private readonly ownerService = inject(OwnerService);
  private readonly petService = inject(PetService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  readonly searchTerm = signal('');

  readonly filteredOwners = computed(() => {
    const term = this.normalize(this.searchTerm());
    return this.ownerService.ownersList().filter((owner) => {
      if (!term) {
        return true;
      }
      return (
        this.normalize(owner.name).includes(term) ||
        this.normalize(owner.document).includes(term)
      );
    });
  });

  onSearch(value: string, table: Table): void {
    this.searchTerm.set(value);
    table.first = 0;
  }

  nuevo(): void {
    this.router.navigate(['/dashboard/owners/add']);
  }

  ver(owner: Owner): void {
    this.router.navigate(['/dashboard/owners', owner.id]);
  }

  editar(owner: Owner): void {
    this.router.navigate(['/dashboard/owners/update', owner.id]);
  }

  eliminar(owner: Owner): void {
    const petsCount = this.petService.getByOwnerId(owner.id).length;
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
      },
    });
  }

  private normalize(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }
}
