import { ConfirmationService } from 'primeng/api';
import { Owner } from '../../../models/owner.model';

// Describe qué pasará con las mascotas del dueño al eliminarlo. Con null (no se pudo consultar
// el conteo) avisa que se eliminarán igual, sin dar una cifra.
const describePets = (petsCount: number | null): string => {
  if (petsCount === null) {
    return 'También se eliminarán sus mascotas asociadas.';
  }
  if (petsCount === 0) {
    return 'No tiene mascotas asociadas.';
  }
  return `También se eliminarán ${petsCount === 1 ? 'su mascota asociada' : `sus ${petsCount} mascotas asociadas`}.`;
};

// Abre el diálogo de confirmación para eliminar un dueño. Lo usan el listado y la ficha para
// que el texto y las opciones sean los mismos. onAccept solo se ejecuta si el usuario acepta.
export const confirmOwnerDeletion = (
  confirmationService: ConfirmationService,
  owner: Owner,
  petsCount: number | null,
  onAccept: () => void,
): void => {
  confirmationService.confirm({
    header: 'Eliminar dueño',
    message:
      `¿Seguro que quieres eliminar a ${owner.name}? ${describePets(petsCount)} ` +
      'Si alguna de sus mascotas tiene tratamientos registrados, no se podrá eliminar. ' +
      'Esta acción no se puede deshacer.',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Eliminar',
    rejectLabel: 'Cancelar',
    acceptButtonStyleClass: 'p-button-danger',
    rejectButtonStyleClass: 'p-button-text',
    accept: () => onAccept(),
  });
};
