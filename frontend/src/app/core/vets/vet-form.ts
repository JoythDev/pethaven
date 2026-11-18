export type VetFormField = 'name' | 'document' | 'specialty' | 'email' | 'password' | 'photoUrl';

type ErrorMap = Record<string, unknown> | null | undefined;

/** Mensaje de error de un campo: qué pasa y cómo corregirlo (11.4). Null si no hay error. */
export function vetFieldError(field: VetFormField, errors: ErrorMap, options: { editing: boolean }): string | null {
  if (!errors || Object.keys(errors).length === 0) {
    return null;
  }
  if (typeof errors['server'] === 'string') {
    return errors['server'];
  }
  const has = (key: string): boolean => key in errors;

  switch (field) {
    case 'name':
      return has('maxlength')
        ? 'Usa 100 caracteres o menos en el nombre.'
        : 'Escribe el nombre completo del veterinario.';
    case 'document':
      return has('maxlength')
        ? 'Usa 20 caracteres o menos en la cédula.'
        : 'Escribe la cédula del veterinario, por ejemplo 1012345678.';
    case 'specialty':
      return has('maxlength')
        ? 'Usa 80 caracteres o menos en la especialidad.'
        : 'Escribe la especialidad, por ejemplo Medicina interna.';
    case 'email':
      if (has('maxlength')) return 'Usa 120 caracteres o menos en el correo.';
      return has('email')
        ? 'Escribe un correo válido, por ejemplo ana@pethaven.com.'
        : 'Escribe el correo del veterinario, por ejemplo ana@pethaven.com.';
    case 'photoUrl':
      return 'Usa 500 caracteres o menos en la URL de la foto.';
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
