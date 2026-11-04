import { ConfirmationService } from 'primeng/api';
import { Drug } from '../../../models/drug.model';

// Abre el diálogo de confirmación para eliminar un medicamento. Lo usan el listado y el detalle
// para que el texto y las opciones sean los mismos. onAccept solo se ejecuta si el usuario acepta.
export const confirmDrugDeletion = (
  confirmationService: ConfirmationService,
  drug: Drug,
  onAccept: () => void,
): void => {
  confirmationService.confirm({
    header: 'Eliminar medicamento',
    message:
      `¿Seguro que quieres eliminar ${drug.name}? ` +
      'Si está asociado a tratamientos registrados, no se podrá eliminar. ' +
      'Esta acción no se puede deshacer.',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Eliminar',
    rejectLabel: 'Cancelar',
    acceptButtonStyleClass: 'p-button-danger',
    rejectButtonStyleClass: 'p-button-text',
    accept: () => onAccept(),
  });
};
