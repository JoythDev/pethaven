import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { ConfirmationService } from 'primeng/api';

/** Formulario que sabe si tiene cambios sin guardar (11.4). */
export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

/** Al salir de un formulario con cambios sin guardar pide confirmación con el diálogo del shell. */
export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  if (!component.hasUnsavedChanges()) {
    return true;
  }
  const confirmation = inject(ConfirmationService);
  return new Promise<boolean>((resolve) => {
    confirmation.confirm({
      header: 'Salir sin guardar',
      message: 'Tienes cambios sin guardar. Si sales ahora, se perderán.',
      acceptLabel: 'Salir sin guardar',
      rejectLabel: 'Seguir editando',
      rejectButtonProps: { outlined: true, severity: 'secondary' },
      acceptButtonProps: { severity: 'danger' },
      accept: () => resolve(true),
      reject: () => resolve(false),
    });
  });
};
