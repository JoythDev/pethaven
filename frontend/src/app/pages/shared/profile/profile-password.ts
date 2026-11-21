import { AbstractControl, ValidationErrors, Validators } from '@angular/forms';

export type PasswordField = 'password' | 'confirm';

// Longitud permitida de la contraseña (la misma que valida el API).
export const PASSWORD_VALIDATORS = [Validators.required, Validators.minLength(8), Validators.maxLength(255)];

type ErrorMap = Record<string, unknown> | null | undefined;

/** Marca `mismatch` cuando la confirmación difiere; con la confirmación vacía lo cubre `required`. */
export function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value as string | null;
  const confirm = group.get('confirm')?.value as string | null;
  return confirm && password !== confirm ? { mismatch: true } : null;
}

/** Mensaje de error de un campo del diálogo de contraseña: qué pasa y cómo corregirlo (11.4). */
export function passwordFieldError(field: PasswordField, errors: ErrorMap): string | null {
  if (!errors || Object.keys(errors).length === 0) {
    return null;
  }
  if (typeof errors['server'] === 'string') {
    return errors['server'];
  }
  if (field === 'confirm') {
    return 'mismatch' in errors ? 'Las contraseñas no coinciden.' : 'Repite la contraseña para confirmarla.';
  }
  if ('maxlength' in errors) return 'Usa 255 caracteres o menos en la contraseña.';
  if ('minlength' in errors) return 'La contraseña debe tener al menos 8 caracteres. Escribe una más larga.';
  return 'Escribe una contraseña de 8 caracteres o más.';
}
