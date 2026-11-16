import { identityIndex } from '../identity/identity';

// Hash FNV-1a de 32 bits para mezclar la sal textual en la semilla.
function hashSalt(salt: string): number {
  let hash = 0x811c9dc5;
  for (let i = 0; i < salt.length; i++) {
    hash = Math.imul(hash ^ salt.charCodeAt(i), 0x01000193);
  }
  return hash >>> 0;
}

// Flujo pseudoaleatorio estable (mulberry32) en [0, 1) a partir de un id y una sal.
// Los datos demo salen de aquí: mismo id y misma sal, siempre los mismos valores.
export function seeded(id: number | string, salt = ''): () => number {
  let state = (Math.imul(identityIndex(id) + 0x9e3779b9, 0x85ebca6b) ^ hashSalt(salt)) >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// Entero estable en [0, max).
export function seededInt(id: number | string, salt: string, max: number): number {
  return Math.floor(seeded(id, salt)() * max);
}

export function seededPick<T>(id: number | string, salt: string, items: readonly T[]): T {
  return items[seededInt(id, salt, items.length)];
}
