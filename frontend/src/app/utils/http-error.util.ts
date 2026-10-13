import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api-error.model';

const CONNECTION_ERROR = 'No se pudo conectar con el servidor. Verifica que el API esté en ejecución.';

// Recibe el error de una petición HTTP y devuelve un mensaje legible para el usuario.
// Prioriza los errores por campo del API, luego su mensaje general y, si no hay
// ninguno (o el servidor no respondió), usa un mensaje de respaldo.
export function getErrorMessage(error: unknown, fallback = 'Ocurrió un error inesperado.'): string {
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
}
