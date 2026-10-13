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
  disease: string | null; // null si la mascota está sana
  photoUrl: string | null;
  active: boolean;
  ownerId: number;
  ownerName: string;
}

// Cuerpo que recibe el API para crear o actualizar una mascota (PetRequest).
// El estado "active" no va aquí: el API lo cambia con un endpoint aparte.
export interface PetRequest {
  name: string;
  ownerId: number;
  species: Species;
  breed: string;
  age: number;
  weight: number;
  disease: string | null;
  photoUrl: string | null;
}
