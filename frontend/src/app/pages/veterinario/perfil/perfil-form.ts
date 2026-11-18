import { Veterinarian, VeterinarianUpdateRequest } from '../../../models/veterinarian.model';

export type VetProfileField = 'name' | 'specialty';

type ErrorMap = Record<string, unknown> | null | undefined;

/** Mensaje de error de un campo del perfil: qué pasa y cómo corregirlo (11.4). Null si no hay error. */
export function vetProfileFieldError(field: VetProfileField, errors: ErrorMap): string | null {
  if (!errors || Object.keys(errors).length === 0) {
    return null;
  }
  if (typeof errors['server'] === 'string') {
    return errors['server'];
  }
  if (field === 'name') {
    return 'maxlength' in errors ? 'Usa 100 caracteres o menos en el nombre.' : 'Escribe tu nombre completo.';
  }
  return 'maxlength' in errors
    ? 'Usa 80 caracteres o menos en la especialidad.'
    : 'Escribe tu especialidad, por ejemplo Cirugía.';
}

/**
 * Cuerpo del PUT del veterinario: el API exige reenviar todo y rechaza cambiar cédula o correo,
 * así que esos dos y la foto se conservan; lo que no se cambia queda como está guardado y sin
 * contraseña nueva se envía `null` para conservar la actual.
 */
export function buildVetUpdate(
  vet: Veterinarian,
  changes: { name?: string; specialty?: string; password?: string },
): VeterinarianUpdateRequest {
  return {
    document: vet.document,
    name: changes.name?.trim() ?? vet.name,
    email: vet.email,
    specialty: changes.specialty?.trim() ?? vet.specialty,
    photoUrl: vet.photoUrl,
    password: changes.password ?? null,
  };
}
