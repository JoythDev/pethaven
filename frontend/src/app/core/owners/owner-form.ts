import { parseRouteId } from '../http/route-id';

export type OwnerFormField = 'name' | 'document' | 'phone' | 'email' | 'password';

type ErrorMap = Record<string, unknown> | null | undefined;

/** Mensaje de error de un campo: qué pasa y cómo corregirlo (11.4). Null si no hay error. */
export function ownerFieldError(field: OwnerFormField, errors: ErrorMap, options: { editing: boolean }): string | null {
  if (!errors || Object.keys(errors).length === 0) {
    return null;
  }
  if (typeof errors['server'] === 'string') {
    return errors['server'];
  }
  const has = (key: string): boolean => key in errors;

  switch (field) {
    case 'name':
      return has('maxlength') ? 'Usa 100 caracteres o menos en el nombre.' : 'Escribe el nombre completo del dueño.';
    case 'document':
      return has('maxlength')
        ? 'Usa 20 caracteres o menos en la cédula.'
        : 'Escribe la cédula del dueño, por ejemplo 1012345678.';
    case 'phone':
      return has('maxlength')
        ? 'Usa 20 caracteres o menos en el teléfono.'
        : 'Escribe el teléfono del dueño, por ejemplo 3001234567.';
    case 'email':
      if (has('maxlength')) return 'Usa 120 caracteres o menos en el correo.';
      return has('email')
        ? 'Escribe un correo válido, por ejemplo ana@correo.com.'
        : 'Escribe el correo del dueño, por ejemplo ana@correo.com.';
    case 'password':
      if (has('maxlength')) return 'Usa 255 caracteres o menos en la contraseña.';
      if (has('minlength')) {
        return options.editing
          ? 'La contraseña debe tener al menos 8 caracteres. Escribe una más larga o déjala vacía.'
          : 'La contraseña debe tener al menos 8 caracteres. Escribe una más larga.';
      }
      return 'Escribe una contraseña de 8 caracteres o más.';
  }
}

/** Id del dueño que llega en `?dueno=`; un valor inválido se ignora sin avisar. */
export const parseOwnerParam = (raw: string | null): number | null => parseRouteId(raw);

/** Formulario de mascota; con `ownerPetQuery(id)` llega con ese dueño elegido. */
export const newPetUrl = (): string => '/veterinario/mascotas/add';

export const ownerPetQuery = (ownerId: number): { dueno: number } => ({ dueno: ownerId });
