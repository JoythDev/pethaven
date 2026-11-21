import { Pet } from '../../models/pet.model';

// Severidades de p-tag. 'secondary' es la variante neutra del sistema de diseño.
export type TagSeverity = 'success' | 'warn' | 'danger' | 'info' | 'secondary';

export interface StatusInfo {
  label: string;
  severity: TagSeverity;
}

// Solo los estados reales: inactiva si está desactivada; si no, según tenga diagnóstico.
export function petStatus(pet: Pick<Pet, 'active' | 'disease'>): StatusInfo {
  if (!pet.active) {
    return { label: 'Inactiva', severity: 'secondary' };
  }
  return pet.disease?.trim()
    ? { label: 'En tratamiento', severity: 'warn' }
    : { label: 'Estable', severity: 'success' };
}
