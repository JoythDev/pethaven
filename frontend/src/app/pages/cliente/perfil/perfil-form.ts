import { Owner, OwnerUpdateRequest } from '../../../models/owner.model';

export type OwnerProfileField = 'name' | 'phone';

type ErrorMap = Record<string, unknown> | null | undefined;

/** Mensaje de error de un campo del perfil: qué pasa y cómo corregirlo (11.4). Null si no hay error. */
export function ownerProfileFieldError(field: OwnerProfileField, errors: ErrorMap): string | null {
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
    ? 'Usa 20 caracteres o menos en el teléfono.'
    : 'Escribe tu teléfono, por ejemplo 3001234567.';
}

/**
 * Cuerpo del PUT del dueño: el API exige reenviar todo y, cuando quien guarda es el propio dueño,
 * rechaza cambiar cédula o correo (`isOwnerKeepingIdentity`), así que esos dos se conservan. Lo que no
 * se cambia queda como está guardado y sin contraseña nueva se envía `null` para conservar la actual.
 */
export function buildOwnerUpdate(
  owner: Owner,
  changes: { name?: string; phone?: string; password?: string },
): OwnerUpdateRequest {
  return {
    document: owner.document,
    name: changes.name?.trim() ?? owner.name,
    email: owner.email,
    phone: changes.phone?.trim() ?? owner.phone,
    password: changes.password ?? null,
  };
}
