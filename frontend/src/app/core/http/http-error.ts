import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl } from '@angular/forms';
import { ApiError } from '../../models/api-error.model';

const CONNECTION_ERROR = 'No se pudo conectar con el servidor. Verifica que el API esté en ejecución.';

// Recibe el error de una petición HTTP y devuelve un mensaje legible para el usuario.
// Prioriza los errores por campo del API, luego su mensaje general y, si no hay
// ninguno (o el servidor no respondió), usa un mensaje de respaldo.
export const getErrorMessage = (error: unknown, fallback = 'Ocurrió un error inesperado.'): string => {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return CONNECTION_ERROR;
  }

  const body = error.error as Partial<ApiError> | null;
  const fieldErrors = body?.errors ? Object.values(body.errors) : [];

  if (fieldErrors.length > 0) {
    return fieldErrors.join(' ');
  }

  return body?.message ?? fallback;
};

// Lleva los errores de validación del API (400, por campo) a los controles del formulario
// para que cada campo muestre su mensaje. Las claves sin control (p. ej. "_global") se ignoran.
// Devuelve false si no se aplicó ningún error, para que el llamador muestre el mensaje general.
export const applyServerErrors = (form: { get(path: string): AbstractControl | null }, error: unknown): boolean => {
  if (!(error instanceof HttpErrorResponse) || error.status !== 400) {
    return false;
  }

  const body = error.error as Partial<ApiError> | null;
  const errors = body?.errors;
  if (!errors || typeof errors !== 'object') {
    return false;
  }

  let applied = false;
  for (const [field, message] of Object.entries(errors)) {
    const control = form.get(field);
    if (control) {
      control.setErrors({ server: message });
      applied = true;
    }
  }

  return applied;
};
