import { ConfirmationService } from 'primeng/api';
import { Drug } from '../../../models/drug.model';

// Abre el diálogo de confirmación para eliminar un medicamento (10.20): el título dice la acción y el
// objeto, el cuerpo la consecuencia y el botón principal, en peligro, repite el verbo. Lo usan el listado
// y el detalle para que el texto y las opciones sean los mismos. onAccept solo se ejecuta si el usuario acepta.
export const confirmDrugDeletion = (
  confirmationService: ConfirmationService,
  drug: Drug,
  onAccept: () => void,
): void => {
  confirmationService.confirm({
    header: `¿Eliminar ${drug.name}?`,
    message:
      'Si está asociado a tratamientos registrados, no se podrá eliminar. Esta acción no se puede deshacer.',
    acceptLabel: 'Eliminar',
    rejectLabel: 'Cancelar',
    acceptButtonProps: { severity: 'danger' },
    rejectButtonProps: { outlined: true, severity: 'secondary' },
    accept: () => onAccept(),
  });
};
