// El enum de especies, calcado del Species.java que ya tenía el backend
export enum Species {
  DOG = 'DOG',
  CAT = 'CAT'
}

// Mascota tal como la devuelve el API (PetResponse): trae el id y el
// nombre de su dueño para poder mostrarlo sin pedir al dueño por aparte.
export interface Pet {
  id: number;
  name: string;
  species: Species;
  breed: string;
  age: number;
  weight: number;
  disease: string | null; // diagnóstico del último tratamiento; null si está estable
  photoUrl: string | null;
  active: boolean;
  ownerId: number;
  ownerName: string;
}

// Cuerpo que recibe el API para crear o actualizar una mascota (PetRequest).
// El estado "active" no va aquí: el API lo cambia con un endpoint aparte. El diagnóstico
// tampoco: lo define cada tratamiento registrado.
export interface PetRequest {
  name: string;
  ownerId: number;
  species: Species;
  breed: string;
  age: number;
  weight: number;
  photoUrl: string | null;
}

// Límites de peso (kg) y edad (años) que valida el API.
export const PET_WEIGHT_MIN = 0.1;
export const PET_WEIGHT_MAX = 120;
export const PET_AGE_MAX = 30;
