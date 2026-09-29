// El enum de especies, calcado del Species.java que ya tenía el backend
export enum Species {
  DOG = 'DOG',
  CAT = 'CAT'
}

// Este es el modelo de una mascota, con los mismos campos que tenía
// la entidad Pet.java en Spring Boot. Lo único que cambia es que en
// vez de guardar el Owner completo, solo guardamos su id (ownerId) —
// por ahora no nos toca manejar la asignación de dueño desde aquí.
export interface Pet {
  id: number;
  name: string;
  species: Species;
  breed: string;
  age: number;
  weight: number;
  disease: string | null; // null si la mascota está sana
  photoUrl: string;
  ownerId: number;
  active: boolean;
}
