// Orden congelado: cambiarlo o quitar un tono cambia el color de todos los recursos existentes.
export const IDENTITY_HUES = ['honey', 'sage', 'sky', 'rose', 'lilac', 'peach'] as const;

export type IdentityHue = (typeof IDENTITY_HUES)[number];

// Color permanente de una mascota, medicamento, veterinario o dueño a partir de su id.
export function identityHue(id: number | string): IdentityHue {
  const n =
    typeof id === 'number'
      ? Math.abs(Math.trunc(id))
      : [...id].reduce((sum, ch) => sum + ch.codePointAt(0)!, 0);
  return IDENTITY_HUES[n % IDENTITY_HUES.length];
}

// Iniciales de una persona (máximo dos), sin el tratamiento «Dr.» o «Dra.».
export function initials(name: string): string {
  return name
    .replace(/^\s*Dra?\.\s*/i, '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0].toUpperCase())
    .join('');
}
